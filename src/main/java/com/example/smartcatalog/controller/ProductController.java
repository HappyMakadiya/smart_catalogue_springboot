package com.example.smartcatalog.controller;

import com.example.smartcatalog.dto.ApiResponse;
import com.example.smartcatalog.dto.ProductDto;
import com.example.smartcatalog.service.ProductService;
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
 * REST controller that exposes the product catalogue endpoints.
 *
 * <h3>Endpoint summary</h3>
 * <pre>
 * GET    /api/product          – paginated list of all products
 * GET    /api/product/{id}     – single product by id
 * POST   /api/product          – create a new product
 * PUT    /api/product/{id}     – full update of an existing product
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
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductService service;

    // -----------------------------------------------------------------------
    // GET – list (paginated)
    // -----------------------------------------------------------------------

    /**
     * Returns a paginated list of products with their categories pre-loaded
     * (N+1 free thanks to {@code @EntityGraph} in the repository).
     *
     * <p>Query parameters (all optional):
     * <ul>
     *   <li>{@code page}  – zero-based page index (default: {@code 0})</li>
     *   <li>{@code size}  – number of items per page (default: {@code 10})</li>
     *   <li>{@code sort}  – field and direction, e.g. {@code sort=price,desc}</li>
     * </ul>
     *
     * @param pageable resolved automatically from request query parameters
     * @return 200 OK with a {@link Page} of {@link ProductDto} inside the envelope
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllProducts(
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        Page<ProductDto> page = service.getAllProducts(pageable);

        Map<String, Object> body = Map.of(
                "products",       page.getContent(),
                "currentPage",    page.getNumber(),
                "totalItems",     page.getTotalElements(),
                "totalPages",     page.getTotalPages(),
                "pageSize",       page.getSize()
        );

        return ApiResponseUtil.ok("Products fetched successfully", body);
    }

    // -----------------------------------------------------------------------
    // GET – single product
    // -----------------------------------------------------------------------

    /**
     * Fetches a single product by its primary key.
     *
     * @param id the product id from the URL path
     * @return 200 OK with the {@link ProductDto}, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProductById(
            @PathVariable Long id
    ) {
        ProductDto product = service.getProductById(id);

        return ApiResponseUtil.ok(
                "Product fetched successfully",
                Map.of("product", product)
        );
    }

    // -----------------------------------------------------------------------
    // POST – create
    // -----------------------------------------------------------------------

    /**
     * Creates a new product.
     *
     * <p>To link the product to a category, include {@code "category": {"id": 1}}
     * in the request body. Omit the category field to create an uncategorised product.</p>
     *
     * @param dto validated request body
     * @return 201 Created with the persisted {@link ProductDto}
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createProduct(
            @Valid @RequestBody ProductDto dto
    ) {
        ProductDto created = service.createProduct(dto);

        return ApiResponseUtil.success(
                "Product created successfully",
                Map.of("product", created),
                HttpStatus.CREATED
        );
    }

    // -----------------------------------------------------------------------
    // PUT – full update
    // -----------------------------------------------------------------------

    /**
     * Fully replaces the mutable fields of an existing product (HTTP PUT semantics).
     *
     * @param id  the product id from the URL path
     * @param dto validated request body with the new values
     * @return 200 OK with the updated {@link ProductDto}, or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductDto dto
    ) {
        ProductDto updated = service.updateProduct(id, dto);

        return ApiResponseUtil.ok(
                "Product updated successfully",
                Map.of("product", updated)
        );
    }
}
