package com.miracle.smart_ecommerce_jpa.graphql.resolver;

import com.miracle.smart_ecommerce_jpa.annotation.RequireRoles;
import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.category.dto.CategoryResponse;
import com.miracle.smart_ecommerce_jpa.domain.category.dto.CreateCategoryRequest;
import com.miracle.smart_ecommerce_jpa.domain.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.UUID;

/**
 * GraphQL Resolver for Category entity.
 * Handles all category-related queries and mutations.
 */
@Controller
@RequiredArgsConstructor
@RequireRoles({"ADMIN", "CUSTOMER"})
public class CategoryResolver {

    private final CategoryService categoryService;

    // ========================================================================
    // CATEGORY QUERIES
    // ========================================================================

    @QueryMapping
    @RequireRoles({"ADMIN", "CUSTOMER"})
    public CategoryResponse category(@Argument UUID id) {
        return categoryService.getCategoryById(id);
    }

    @QueryMapping
    @RequireRoles({"ADMIN", "CUSTOMER"})
    public PageResponse<CategoryResponse> categories(@Argument Integer page, @Argument Integer size) {
        int p = (page == null) ? 0 : page;
        int s = (size == null) ? 10 : size;
        return categoryService.getAllCategories(PageRequest.of(p, s));
    }


    // ========================================================================
    // CATEGORY MUTATIONS
    // ========================================================================

    @MutationMapping
    @RequireRoles({"ADMIN"})
    public CategoryResponse createCategory(@Argument Map<String, Object> input) {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .categoryName((String) input.get("categoryName"))
                .build();
        return categoryService.createCategory(request);
    }

    @MutationMapping
    @RequireRoles({"ADMIN"})
    public CategoryResponse updateCategory(@Argument UUID id, @Argument Map<String, Object> input) {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .categoryName((String) input.get("categoryName"))
                .build();
        return categoryService.updateCategory(id, request);
    }

    @MutationMapping
    @RequireRoles({"ADMIN"})
    public boolean deleteCategory(@Argument UUID id) {
        categoryService.deleteCategory(id);
        return true;
    }
}
