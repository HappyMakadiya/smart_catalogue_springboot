package com.example.smartcatalog.service;

import com.example.smartcatalog.config.VectorFloatConverter;
import com.example.smartcatalog.model.Product;
import com.example.smartcatalog.repository.ProductRepository;
import com.google.genai.errors.ClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates and persists semantic embedding vectors for {@link Product} entities.
 *
 * <h3>Design decisions</h3>
 * <ul>
 *   <li><b>Async, not a JPA lifecycle callback</b> – {@code @PostPersist} /
 *       {@code @PostUpdate} fire inside the owning transaction and cannot safely
 *       inject Spring-managed beans (especially one that makes a blocking
 *       HTTP call to the OpenAI API). Using {@code @Async} runs the embedding
 *       work on a separate thread <em>after</em> the caller's transaction has
 *       committed, so the HTTP response is never held open waiting for OpenAI.</li>
 *   <li><b>Own transaction</b> – {@code @Transactional} here is a fresh
 *       transaction (propagation = REQUIRED, but the caller's tx is already
 *       committed by the time the async thread runs), so the repository save is
 *       safe and isolated.</li>
 *   <li><b>float[] storage</b> – Spring AI's {@code EmbeddingModel.embed()} returns
 *       {@code float[]} directly; this is stored via
 *       {@link com.example.smartcatalog.config.VectorFloatConverter} into
 *       pgvector's {@code vector(1536)} column.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductEmbeddingService {

    /** Gemini's batchEmbedContents accepts at most 100 texts per request. */
    private static final int BACKFILL_BATCH_SIZE = 100;

    /** Longest 429 retry hint worth sleeping through; longer means the daily quota is used up. */
    private static final Duration MAX_RATE_LIMIT_WAIT = Duration.ofMinutes(2);

    private static final int MAX_RATE_LIMIT_ATTEMPTS = 5;

    /** Gemini's 429 body carries a RetryInfo hint, e.g. {@code "retryDelay":"61517s"}. */
    private static final Pattern RETRY_DELAY = Pattern.compile("\"retryDelay\":\"(\\d+)(?:\\.\\d+)?s\"");

    private static final VectorFloatConverter VECTOR_CONVERTER = new VectorFloatConverter();

    private final EmbeddingModel embeddingModel;
    private final ProductRepository productRepository;
    private final TransactionTemplate transactionTemplate;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Asynchronously generates an embedding for the given product and persists it.
     *
     * <p>Called by {@link ProductService} after every successful create or update.
     * The method returns immediately to the caller; embedding generation happens
     * on Spring's default async executor.</p>
     *
     * @param productId the primary key of the product to embed
     */
    @Async
    @Transactional
    public void embedAndSave(Long productId) {
        productRepository.findById(productId).ifPresentOrElse(
                this::doEmbed,
                () -> log.warn("[ProductEmbeddingService] Product {} not found; skipping embedding.", productId)
        );
    }

    /**
     * Backfills embeddings for every product whose {@code embedding} column is
     * {@code NULL} — i.e. all products that existed before this feature was added,
     * or any that failed during a previous run.
     *
     * <h3>Why @Async, batched, and sequential?</h3>
     * <ul>
     *   <li>{@code @Async} — the whole backfill runs on a background thread so
     *       the application finishes starting up immediately and begins serving
     *       traffic without waiting for Gemini.</li>
     *   <li>Batched — {@link #BACKFILL_BATCH_SIZE} texts go to Gemini in a single
     *       request, and each batch is written with targeted UPDATEs in one
     *       transaction. 5,500 products become 55 API calls instead of 5,500.</li>
     *   <li>Sequential batches — running batches concurrently would quickly hit
     *       Gemini's per-minute rate limits.</li>
     *   <li>Rate limits — a 429 whose retry hint is short (per-minute limit) is
     *       waited out and the batch retried; a long hint (daily quota) stops the
     *       backfill instead of sending every remaining batch into the same 429.</li>
     * </ul>
     *
     * <p>Products left {@code NULL} by a failed batch or an early stop are picked
     * up on the next startup.</p>
     */
    @Async
    public void backfillMissingEmbeddings() {
        List<Product> products = productRepository.findAllWithNullEmbedding();
        if (products.isEmpty()) {
            log.info("[ProductEmbeddingService] Backfill: no products with missing embeddings found.");
            return;
        }

        log.info("[ProductEmbeddingService] Backfill: starting — {} product(s) need embeddings, batches of {}.",
                products.size(), BACKFILL_BATCH_SIZE);
        long startedAt = System.currentTimeMillis();
        int success = 0;
        int failure = 0;
        int deferred = 0;

        for (int from = 0; from < products.size(); from += BACKFILL_BATCH_SIZE) {
            List<Product> batch = products.subList(from, Math.min(from + BACKFILL_BATCH_SIZE, products.size()));
            try {
                embedBatchWithRateLimitRetry(batch);
                success += batch.size();
                log.info("[ProductEmbeddingService] Backfill: {}/{} done.", from + batch.size(), products.size());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                deferred = products.size() - from;
                log.warn("[ProductEmbeddingService] Backfill: interrupted while waiting out a rate limit.");
                break;
            } catch (ClientException ex) {
                if (ex.code() != 429) {
                    failure += batch.size();
                    logBatchFailure(batch, ex);
                    continue;
                }
                deferred = products.size() - from;
                Duration retryIn = retryDelay(ex);
                log.warn("[ProductEmbeddingService] Backfill: still rate limited by Gemini (retry hint ~{}h {}m) — "
                                + "stopping; {} product(s) left for a restart after that.",
                        retryIn.toHours(), retryIn.toMinutesPart(), deferred);
                break;
            } catch (Exception ex) {
                failure += batch.size();
                logBatchFailure(batch, ex);
            }
        }

        log.info("[ProductEmbeddingService] Backfill finished — {} succeeded, {} failed, {} deferred in {} s.",
                success, failure, deferred, (System.currentTimeMillis() - startedAt) / 1000);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Builds the embedding input text, calls the OpenAI Embeddings API, and
     * saves the resulting vector back to the product row.
     *
     * <p>Input format (keeps the model grounded with explicit labels):</p>
     * <pre>
     *   title: &lt;name&gt;
     *   category: &lt;category name or "Uncategorized"&gt;
     *   description: &lt;description or ""&gt;
     * </pre>
     *
     * @param product the managed entity to embed
     */
    private void doEmbed(Product product) {
        String inputText = buildInputText(product);
        log.debug("[ProductEmbeddingService] Embedding product {} – input: {}", product.getId(), inputText);

        try {
            float[] vector = embeddingModel.embed(inputText);
            product.setEmbedding(vector);
            productRepository.save(product);
            log.info("[ProductEmbeddingService] Saved embedding ({} dims) for product id={}.",
                    vector.length, product.getId());
        } catch (Exception ex) {
            log.error("[ProductEmbeddingService] Failed to embed product id={}: {}",
                    product.getId(), ex.getMessage(), ex);
            // Intentionally swallowed: embedding failure must not roll back the
            // primary create/update that already committed.
        }
    }

    /**
     * Runs {@link #embedBatch} and, when Gemini answers 429 with a short retry hint
     * (per-minute limit), sleeps for that long and tries the same batch again.
     *
     * @throws ClientException 429 when the hint exceeds {@link #MAX_RATE_LIMIT_WAIT}
     *         (daily quota) or the retries run out; any other API error as-is
     */
    private void embedBatchWithRateLimitRetry(List<Product> batch) throws InterruptedException {
        for (int attempt = 1; ; attempt++) {
            try {
                embedBatch(batch);
                return;
            } catch (ClientException ex) {
                if (ex.code() != 429 || attempt == MAX_RATE_LIMIT_ATTEMPTS) throw ex;
                Duration wait = retryDelay(ex);
                if (wait.compareTo(MAX_RATE_LIMIT_WAIT) > 0) throw ex;

                log.warn("[ProductEmbeddingService] Backfill: rate limited; retrying batch in {} s (attempt {}/{}).",
                        wait.toSeconds(), attempt + 1, MAX_RATE_LIMIT_ATTEMPTS);
                Thread.sleep(wait.plusSeconds(1));
            }
        }
    }

    /**
     * Reads Gemini's {@code RetryInfo} hint (e.g. {@code "retryDelay":"42s"}) from a
     * 429 response, defaulting to one minute when absent.
     */
    private static Duration retryDelay(ClientException ex) {
        Matcher matcher = RETRY_DELAY.matcher(String.valueOf(ex.getMessage()));
        return matcher.find() ? Duration.ofSeconds(Long.parseLong(matcher.group(1))) : Duration.ofMinutes(1);
    }

    private static void logBatchFailure(List<Product> batch, Exception ex) {
        log.error("[ProductEmbeddingService] Backfill: batch of {} starting at product id={} failed: {}",
                batch.size(), batch.get(0).getId(), ex.getMessage(), ex);
    }

    /**
     * Embeds a batch of products with one Gemini request and writes the vectors in
     * a single transaction.
     *
     * @param batch detached products (category already fetched)
     */
    private void embedBatch(List<Product> batch) {
        List<String> texts = batch.stream().map(this::buildInputText).toList();
        List<float[]> vectors = embeddingModel.embed(texts);
        if (vectors.size() != batch.size()) {
            throw new IllegalStateException(
                    "Expected " + batch.size() + " embeddings but received " + vectors.size());
        }

        transactionTemplate.executeWithoutResult(status -> {
            for (int i = 0; i < batch.size(); i++) {
                productRepository.updateEmbeddingIfMissing(
                        batch.get(i).getId(), VECTOR_CONVERTER.convertToDatabaseColumn(vectors.get(i)));
            }
        });
    }

    /**
     * Combines the product's name, category, and description into a single string
     * suitable for embedding. Labels make the context explicit for the model.
     *
     * @param product the source entity
     * @return a newline-delimited, label-prefixed string
     */
    private String buildInputText(Product product) {
        String categoryName = (product.getCategory() != null)
                ? product.getCategory().getName()
                : "Uncategorized";
        String description  = (product.getDescription() != null && !product.getDescription().isBlank())
                ? product.getDescription()
                : "";

        return "title: "       + product.getName()  + "\n"
             + "category: "    + categoryName        + "\n"
             + "description: " + description;
    }
}
