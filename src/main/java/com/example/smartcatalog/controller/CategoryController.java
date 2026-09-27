package com.example.smartcatalog.controller;

import com.example.smartcatalog.dto.ApiResponse;
import com.example.smartcatalog.dto.CategoryDto;
import com.example.smartcatalog.service.CategoryService;
import com.example.smartcatalog.util.ApiResponseUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller that exposes the category catalogue endpoints.
 *
 * <h3>Endpoint summary</h3>
 * <pre>
 * GET    /api/category          – paginated list of all categories
 * GET    /api/category/{id}     – single category by id
 * POST   /api/category          – create a new category
 * PUT    /api/category/{id}     – full update of an existing category
 * </pre>
 *
 * <h3>Pagination</h3>
 * <p>The GET list endpoint accepts standard Spring Data query parameters:
 * {@code ?page=0&size=10&sort=name,asc}. Defaults are page 0, size 10,
 * sorted by {@code name} ascending.</p>
 *
 * <h3>Response envelope</h3>
 * <p>Every response is wrapped in the shared {@link ApiResponse} envelope
 * produced by {@link ApiResponseUtil}.</p>
 */
@RestController
@RequestMapping("/api/category")
public class CategoryController {

    @Autowired
    private CategoryService service;

    // -----------------------------------------------------------------------
    // GET – list (paginated)
    // -----------------------------------------------------------------------

    /**
     * Returns a paginated list of categories.
     *
     * <p>Query parameters (all optional):
     * <ul>
     *   <li>{@code page}  – zero-based page index (default: {@code 0})</li>
     *   <li>{@code size}  – number of items per page (default: {@code 10})</li>
     *   <li>{@code sort}  – field and direction, e.g. {@code sort=name,desc}</li>
     * </ul>
     *
     * @param pageable resolved automatically from request query parameters
     * @return 200 OK with a {@link Page} of {@link CategoryDto} inside the envelope
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllCategories(
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        Page<CategoryDto> page = service.getAllCategories(pageable);

        Map<String, Object> body = Map.of(
                "categories",  page.getContent(),
                "currentPage", page.getNumber(),
                "totalItems",  page.getTotalElements(),
                "totalPages",  page.getTotalPages(),
                "pageSize",    page.getSize()
        );

        return ApiResponseUtil.ok("Categories fetched successfully", body);
    }

    // -----------------------------------------------------------------------
    // GET – single category
    // -----------------------------------------------------------------------

    /**
     * Fetches a single category by its primary key.
     *
     * @param id the category id from the URL path
     * @return 200 OK with the {@link CategoryDto}, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCategoryById(
            @PathVariable Long id
    ) {
        CategoryDto category = service.getCategoryById(id);

        return ApiResponseUtil.ok(
                "Category fetched successfully",
                Map.of("category", category)
        );
    }

    // -----------------------------------------------------------------------
    // POST – create
    // -----------------------------------------------------------------------

    /**
     * Creates a new category.
     *
     * <p>Category names are unique — a {@code 409 Conflict} is returned if the
     * name is already taken (case-insensitive).</p>
     *
     * @param dto validated request body ({@code name} is required)
     * @return 201 Created with the persisted {@link CategoryDto}
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createCategory(
            @Valid @RequestBody CategoryDto dto
    ) {
        CategoryDto created = service.createCategory(dto);

        return ApiResponseUtil.success(
                "Category created successfully",
                Map.of("category", created),
                HttpStatus.CREATED
        );
    }

    // -----------------------------------------------------------------------
    // PUT – full update
    // -----------------------------------------------------------------------

    /**
     * Fully replaces the mutable fields of an existing category (HTTP PUT semantics).
     *
     * @param id  the category id from the URL path
     * @param dto validated request body with the new values
     * @return 200 OK with the updated {@link CategoryDto}, or 404 / 409 on error
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryDto dto
    ) {
        CategoryDto updated = service.updateCategory(id, dto);

        return ApiResponseUtil.ok(
                "Category updated successfully",
                Map.of("category", updated)
        );
    }
}
