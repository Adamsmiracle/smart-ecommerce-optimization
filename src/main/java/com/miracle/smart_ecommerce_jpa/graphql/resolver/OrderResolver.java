package com.miracle.smart_ecommerce_jpa.graphql.resolver;

import com.miracle.smart_ecommerce_jpa.annotation.RequireRoles;
import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.CreateOrderRequest;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.OrderResponse;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.UpdateOrderRequest;
import com.miracle.smart_ecommerce_jpa.domain.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequireRoles({"ADMIN", "CUSTOMER"})
public class OrderResolver {

    private final OrderService orderService;

    // =====================
    // QUERIES
    // =====================

    @QueryMapping
    @RequireRoles({"ADMIN", "CUSTOMER"})
    public OrderResponse order(@Argument UUID id) {
        return orderService.getOrderById(id);
    }

    @QueryMapping
    @RequireRoles({"ADMIN", "CUSTOMER"})
    public OrderResponse orderByNumber(@Argument String orderNumber) {
        return orderService.getOrderByOrderNumber(orderNumber);
    }

    @QueryMapping
    @RequireRoles({"ADMIN"})
    public PageResponse<OrderResponse> orders(@Argument int page, @Argument int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderService.getAllOrders(pageable);
    }

    @QueryMapping
    @RequireRoles({"ADMIN", "CUSTOMER"})
    public PageResponse<OrderResponse> ordersByUser(@Argument UUID userId, @Argument int page, @Argument int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderService.getOrdersByUserId(userId, pageable);
    }

    @QueryMapping
    @RequireRoles({"ADMIN", "CUSTOMER"})
    public PageResponse<OrderResponse> ordersByStatus(@Argument String status, @Argument int page, @Argument int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderService.getOrdersByStatus(status, pageable);
    }

    // =====================
    // MUTATIONS
    // =====================

    @MutationMapping
    @RequireRoles({"ADMIN", "CUSTOMER"})
    public OrderResponse createOrder(@Argument CreateOrderRequest input) {
        return orderService.createOrder(input);
    }

    @MutationMapping
    @RequireRoles({"ADMIN", "CUSTOMER"})
    public OrderResponse updateOrder(@Argument UUID id, @Argument UpdateOrderRequest input) {
        return orderService.updateOrder(id, input);
    }

    @MutationMapping
    @RequireRoles({"ADMIN", "CUSTOMER"})
    public OrderResponse updateOrderStatus(@Argument UUID id, @Argument String status) {
        return orderService.updateOrderStatus(id, status);
    }

    @MutationMapping
    @RequireRoles({"CUSTOMER"})
    public OrderResponse updatePaymentStatus(@Argument UUID id, @Argument String paymentStatus) {
        return orderService.updatePaymentStatus(id, paymentStatus);
    }

    @MutationMapping
    @RequireRoles({"ADMIN"})
    public boolean deleteOrder(@Argument UUID id) {
        orderService.deleteOrder(id);
        return true;
    }

    @MutationMapping
    @RequireRoles({"CUSTOMER"})
    public OrderResponse cancelOrder(@Argument UUID id) {
        return orderService.cancelOrder(id);
    }
}

