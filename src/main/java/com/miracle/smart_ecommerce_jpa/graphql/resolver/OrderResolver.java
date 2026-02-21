package com.miracle.smart_ecommerce_jpa.graphql.resolver;

import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.CreateOrderRequest;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.OrderResponse;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.UpdateOrderRequest;
import com.miracle.smart_ecommerce_jpa.domain.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class OrderResolver {

    private final OrderService orderService;

    // =====================
    // QUERIES
    // =====================

    @QueryMapping
    public OrderResponse order(@Argument UUID id) {
        return orderService.getOrderById(id);
    }

    @QueryMapping
    public OrderResponse orderByNumber(@Argument String orderNumber) {
        return orderService.getOrderByOrderNumber(orderNumber);
    }

    @QueryMapping
    public PageResponse<OrderResponse> orders(@Argument int page, @Argument int size) {
        return orderService.getAllOrders(page, size);
    }

    @QueryMapping
    public PageResponse<OrderResponse> ordersByUser(@Argument UUID userId, @Argument int page, @Argument int size) {
        return orderService.getOrdersByUserId(userId, page, size);
    }

    @QueryMapping
    public PageResponse<OrderResponse> ordersByStatus(@Argument String status, @Argument int page, @Argument int size) {
        return orderService.getOrdersByStatus(status, page, size);
    }

    // =====================
    // MUTATIONS
    // =====================

    @MutationMapping
    public OrderResponse createOrder(@Argument CreateOrderRequest input) {
        return orderService.createOrder(input);
    }

    @MutationMapping
    public OrderResponse updateOrder(@Argument UUID id, @Argument UpdateOrderRequest input) {
        return orderService.updateOrder(id, input);
    }

    @MutationMapping
    public OrderResponse updateOrderStatus(@Argument UUID id, @Argument String status) {
        return orderService.updateOrderStatus(id, status);
    }

    @MutationMapping
    public OrderResponse updatePaymentStatus(@Argument UUID id, @Argument String paymentStatus) {
        return orderService.updatePaymentStatus(id, paymentStatus);
    }

    @MutationMapping
    public boolean deleteOrder(@Argument UUID id) {
        orderService.deleteOrder(id);
        return true;
    }

    @MutationMapping
    public OrderResponse cancelOrder(@Argument UUID id) {
        return orderService.cancelOrder(id);
    }
}

