package com.miracle.smart_ecommerce_security.graphql.resolver;

import com.miracle.smart_ecommerce_security.domain.user.dto.request.CreateUserRequest;
import com.miracle.smart_ecommerce_security.domain.user.dto.request.UpdateUserRequest;
import com.miracle.smart_ecommerce_security.domain.user.dto.response.UserResponse;
import com.miracle.smart_ecommerce_security.domain.user.service.UserService;
import com.miracle.smart_ecommerce_security.graphql.type.GraphQLPage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.UUID;

/**
 * GraphQL Resolver for User entity.
 * Handles all user-related queries and mutations.
 *
 * Access: ADMIN + CUSTOMER for own-profile operations; ADMIN-only for user management.
 */
@Controller
@RequiredArgsConstructor
public class UserResolver {

    private final UserService userService;

    // ========================================================================
    // USER QUERIES
    // ========================================================================

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
    public UserResponse user(@Argument UUID id) {
        return userService.getUserById(id);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public UserResponse userByEmail(@Argument String email) {
        return userService.getUserByEmail(email);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public GraphQLPage<UserResponse> users(@Argument int page, @Argument int size) {
        Pageable pageable = PageRequest.of(page, size);
        return GraphQLPage.of(userService.getAllUsers(pageable));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public GraphQLPage<UserResponse> searchUsers(@Argument String keyword,
                                                 @Argument int page,
                                                 @Argument int size) {
        Pageable pageable = PageRequest.of(page, size);
        return GraphQLPage.of(userService.searchUsers(keyword, pageable));
    }

    // ========================================================================
    // USER MUTATIONS
    // ========================================================================

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
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
    @PreAuthorize("hasRole('ADMIN')")
    public boolean deleteUser(@Argument UUID id) {
        userService.deleteUser(id);
        return true;
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public boolean activateUser(@Argument UUID id) {
        userService.activateUser(id);
        return true;
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public boolean deactivateUser(@Argument UUID id) {
        userService.deactivateUser(id);
        return true;
    }
}
