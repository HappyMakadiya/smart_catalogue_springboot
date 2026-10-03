package com.example.smartcatalog.repository;

import com.example.smartcatalog.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence layer for {@link Product}.
 *
 * <h3>N+1 prevention with {@code @EntityGraph}</h3>
 * <p>{@code Product} has a {@code @ManyToOne(fetch = FetchType.LAZY)} to
 * {@code Category}. Without an entity-graph, every access to
 * {@code product.getCategory()} would fire an additional SQL query — the classic
 * N+1 problem.</p>
 *
 * <p>The fix is to override the two standard {@link JpaRepository} methods
 * ({@code findAll(Pageable)} and {@code findById(Long)}) and annotate them with
 * {@code @EntityGraph}. Spring Data recognises these as overrides of inherited
 * signatures and therefore does <em>not</em> attempt derived-query parsing on
 * them — it simply adds a JOIN FETCH for the {@code category} attribute.</p>
 *
 * <h3>Why NOT custom method names?</h3>
 * <p>Names like {@code findAllWithCategory(Pageable)} look reasonable but cause
 * a startup failure: Spring Data interprets {@code WithCategory} as a property
 * predicate and throws {@code PropertyReferenceException} because no such
 * field exists on {@link Product}.</p>
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Returns a page of products with their {@code category} association eagerly
     * loaded in a single JOIN FETCH query.
     *
     * @param pageable page number, size, and sort specification
     * @return a {@link Page} of {@link Product} entities (never {@code null})
     */
    @Override
    @EntityGraph(attributePaths = "category")
    Page<Product> findAll(Pageable pageable);

    /**
     * Fetches a single product by its primary key, eagerly loading its
     * {@code category} so the caller never triggers a lazy-load proxy.
     *
     * @param id the product primary key
     * @return an {@link Optional} containing the product, or empty if not found
     */
    @Override
    @EntityGraph(attributePaths = "category")
    Optional<Product> findById(Long id);

    /**
     * Returns the IDs of every product whose {@code embedding} column is {@code NULL}.
     *
     * <p>Used by the startup backfill runner to cheaply determine which products
     * still need an embedding without loading full entity state.</p>
     *
     * @return list of product primary keys with no embedding (may be empty)
     */
    @Query("SELECT p.id FROM Product p WHERE p.embedding IS NULL")
    List<Long> findIdsWithNullEmbedding();

    /**
     * Returns all products with a {@code NULL} embedding, eagerly fetching the
     * {@code category} association in a single JOIN so that
     * {@link com.example.smartcatalog.service.ProductEmbeddingService} can safely
     * read {@code product.getCategory().getName()} on the detached entity without
     * triggering a {@code LazyInitializationException}.
     *
     * @return list of products that still need embeddings
     */
    @EntityGraph(attributePaths = "category")
    @Query("SELECT p FROM Product p WHERE p.embedding IS NULL")
    List<Product> findAllWithNullEmbedding();

    /**
     * Writes only the {@code embedding} column of one product, bypassing the JPA merge
     * (no extra SELECT, no version bump). Used by the bulk backfill.
     *
     * <p>{@code embedding IS NULL} guards against overwriting a fresher vector written
     * by a concurrent {@code embedAndSave} after the product was edited mid-backfill.</p>
     *
     * @param id     the product primary key
     * @param vector the embedding formatted as a pgvector string literal
     * @return number of rows updated (0 if the product already had an embedding)
     */
    @Modifying
    @Query(value = "UPDATE products SET embedding = CAST(:vector AS vector) " +
                   "WHERE id = :id AND embedding IS NULL",
           nativeQuery = true)
    int updateEmbeddingIfMissing(@Param("id") Long id, @Param("vector") String vector);

    /**
     * Performs a semantic search for products using pgvector's cosine distance operator (<=>).
     * 
     * <p>A native query is required because JPQL does not natively support pgvector operators.
     * We cast the provided string to a vector inside the query.</p>
     * 
     * @param vector the query embedding formatted as a pgvector string literal (e.g. "[0.1, 0.2, ...]")
     * @param maxPrice optional maximum price filter (if null, price check is bypassed)
     * @return the top 10 most semantically similar products
     */
    @Query(value = "SELECT * FROM products " +
                   "WHERE (:maxPrice IS NULL OR price <= CAST(:maxPrice AS NUMERIC)) " +
                    "AND (embedding <=> CAST(:vector AS vector)) < 0.4 " +
                    "ORDER BY embedding <=> CAST(:vector AS vector) " +
                   "LIMIT 10",
           nativeQuery = true)
    List<Product> searchBySimilarityAndPrice(
            @org.springframework.data.repository.query.Param("vector") String vector,
            @org.springframework.data.repository.query.Param("maxPrice") java.math.BigDecimal maxPrice);
}


