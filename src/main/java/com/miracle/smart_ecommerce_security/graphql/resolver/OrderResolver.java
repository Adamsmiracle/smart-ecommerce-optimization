package com.miracle.smart_ecommerce_security.graphql.resolver;

import com.miracle.smart_ecommerce_security.domain.order.dto.CreateOrderRequest;
import com.miracle.smart_ecommerce_security.domain.order.dto.OrderResponse;
import com.miracle.smart_ecommerce_security.domain.order.dto.UpdateOrderRequest;
import com.miracle.smart_ecommerce_security.domain.order.service.OrderService;
import com.miracle.smart_ecommerce_security.graphql.type.GraphQLPage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.UUID;

/**
 * GraphQL Resolver for Order entity.
 *
 * Access: ADMIN + CUSTOMER for most operations; ADMIN-only for listing all orders and deletion.
 */
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public GraphQLPage<OrderResponse> orders(@Argument int page, @Argument int size) {
        Pageable pageable = PageRequest.of(page, size);
        return GraphQLPage.of(orderService.getAllOrders(pageable));
    }

    @QueryMapping
    @PreAuthorize("principal.username == #userId || hasAnyRole('ADMIN', 'STAFF')")
    public GraphQLPage<OrderResponse> ordersByUser(@Argument UUID userId, @Argument int page, @Argument int size) {
        Pageable pageable = PageRequest.of(page, size);
        return GraphQLPage.of(orderService.getOrdersByUserId(userId, pageable));
    }

    @QueryMapping
    public GraphQLPage<OrderResponse> ordersByStatus(@Argument String status, @Argument int page, @Argument int size) {
        Pageable pageable = PageRequest.of(page, size);
        return GraphQLPage.of(orderService.getOrdersByStatus(status, pageable));
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
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public OrderResponse updateOrderStatus(@Argument UUID id, @Argument String status) {
        return orderService.updateOrderStatus(id, status);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public OrderResponse updatePaymentStatus(@Argument UUID id, @Argument String paymentStatus) {
        return orderService.updatePaymentStatus(id, paymentStatus);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public boolean deleteOrder(@Argument UUID id) {
        orderService.deleteOrder(id);
        return true;
    }

    @MutationMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderResponse cancelOrder(@Argument UUID id) {
        return orderService.cancelOrder(id);
    }
}
