package com.miracle.smart_ecommerce_jpa.domain.order.controller;

import com.miracle.smart_ecommerce_jpa.common.response.ApiResponse;
import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.CreateOrderRequest;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.OrderResponse;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.UpdateOrderRequest;
import com.miracle.smart_ecommerce_jpa.domain.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for Order management.
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order management APIs")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Create a new order", description = "Places a new order (checkout)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Order created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User or product not found")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(order, "Order placed successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Retrieves an order by its unique ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @Parameter(description = "Order ID") @PathVariable String id) {
        try {
            UUID uuid = UUID.fromString(id);
            OrderResponse order = orderService.getOrderById(uuid);
            return ResponseEntity.ok(ApiResponse.success(order));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid UUID format: " + id, 400));
        }
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Get order by order number", description = "Retrieves an order by its order number")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByOrderNumber(
            @Parameter(description = "Order Number") @PathVariable String orderNumber) {
        OrderResponse order = orderService.getOrderByOrderNumber(orderNumber);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping
    @Operation(summary = "Get all orders", description = "Retrieves all orders with pagination (Admin)")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getAllOrders(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        PageResponse<OrderResponse> orders = orderService.getAllOrders(page, size);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get orders by user", description = "Retrieves all orders for a specific user")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getOrdersByUserId(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        PageResponse<OrderResponse> orders = orderService.getOrdersByUserId(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get orders by status", description = "Retrieves orders filtered by status")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getOrdersByStatus(
            @Parameter(description = "Order status (pending, confirmed, processing, shipped, delivered, cancelled)")
            @PathVariable String status,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        PageResponse<OrderResponse> orders = orderService.getOrdersByStatus(status, page, size);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update order status", description = "Updates the status of an order (Admin)")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @Parameter(description = "Order ID") @PathVariable UUID id,
            @Parameter(description = "New status") @RequestParam String status) {
        OrderResponse order = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(order, "Order status updated successfully"));
    }

    @PatchMapping("/{id}/payment-status")
    @Operation(summary = "Update payment status", description = "Updates the payment status of an order")
    public ResponseEntity<ApiResponse<OrderResponse>> updatePaymentStatus(
            @Parameter(description = "Order ID") @PathVariable UUID id,
            @Parameter(description = "Payment status (pending, paid, failed, refunded)") @RequestParam String paymentStatus) {
        OrderResponse order = orderService.updatePaymentStatus(id, paymentStatus);
        return ResponseEntity.ok(ApiResponse.success(order, "Payment status updated successfully"));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel order", description = "Cancels an order")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Order cannot be cancelled"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @Parameter(description = "Order ID") @PathVariable UUID id) {
        OrderResponse order = orderService.cancelOrder(id);
        return ResponseEntity.ok(ApiResponse.success(order, "Order cancelled successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete order", description = "Deletes an order (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(
            @Parameter(description = "Order ID") @PathVariable UUID id) {
        orderService.deleteOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Order deleted successfully"));
    }

    @GetMapping("/count")
    @Operation(summary = "Get total order count", description = "Returns the total number of orders")
    public ResponseEntity<ApiResponse<Long>> getOrderCount() {
        long count = orderService.countOrders();
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/count/status/{status}")
    @Operation(summary = "Get order count by status", description = "Returns the number of orders with a specific status")
    public ResponseEntity<ApiResponse<Long>> getOrderCountByStatus(
            @Parameter(description = "Order status") @PathVariable String status) {
        long count = orderService.countOrdersByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update order", description = "Update editable top-level order fields")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrder(
            @Parameter(description = "Order ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderRequest request) {
        OrderResponse order = orderService.updateOrder(id, request);
        return ResponseEntity.ok(ApiResponse.success(order, "Order updated successfully"));
    }
}

