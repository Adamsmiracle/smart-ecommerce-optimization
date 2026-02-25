package com.miracle.smart_ecommerce_jpa.graphql.resolver;

import com.miracle.smart_ecommerce_jpa.annotation.RequireRoles;
import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.user.dto.request.CreateUserRequest;
import com.miracle.smart_ecommerce_jpa.domain.user.dto.request.UpdateUserRequest;
import com.miracle.smart_ecommerce_jpa.domain.user.dto.response.UserResponse;
import com.miracle.smart_ecommerce_jpa.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.UUID;

/**
 * GraphQL Resolver for User entity.
 * Handles all user-related queries and mutations.
 */
@Controller
@RequiredArgsConstructor
public class UserResolver {

    private final UserService userService;

    // ========================================================================
    // USER QUERIES
    // ========================================================================

    @QueryMapping
    @RequireRoles({"ADMIN", "CUSTOMER"})
    public UserResponse user(@Argument UUID id) {
        return userService.getUserById(id);
    }

    @QueryMapping
    @RequireRoles({"ADMIN"})
    public UserResponse userByEmail(@Argument String email) {
        return userService.getUserByEmail(email);
    }

    @QueryMapping
    @RequireRoles({"ADMIN"})
    public PageResponse<UserResponse> users(@Argument int page, @Argument int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userService.getAllUsers(pageable);
    }

    @QueryMapping
    @RequireRoles({"ADMIN"})
    public PageResponse<UserResponse> searchUsers(@Argument String keyword,
                                                   @Argument int page,
                                                   @Argument int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userService.searchUsers(keyword, pageable);
    }

    // ========================================================================
    // USER MUTATIONS
    // ========================================================================

    @MutationMapping
    @RequireRoles({"ADMIN", "CUSTOMER"})
    public UserResponse createUser(@Argument Map<String, Object> input) {
        CreateUserRequest request = CreateUserRequest.builder()
                .emailAddress((String) input.get("emailAddress"))
                .firstName((String) input.get("firstName"))
                .lastName((String) input.get("lastName"))
                .phoneNumber((String) input.get("phoneNumber"))
                .password((String) input.get("password"))
                .build();
        return userService.createUser(request);
    }

    @MutationMapping
    @RequireRoles({"ADMIN", "CUSTOMER"})
    public UserResponse updateUser(@Argument UUID id, @Argument Map<String, Object> input) {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .emailAddress((String) input.get("emailAddress"))
                .firstName((String) input.get("firstName"))
                .lastName((String) input.get("lastName"))
                .phoneNumber((String) input.get("phoneNumber"))
                .role((String) input.get("role"))
                .build();
        return userService.updateUser(id, request);
    }

    @MutationMapping
    @RequireRoles({"ADMIN"})
    public boolean deleteUser(@Argument UUID id) {
        userService.deleteUser(id);
        return true;
    }

    @MutationMapping
    @RequireRoles({"ADMIN"})
    public boolean activateUser(@Argument UUID id) {
        userService.activateUser(id);
        return true;
    }

    @MutationMapping
    @RequireRoles({"ADMIN"})
    public boolean deactivateUser(@Argument UUID id) {
        userService.deactivateUser(id);
        return true;
    }
}

