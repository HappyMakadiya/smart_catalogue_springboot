package com.example.smartcatalog.service;

import com.example.smartcatalog.dto.CategoryDto;
import com.example.smartcatalog.exception.custom.DuplicateResourceException;
import com.example.smartcatalog.exception.custom.ResourceNotFoundException;
import com.example.smartcatalog.mapper.CategoryMapper;
import com.example.smartcatalog.model.Category;
import com.example.smartcatalog.repository.CategoryRepository;
import com.opencsv.CSVReaderHeaderAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Business logic for the category catalogue.
 *
 * <p>All public methods that write to the database are annotated with
 * {@link Transactional} to guarantee atomicity. Read-only methods carry
 * {@code readOnly = true} to hint to the JPA provider that no dirty-checking
 * flush is required, which can improve performance on large result sets.</p>
 */
@Service
public class CategoryService {

    @Autowired
    private CategoryRepository repository;

    @Autowired
    private CategoryMapper categoryMapper;

    // -----------------------------------------------------------------------
    // Read
    // -----------------------------------------------------------------------

    /**
     * Returns a paginated slice of all categories.
     *
     * @param pageable page number, size, and optional sort
     *                 (e.g. {@code ?page=0&size=10&sort=name,asc})
     * @return a {@link Page} of {@link CategoryDto} objects
     */
    @Transactional(readOnly = true)
    public Page<CategoryDto> getAllCategories(Pageable pageable) {
        return repository.findAll(pageable)
                .map(categoryMapper::toDto);
    }

    /**
     * Fetches a single category by its primary key.
     *
     * @param id the category primary key
     * @return the matching {@link CategoryDto}
     * @throws ResourceNotFoundException if no category with the given {@code id} exists
     */
    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(Long id) {
        Category category = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        return categoryMapper.toDto(category);
    }

    // -----------------------------------------------------------------------
    // Write
    // -----------------------------------------------------------------------

    /**
     * Persists a new category.
     *
     * <p>Category names are unique (enforced both at the DB level and here via a
     * pre-check to give a cleaner {@code 409 Conflict} response).</p>
     *
     * @param dto validated request body from the controller
     * @return the saved category as a {@link CategoryDto} (includes generated {@code id})
     * @throws DuplicateResourceException if a category with the same name already exists
     */
    @Transactional
    public CategoryDto createCategory(CategoryDto dto) {
        if (repository.existsByNameIgnoreCase(dto.getName())) {
            throw new DuplicateResourceException("Category", "name", dto.getName());
        }
        Category category = categoryMapper.toEntity(dto);
        return categoryMapper.toDto(repository.save(category));
    }

    /**
     * Fully replaces the mutable fields of an existing category (HTTP PUT semantics).
     *
     * <p>If the name is being changed to one that is already taken by
     * <em>another</em> category, a {@code 409 Conflict} is raised.</p>
     *
     * @param id  the primary key of the category to update
     * @param dto validated request body from the controller
     * @return the updated category as a {@link CategoryDto}
     * @throws ResourceNotFoundException  if no category with the given {@code id} exists
     * @throws DuplicateResourceException if the new name is already used by a different category
     */
    @Transactional
    public CategoryDto updateCategory(Long id, CategoryDto dto) {
        Category existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        // Only reject the name if it belongs to a DIFFERENT category
        boolean nameConflict = !existing.getName().equalsIgnoreCase(dto.getName())
                && repository.existsByNameIgnoreCase(dto.getName());
        if (nameConflict) {
            throw new DuplicateResourceException("Category", "name", dto.getName());
        }

        categoryMapper.updateEntityFromDto(dto, existing);
        return categoryMapper.toDto(repository.save(existing));
    }


    public void seedCategoriesFromCsv(MultipartFile file) throws Exception {
        List<Category> categories = new ArrayList<>();

        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"));
             CSVReaderHeaderAware csvReader = new CSVReaderHeaderAware(fileReader)) {

            Map<String, String> values;
            while ((values = csvReader.readMap()) != null) {
                Category category = Category.builder().name(values.get("name")).description(values.get("description")).build();
                categories.add(category);
            }
        }

        // Save all records efficiently to the database
        repository.saveAll(categories);
    }
}
