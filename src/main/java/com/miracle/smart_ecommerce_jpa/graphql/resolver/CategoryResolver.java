package com.miracle.smart_ecommerce_jpa.graphql.resolver;

import com.miracle.smart_ecommerce_jpa.domain.category.dto.CategoryResponse;
import com.miracle.smart_ecommerce_jpa.domain.category.dto.CreateCategoryRequest;
import com.miracle.smart_ecommerce_jpa.domain.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GraphQL Resolver for Category entity.
 * Handles all category-related queries and mutations.
 */
@Controller
@RequiredArgsConstructor
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
    public List<CategoryResponse> categories() {
        return categoryService.getAllCategories();
    }


    // ========================================================================
    // CATEGORY MUTATIONS
    // ========================================================================

    @MutationMapping
    public CategoryResponse createCategory(@Argument Map<String, Object> input) {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .categoryName((String) input.get("categoryName"))
                .build();
        return categoryService.createCategory(request);
    }

    @MutationMapping
    public CategoryResponse updateCategory(@Argument UUID id, @Argument Map<String, Object> input) {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .categoryName((String) input.get("categoryName"))
                .build();
        return categoryService.updateCategory(id, request);
    }

    @MutationMapping
    public boolean deleteCategory(@Argument UUID id) {
        categoryService.deleteCategory(id);
        return true;
    }
}

