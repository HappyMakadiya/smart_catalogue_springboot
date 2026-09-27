package com.example.smartcatalog.mapper;

import com.example.smartcatalog.dto.ProductDto;
import com.example.smartcatalog.model.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper that converts between {@link Product} entities and
 * {@link ProductDto} transfer objects.
 *
 * <p>{@code componentModel = "spring"} tells MapStruct to generate an
 * implementation that is a Spring bean, so it can be injected with
 * {@code @Autowired} / constructor injection anywhere in the application.</p>
 *
 * <p>The nested {@code category} field is mapped automatically because
 * {@link com.example.smartcatalog.dto.CategoryDto} has the same field names as
 * {@link com.example.smartcatalog.model.Category}.</p>
 */
@Mapper(componentModel = "spring")
public interface ProductMapper {

    /**
     * Entity → DTO (used in GET responses and after POST / PUT).
     *
     * @param product the persisted entity (category association already loaded
     *                via {@code @EntityGraph})
     * @return a fully populated {@link ProductDto}
     */
    ProductDto toDto(Product product);

    /**
     * DTO → Entity (used when creating a new product via POST).
     *
     * <p>{@code id}, {@code createdAt}, and {@code updatedAt} are managed by
     * JPA / Spring Data Auditing and must not be set from the incoming payload.</p>
     *
     * @param dto the incoming request body
     * @return a transient {@link Product} ready for {@code repository.save()}
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "category", ignore = true) // resolved by service via categoryId
    Product toEntity(ProductDto dto);

    /**
     * Merges updated fields from a DTO into an existing managed entity (used for PUT).
     *
     * <p>JPA-managed fields ({@code id}, {@code createdAt}, {@code updatedAt},
     * {@code version}) are left untouched so that auditing and optimistic-locking
     * keep working correctly.</p>
     *
     * @param dto    the incoming update payload
     * @param entity the existing managed entity to be updated in-place
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "category", ignore = true) // resolved by service via categoryId
    void updateEntityFromDto(ProductDto dto, @MappingTarget Product entity);
}
