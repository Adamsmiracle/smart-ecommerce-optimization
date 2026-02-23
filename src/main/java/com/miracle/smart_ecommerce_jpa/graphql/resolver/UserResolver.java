package com.miracle.smart_ecommerce_jpa.graphql.resolver;

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
    public UserResponse user(@Argument UUID id) {
        return userService.getUserById(id);
    }

    @QueryMapping
    public UserResponse userByEmail(@Argument String email) {
        return userService.getUserByEmail(email);
    }

    @QueryMapping
    public PageResponse<UserResponse> users(@Argument int page, @Argument int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userService.getAllUsers(pageable);
    }

    @QueryMapping
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
    public boolean deleteUser(@Argument UUID id) {
        userService.deleteUser(id);
        return true;
    }

    @MutationMapping
    public boolean activateUser(@Argument UUID id) {
        userService.activateUser(id);
        return true;
    }

    @MutationMapping
    public boolean deactivateUser(@Argument UUID id) {
        userService.deactivateUser(id);
        return true;
    }
}

