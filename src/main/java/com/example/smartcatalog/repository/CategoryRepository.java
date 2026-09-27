package com.example.smartcatalog.repository;

import com.example.smartcatalog.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence layer for {@link Category}.
 *
 * <h3>Pagination</h3>
 * <p>{@link JpaRepository} already inherits {@code findAll(Pageable)} from
 * {@link org.springframework.data.repository.PagingAndSortingRepository}, so no
 * custom query method is needed for the paginated list. The method is re-declared
 * here with a narrower return type ({@link Page}) purely for readability and to
 * make IDE auto-complete explicit.</p>
 *
 * <h3>Uniqueness check</h3>
 * <p>{@link #existsByNameIgnoreCase(String)} is used by the service layer to give
 * callers a clear {@code 409 Conflict} before hitting the database unique constraint,
 * producing a friendlier error message than a raw {@code DataIntegrityViolationException}.</p>
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Returns a page of categories sorted / sized according to {@code pageable}.
     *
     * @param pageable page number, size, and sort specification
     * @return a {@link Page} of {@link Category} entities (never {@code null})
     */
    Page<Category> findAll(Pageable pageable);

    /**
     * Checks whether a category with the given name already exists
     * (case-insensitive), used to prevent duplicate category names.
     *
     * @param name the name to check
     * @return {@code true} if a category with that name already exists
     */
    boolean existsByNameIgnoreCase(String name);
}
