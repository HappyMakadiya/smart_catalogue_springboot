package com.example.smartcatalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Public-facing representation of a {@link com.example.smartcatalog.model.Product}.
 *
 * <ul>
 *   <li>Used as <em>response body</em> for GET / POST / PUT endpoints.</li>
 *   <li>Used as <em>request body</em> for POST (create) and PUT (update) endpoints —
 *       validation annotations ensure the payload is always well-formed.</li>
 * </ul>
 */
@Data
public class ProductDto {

    /** Present in responses; ignored / null in create requests. */
    private Long id;

    @NotBlank(message = "Product name must not be blank")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @PositiveOrZero(message = "Price must be zero or positive")
    private String price;

    @PositiveOrZero(message = "Stock must be zero or positive")
    private Integer stock;

    /**
     * Category reference.
     * <ul>
     *   <li>In <strong>responses</strong>: fully populated {@link CategoryDto}.</li>
     *   <li>In <strong>requests</strong>: only {@code id} needs to be set so the
     *       service can look up the {@link com.example.smartcatalog.model.Category}.</li>
     * </ul>
     */
    private CategoryDto category;

    /** Populated by the server; ignored when sent by clients. */
    private LocalDateTime createdAt;

    /** Populated by the server; ignored when sent by clients. */
    private LocalDateTime updatedAt;
}
