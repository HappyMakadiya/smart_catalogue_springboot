package com.example.smartcatalog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Public-facing representation of a {@link com.example.smartcatalog.model.Category}.
 *
 * <ul>
 *   <li>Used as <em>response body</em> for GET / POST / PUT endpoints.</li>
 *   <li>Used as <em>request body</em> for POST (create) and PUT (update) — the
 *       {@code @NotBlank} constraint on {@code name} guards incoming payloads.</li>
 *   <li>Also embedded inside {@link ProductDto#getCategory()} so product responses
 *       carry category data without triggering extra queries.</li>
 * </ul>
 */
@Data
public class CategoryDto {

    /** Present in responses; ignored / null in create requests. */
    private Long id;

    @NotBlank(message = "Category name must not be blank")
    private String name;

    private String description;

    /** Populated by the server; ignored when sent by clients. */
    private LocalDateTime createdAt;

    /** Populated by the server; ignored when sent by clients. */
    private LocalDateTime updatedAt;
}
