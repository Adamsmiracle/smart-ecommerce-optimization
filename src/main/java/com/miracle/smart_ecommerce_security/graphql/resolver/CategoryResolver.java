package com.miracle.smart_ecommerce_security.graphql.resolver;

import com.miracle.smart_ecommerce_security.domain.category.dto.CategoryResponse;
import com.miracle.smart_ecommerce_security.domain.category.dto.CreateCategoryRequest;
import com.miracle.smart_ecommerce_security.domain.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.UUID;

/**
 * GraphQL Resolver for Category entity.
 * Handles all category-related queries and mutations.
 *
 * Access: Queries open to ADMIN + CUSTOMER; mutations restricted to ADMIN.
 */
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
public class CategoryResolver {

    private final CategoryService categoryService;

    // ========================================================================
    // CATEGORY QUERIES
    // ========================================================================

    @QueryMapping
    public CategoryResponse category(@Argument UUID id) {
        return categoryService.getCategoryById(id);
    }

    @QueryMapping
    public Page<CategoryResponse> categories(@Argument Integer page, @Argument Integer size) {
        int p = (page == null) ? 0 : page;
        int s = (size == null) ? 10 : size;
        return categoryService.getAllCategories(PageRequest.of(p, s));
    }

    // ========================================================================
    // CATEGORY MUTATIONS
    // ========================================================================

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public CategoryResponse createCategory(@Argument Map<String, Object> input) {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .categoryName((String) input.get("categoryName"))
                .build();
        return categoryService.createCategory(request);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public CategoryResponse updateCategory(@Argument UUID id, @Argument Map<String, Object> input) {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .categoryName((String) input.get("categoryName"))
                .build();
        return categoryService.updateCategory(id, request);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public boolean deleteCategory(@Argument UUID id) {
        categoryService.deleteCategory(id);
        return true;
    }
}
