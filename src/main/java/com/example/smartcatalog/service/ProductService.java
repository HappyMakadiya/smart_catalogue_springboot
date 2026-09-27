package com.example.smartcatalog.service;

import com.example.smartcatalog.dto.ProductDto;
import com.example.smartcatalog.exception.custom.ResourceNotFoundException;
import com.example.smartcatalog.mapper.ProductMapper;
import com.example.smartcatalog.model.Category;
import com.example.smartcatalog.model.Product;
import com.example.smartcatalog.repository.CategoryRepository;
import com.example.smartcatalog.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for the product catalogue.
 *
 * <p>This service is the single integration point between the HTTP layer
 * ({@link com.example.smartcatalog.controller.ProductController}) and the
 * persistence layer ({@link ProductRepository}). All public methods that write
 * to the database are annotated with {@link Transactional} to guarantee
 * atomicity.</p>
 */
@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductMapper productMapper;

    // -----------------------------------------------------------------------
    // Read
    // -----------------------------------------------------------------------

    /**
     * Returns a paginated slice of all products.
     *
     * <p>The underlying repository method uses {@code @EntityGraph} to JOIN FETCH
     * the category in a single SQL query, preventing N+1 issues.</p>
     *
     * @param pageable page number, size, and optional sort (e.g. {@code ?page=0&size=10&sort=name,asc})
     * @return a {@link Page} of {@link ProductDto} objects
     */
    @Transactional(readOnly = true)
    public Page<ProductDto> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(productMapper::toDto);
    }

    /**
     * Fetches a single product by its identifier.
     *
     * @param id the product primary key
     * @return the matching {@link ProductDto}
     * @throws ResourceNotFoundException if no product with the given {@code id} exists
     */
    @Transactional(readOnly = true)
    public ProductDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return productMapper.toDto(product);
    }

    // -----------------------------------------------------------------------
    // Write
    // -----------------------------------------------------------------------

    /**
     * Persists a new product.
     *
     * <p>If {@code dto.getCategory().getId()} is provided, the category entity is
     * looked up and associated. If no category is supplied the product is saved
     * without one.</p>
     *
     * @param dto validated request body from the controller
     * @return the saved product as a {@link ProductDto} (includes generated {@code id})
     * @throws ResourceNotFoundException if the supplied category id does not exist
     */
    @Transactional
    public ProductDto createProduct(ProductDto dto) {
        Product product = productMapper.toEntity(dto);
        resolveCategory(dto, product);
        Product saved = productRepository.save(product);
        return productMapper.toDto(saved);
    }

    /**
     * Replaces the mutable fields of an existing product with the values from
     * {@code dto} (full update / HTTP PUT semantics).
     *
     * @param id  the primary key of the product to update
     * @param dto validated request body from the controller
     * @return the updated product as a {@link ProductDto}
     * @throws ResourceNotFoundException if no product with the given {@code id} exists,
     *                                   or if the supplied category id does not exist
     */
    @Transactional
    public ProductDto updateProduct(Long id, ProductDto dto) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        productMapper.updateEntityFromDto(dto, existing);
        resolveCategory(dto, existing);
        Product saved = productRepository.save(existing);
        return productMapper.toDto(saved);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Resolves the {@link Category} entity from the DTO's nested category id and
     * assigns it to the given {@code product}.
     *
     * <p>If the DTO contains no category reference (or the category id is
     * {@code null}), the product's category is left as-is / set to {@code null}.</p>
     *
     * @param dto     the incoming transfer object
     * @param product the entity being created or updated
     */
    private void resolveCategory(ProductDto dto, Product product) {
        if (dto.getCategory() != null && dto.getCategory().getId() != null) {
            Long categoryId = dto.getCategory().getId();
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }
    }
}
