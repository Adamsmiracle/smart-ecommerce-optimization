package com.miracle.smart_ecommerce_jpa.domain.user.controller;

import com.miracle.smart_ecommerce_jpa.common.response.ApiResponse;
import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.user.dto.request.CreateUserRequest;
import com.miracle.smart_ecommerce_jpa.domain.user.dto.request.UpdateUserRequest;
import com.miracle.smart_ecommerce_jpa.domain.user.dto.response.UserResponse;
import com.miracle.smart_ecommerce_jpa.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for User management.
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management APIs")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @Operation(summary = "Create a new user", description = "Creates a new user account")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email already exists")
    })
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(user, "User created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieves a user by their unique ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @Parameter(description = "User ID") @PathVariable UUID id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get user by email", description = "Retrieves a user by their email address")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByEmail(
            @Parameter(description = "User email") @PathVariable String email) {
        UserResponse user = userService.getUserByEmail(email);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieves all users with pagination")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
            @PageableDefault(size = 10) Pageable pageable) {
        PageResponse<UserResponse> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/search")
    @Operation(summary = "Search users", description = "Search users by keyword (name or email)")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> searchUsers(
            @Parameter(description = "Search keyword") @RequestParam String keyword,
            @PageableDefault(size = 10) Pageable pageable) {
        PageResponse<UserResponse> users = userService.searchUsers(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Updates an existing user")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @Parameter(description = "User ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        UserResponse user = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success(user, "User updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Deletes a user by ID")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "User ID") @PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully"));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate user", description = "Activates a user account")
    public ResponseEntity<ApiResponse<Void>> activateUser(
            @Parameter(description = "User ID") @PathVariable UUID id) {
        userService.activateUser(id);
        return ResponseEntity.ok(ApiResponse.success("User activated successfully"));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate user", description = "Deactivates a user account")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(
            @Parameter(description = "User ID") @PathVariable UUID id) {
        userService.deactivateUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deactivated successfully"));
    }

//    @PatchMapping("/{id}/roles")
//    @Operation(summary = "Update user roles", description = "Admin: update roles for a user")
//    public ResponseEntity<ApiResponse<Void>> updateRoles(
//            @Parameter(description = "User ID") @PathVariable UUID id,
//            @Valid @RequestBody UpdateRolesRequest request) {
//        userService.updateUserRoles(id, request.getRoles());
//        return ResponseEntity.ok(ApiResponse.success("User roles updated successfully"));
//    }
}
