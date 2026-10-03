package com.example.smartcatalog.model;

import com.example.smartcatalog.config.VectorFloatConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@NoArgsConstructor
@AllArgsConstructor
@Data
@EntityListeners(AuditingEntityListener.class)
@Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @PositiveOrZero
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @PositiveOrZero // Enforces the CHECK (stock >= 0) at the application level
    @Column(nullable = false)
    private Integer stock = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L; // Enables Optimistic Locking

    /**
     * Semantic embedding vector produced by Gemini gemini-embedding-001 (truncated to 768 dims).
     * Populated asynchronously by {@link com.example.smartcatalog.service.ProductEmbeddingService}
     * after each create/update. Null until the first embedding job completes.
     */
    @Convert(converter = VectorFloatConverter.class)
    @Column(name = "embedding", columnDefinition = "vector(768)")
    @org.hibernate.annotations.ColumnTransformer(read = "embedding::text", write = "?::vector")
    private float[] embedding;

}
