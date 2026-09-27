package com.example.smartcatalog.mapper;

import com.example.smartcatalog.dto.CategoryDto;
import com.example.smartcatalog.model.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper that converts between {@link Category} entities and
 * {@link CategoryDto} transfer objects.
 *
 * <p>{@code componentModel = "spring"} causes MapStruct to generate an
 * implementation that is a Spring-managed bean, injectable via
 * {@code @Autowired} anywhere in the application.</p>
 *
 * <p>Because {@link CategoryDto} and {@link Category} share identical field names
 * ({@code id}, {@code name}, {@code description}, {@code createdAt},
 * {@code updatedAt}), no explicit {@code @Mapping} is needed for {@code toDto}.
 * JPA / auditing fields that must never be set from an incoming payload are
 * explicitly ignored in the write-direction methods.</p>
 */
@Mapper(componentModel = "spring")
public interface CategoryMapper {

    /**
     * Entity → DTO (used in GET responses and after POST / PUT).
     *
     * @param category the persisted entity
     * @return a fully populated {@link CategoryDto}
     */
    CategoryDto toDto(Category category);

    /**
     * DTO → Entity (used when creating a new category via POST).
     *
     * <p>{@code id}, audit timestamps, {@code version}, and the {@code products}
     * collection are managed by JPA / Spring Data Auditing and must never be set
     * from an incoming request payload.</p>
     *
     * @param dto the validated incoming request body
     * @return a transient {@link Category} ready for {@code repository.save()}
     */
    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version",   ignore = true)
    @Mapping(target = "products",  ignore = true) // managed by the Product side
    Category toEntity(CategoryDto dto);

    /**
     * Merges updated fields from a DTO into an existing managed entity (used for PUT).
     *
     * <p>JPA-managed fields are left untouched so auditing and optimistic-locking
     * continue to work correctly.</p>
     *
     * @param dto    the incoming update payload
     * @param entity the existing managed entity to update in-place
     */
    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version",   ignore = true)
    @Mapping(target = "products",  ignore = true)
    void updateEntityFromDto(CategoryDto dto, @MappingTarget Category entity);
}
