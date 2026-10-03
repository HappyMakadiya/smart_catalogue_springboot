package com.example.smartcatalog.runner;

import com.example.smartcatalog.service.ProductEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Startup hook that triggers a one-time backfill of product embeddings.
 *
 * <h3>Why {@link ApplicationRunner} instead of {@code @EventListener(ApplicationReadyEvent)}</h3>
 * <p>{@code ApplicationRunner} runs after the full Spring context — including all
 * {@code SmartLifecycle} beans and the web server — is ready, so the application
 * is already serving traffic when the backfill begins. It also receives parsed
 * {@link ApplicationArguments}, making it easy to add CLI flags later (e.g.
 * {@code --skip-embedding-backfill}) if needed.</p>
 *
 * <h3>Fire-and-forget</h3>
 * <p>{@link ProductEmbeddingService#backfillMissingEmbeddings()} is annotated
 * {@code @Async}, so this runner returns immediately and the backfill runs on
 * a background thread. The HTTP layer is never blocked.</p>
 *
 * <h3>Idempotency</h3>
 * <p>The backfill query only selects products where {@code embedding IS NULL}.
 * Re-starting the application after a partial backfill (e.g. after a crash) is
 * therefore safe — already-embedded products are silently skipped.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingBackfillRunner implements ApplicationRunner {

    private final ProductEmbeddingService productEmbeddingService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[EmbeddingBackfillRunner] Application ready — scheduling embedding backfill on background thread.");
        productEmbeddingService.backfillMissingEmbeddings();
        // Returns immediately; backfill continues asynchronously.
    }
}
