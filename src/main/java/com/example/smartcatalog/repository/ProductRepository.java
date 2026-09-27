package com.example.smartcatalog.repository;

import com.example.smartcatalog.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
