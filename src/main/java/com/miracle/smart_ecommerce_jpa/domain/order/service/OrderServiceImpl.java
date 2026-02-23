package com.miracle.smart_ecommerce_jpa.domain.order.service;

import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.order.entity.CustomerOrder;
import com.miracle.smart_ecommerce_jpa.domain.order.entity.CustomerOrder.OrderStatus;
import com.miracle.smart_ecommerce_jpa.domain.order.entity.CustomerOrder.PaymentStatus;
import com.miracle.smart_ecommerce_jpa.domain.order.entity.OrderItem;
import com.miracle.smart_ecommerce_jpa.domain.order.entity.ShippingMethod;
import com.miracle.smart_ecommerce_jpa.domain.order.repository.OrderItemRepository;
import com.miracle.smart_ecommerce_jpa.domain.order.repository.OrderRepository;
import com.miracle.smart_ecommerce_jpa.domain.order.repository.ShippingMethodRepository;
import com.miracle.smart_ecommerce_jpa.domain.product.entity.Product;
import com.miracle.smart_ecommerce_jpa.domain.product.repository.ProductRepository;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.CreateOrderRequest;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.OrderResponse;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.UpdateOrderRequest;
import com.miracle.smart_ecommerce_jpa.domain.user.repository.UserRepository;
import com.miracle.smart_ecommerce_jpa.exception.BadRequestException;
import com.miracle.smart_ecommerce_jpa.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.miracle.smart_ecommerce_jpa.config.CacheConfig.*;

/**
 * Implementation of OrderService using Spring Data JPA.
 *
 * Transaction strategy:
 * - createOrder uses REQUIRED propagation to ensure all inserts and stock updates
 *   are atomic — a failure in any step rolls back the entire order
 * - cancelOrder restores stock within the same transaction
 * - updateOrder recalculates totals and persists item changes atomically
 * - Read operations use readOnly = true for performance
 *
 * Cache strategy:
 * - Individual orders cached by ID and order number
 * - All order cache entries evicted on any write to prevent stale listings
 *
 * Exception strategy:
 * - ResourceNotFoundException for missing entities
 * - BadRequestException for invalid state transitions or cancellation rules
 * - IllegalArgumentException for invalid input (e.g. insufficient stock)
 * - DataIntegrityViolationException caught as safety net for DB constraint violations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ShippingMethodRepository shippingMethodRepository;

    /**
     * Create a new order.
     * Validates user, products, and stock before saving.
     * Deducts stock for each product atomically within the same transaction.
     * Rolls back entirely if any product is out of stock or not found.
     */
    @Override
    @Transactional
    @CacheEvict(value = ORDERS_CACHE, allEntries = true)
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for user: {}", request.getUserId());

        if (!userRepository.existsById(request.getUserId())) {
            throw ResourceNotFoundException.forResource("User", request.getUserId());
        }

        // Resolve and validate all products upfront to fail fast before any DB writes
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> ResourceNotFoundException.forResource("Product", itemRequest.getProductId()));

            if (!product.canBeOrdered(itemRequest.getQuantity())) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product: " + product.getName() +
                                ". Available: " + product.getStockQuantity() +
                                ", Requested: " + itemRequest.getQuantity());
            }

            OrderItem item = OrderItem.fromProduct(product, itemRequest.getQuantity());
            orderItems.add(item);
            subtotal = subtotal.add(item.getTotalPrice());
        }

        // Resolve shipping cost
        BigDecimal shippingCost = BigDecimal.ZERO;
        if (request.getShippingMethodId() != null) {
            ShippingMethod shippingMethod = shippingMethodRepository.findById(request.getShippingMethodId())
                    .orElseThrow(() -> ResourceNotFoundException.forResource("ShippingMethod", request.getShippingMethodId()));
            shippingCost = shippingMethod.getPrice() != null ? shippingMethod.getPrice() : BigDecimal.ZERO;
        }

        BigDecimal total = subtotal.add(shippingCost);

        try {
            // Save order
            CustomerOrder order = CustomerOrder.builder()
                    .userId(request.getUserId())
                    .orderNumber(CustomerOrder.generateOrderNumber())
                    .status(OrderStatus.PENDING.name().toLowerCase())
                    .paymentStatus(PaymentStatus.PENDING.name().toLowerCase())
                    .paymentMethodId(request.getPaymentMethodId())
                    .shippingMethodId(request.getShippingMethodId())
                    .subtotal(subtotal)
                    .total(total)
                    .build();

            CustomerOrder saved = orderRepository.save(order);

            // Save order items and deduct stock
            for (int i = 0; i < orderItems.size(); i++) {
                OrderItem item = orderItems.get(i);
                CreateOrderRequest.OrderItemRequest itemRequest = request.getItems().get(i);

                item.setOrderId(saved.getId());
                orderItemRepository.save(item);

                // Deduct stock: fetch current stock and subtract quantity
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> ResourceNotFoundException.forResource("Product", item.getProductId()));
                int newStock = product.getStockQuantity() - itemRequest.getQuantity();
                productRepository.updateStock(item.getProductId(), newStock);
            }

            log.info("Order created with ID: {} and number: {}", saved.getId(), saved.getOrderNumber());

            // Load items for response
            saved.setOrderItems(orderItemRepository.findByOrderId(saved.getId()));
            return mapToResponse(saved);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while creating order for user: {}", request.getUserId(), e);
            throw new DataIntegrityViolationException("Failed to create order due to a data constraint violation: " + e.getMessage());
        }
    }

    /**
     * Get order by ID.
     * Result cached by ID.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ORDERS_CACHE, key = "'id:' + #id")
    public OrderResponse getOrderById(UUID id) {
        log.debug("Getting order by ID: {}", id);
        CustomerOrder order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));
        return mapToResponseWithDetails(order);
    }

    /**
     * Get order by order number.
     * Result cached by order number.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ORDERS_CACHE, key = "'number:' + #orderNumber")
    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        log.debug("Getting order by order number: {}", orderNumber);
        CustomerOrder order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        return mapToResponseWithDetails(order);
    }

    /**
     * Get all orders with pagination.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(Pageable pageable) {
        log.debug("Getting all orders - pageable: {}", pageable);

        Page<CustomerOrder> orderPage = orderRepository.findAll(pageable);
        List<OrderResponse> responses = orderPage.getContent().stream()
                .map(this::mapToResponseWithDetails)
                .toList();

        return PageResponse.of(responses, pageable.getPageNumber(), pageable.getPageSize(), orderPage.getTotalElements());
    }

    /**
     * Get orders by user ID with pagination.
     * Validates user existence before querying.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrdersByUserId(UUID userId, Pageable pageable) {
        log.debug("Getting orders for user: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw ResourceNotFoundException.forResource("User", userId);
        }

        Page<CustomerOrder> orderPage = orderRepository.findByUserId(userId, pageable);
        List<OrderResponse> responses = orderPage.getContent().stream()
                .map(this::mapToResponseWithDetails)
                .toList();

        return PageResponse.of(responses, pageable.getPageNumber(), pageable.getPageSize(), orderPage.getTotalElements());
    }

    /**
     * Get orders by status with pagination.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrdersByStatus(String status, Pageable pageable) {
        log.debug("Getting orders by status: {}", status);

        Page<CustomerOrder> orderPage = orderRepository.findByStatus(status.toLowerCase(), pageable);
        List<OrderResponse> responses = orderPage.getContent().stream()
                .map(this::mapToResponseWithDetails)
                .toList();

        return PageResponse.of(responses, pageable.getPageNumber(), pageable.getPageSize(), orderPage.getTotalElements());
    }

    /**
     * Update the status of an order.
     * Validates status transitions before applying.
     * All cache entries evicted after update.
     */
    @Override
    @Transactional
    @CacheEvict(value = ORDERS_CACHE, allEntries = true)
    public OrderResponse updateOrderStatus(UUID id, String status) {
        log.info("Updating order status: {} to {}", id, status);

        CustomerOrder order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));

        if ("cancelled".equalsIgnoreCase(status)) {
            if (!order.canBeCancelled()) {
                throw new BadRequestException("Order cannot be cancelled. Current status: " + order.getStatus());
            }
        } else {
            validateStatusTransition(order.getStatus(), status);
        }

        orderRepository.updateStatus(id, status.toLowerCase());
        log.info("Order status updated successfully: {} -> {}", id, status);

        CustomerOrder updated = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));
        return mapToResponseWithDetails(updated);
    }

    /**
     * Update the payment status of an order.
     * Automatically confirms order if payment is marked as paid.
     * All cache entries evicted after update.
     */
    @Override
    @Transactional
    @CacheEvict(value = ORDERS_CACHE, allEntries = true)
    public OrderResponse updatePaymentStatus(UUID id, String paymentStatus) {
        log.info("Updating payment status for order: {} to {}", id, paymentStatus);

        CustomerOrder order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));

        orderRepository.updatePaymentStatus(id, paymentStatus.toLowerCase());

        // Auto-confirm order when payment is received
        if ("paid".equalsIgnoreCase(paymentStatus) &&
                OrderStatus.PENDING.name().equalsIgnoreCase(order.getStatus())) {
            orderRepository.updateStatus(id, OrderStatus.CONFIRMED.name().toLowerCase());
        }

        log.info("Payment status updated for order: {}", id);

        CustomerOrder updated = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));
        return mapToResponseWithDetails(updated);
    }

    /**
     * Cancel an order and restore product stock.
     * Only cancellable orders (PENDING, CONFIRMED, PROCESSING) can be cancelled.
     * Stock restoration and status update are atomic within the same transaction.
     */
    @Override
    @Transactional
    @CacheEvict(value = ORDERS_CACHE, allEntries = true)
    public OrderResponse cancelOrder(UUID id) {
        log.info("Cancelling order: {}", id);

        CustomerOrder order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));

        if (!order.canBeCancelled()) {
            throw new BadRequestException("Order cannot be cancelled. Current status: " + order.getStatus());
        }

        orderRepository.updateStatus(id, OrderStatus.CANCELLED.name().toLowerCase());

        // Restore product stock atomically within the same transaction
        List<OrderItem> items = orderItemRepository.findByOrderId(id);
        for (OrderItem item : items) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> ResourceNotFoundException.forResource("Product", item.getProductId()));
            int restoredStock = product.getStockQuantity() + item.getQuantity();
            productRepository.updateStock(item.getProductId(), restoredStock);
        }

        log.info("Order cancelled and stock restored for order: {}", id);

        CustomerOrder updated = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));
        return mapToResponseWithDetails(updated);
    }

    /**
     * Update editable fields of an order (payment method, shipping method, items).
     * Recalculates subtotal and total after item changes.
     * Stock is adjusted for quantity changes and new/removed items.
     * All changes are atomic within the same transaction.
     */
    @Override
    @Transactional
    @CacheEvict(value = ORDERS_CACHE, allEntries = true)
    public OrderResponse updateOrder(UUID id, UpdateOrderRequest request) {
        log.info("Updating order: {}", id);

        CustomerOrder order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));

        boolean changed = false;

        if (request.getPaymentMethodId() != null &&
                !request.getPaymentMethodId().equals(order.getPaymentMethodId())) {
            order.setPaymentMethodId(request.getPaymentMethodId());
            changed = true;
        }

        if (request.getShippingMethodId() != null &&
                !request.getShippingMethodId().equals(order.getShippingMethodId())) {
            if (!shippingMethodRepository.existsById(request.getShippingMethodId())) {
                throw ResourceNotFoundException.forResource("ShippingMethod", request.getShippingMethodId());
            }
            order.setShippingMethodId(request.getShippingMethodId());
            changed = true;
        }

        if (request.getItems() != null) {
            List<OrderItem> existingItems = orderItemRepository.findByOrderId(id);
            Map<UUID, OrderItem> existingById = existingItems.stream()
                    .filter(it -> it.getId() != null)
                    .collect(Collectors.toMap(OrderItem::getId, it -> it));

            List<OrderItem> resultingItems = new ArrayList<>();
            Map<UUID, Integer> stockDeltas = new java.util.HashMap<>();

            for (UpdateOrderRequest.OrderItemUpdateRequest itemReq : request.getItems()) {
                if (itemReq.getId() != null && existingById.containsKey(itemReq.getId())) {
                    OrderItem existing = existingById.get(itemReq.getId());

                    if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                        // Remove item — restore its stock
                        stockDeltas.merge(existing.getProductId(), existing.getQuantity(), Integer::sum);
                        changed = true;
                        continue;
                    }

                    if (!existing.getQuantity().equals(itemReq.getQuantity())) {
                        int qtyDiff = itemReq.getQuantity() - existing.getQuantity();
                        stockDeltas.merge(existing.getProductId(), -qtyDiff, Integer::sum);
                        existing.setQuantity(itemReq.getQuantity());
                        changed = true;
                    }
                    resultingItems.add(existing);

                } else if (itemReq.getProductId() != null &&
                        itemReq.getQuantity() != null &&
                        itemReq.getQuantity() > 0) {
                    // New item
                    Product product = productRepository.findById(itemReq.getProductId())
                            .orElseThrow(() -> ResourceNotFoundException.forResource("Product", itemReq.getProductId()));

                    if (product.getStockQuantity() < itemReq.getQuantity()) {
                        throw new IllegalArgumentException(
                                "Insufficient stock for product: " + product.getName() +
                                        ". Available: " + product.getStockQuantity());
                    }

                    OrderItem newItem = OrderItem.fromProduct(product, itemReq.getQuantity());
                    newItem.setOrderId(id);
                    resultingItems.add(newItem);
                    stockDeltas.merge(product.getId(), -itemReq.getQuantity(), Integer::sum);
                    changed = true;
                }
            }

            if (changed) {
                // Apply stock deltas using absolute values
                for (Map.Entry<UUID, Integer> entry : stockDeltas.entrySet()) {
                    if (entry.getValue() == 0) continue;
                    Product product = productRepository.findById(entry.getKey())
                            .orElseThrow(() -> ResourceNotFoundException.forResource("Product", entry.getKey()));
                    int newStock = product.getStockQuantity() + entry.getValue();
                    if (newStock < 0) {
                        throw new IllegalArgumentException("Stock would go negative for product: " + product.getName());
                    }
                    productRepository.updateStock(entry.getKey(), newStock);
                }

                // Persist items: delete all and re-insert
                orderItemRepository.deleteByOrderId(id);
                for (OrderItem item : resultingItems) {
                    item.setOrderId(id);
                    orderItemRepository.save(item);
                }

                order.setOrderItems(resultingItems);

                // Recalculate subtotal
                BigDecimal newSubtotal = resultingItems.stream()
                        .map(OrderItem::getTotalPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                order.setSubtotal(newSubtotal);

                // Recalculate total with shipping
                BigDecimal shippingCost = BigDecimal.ZERO;
                UUID shippingId = order.getShippingMethodId();
                if (shippingId != null) {
                    shippingMethodRepository.findById(shippingId).ifPresent(sm -> {
                        // handled below
                    });
                    ShippingMethod sm = shippingMethodRepository.findById(shippingId).orElse(null);
                    if (sm != null && sm.getPrice() != null) {
                        shippingCost = sm.getPrice();
                    }
                }
                order.setTotal(newSubtotal.add(shippingCost));
            }
        }

        if (!changed) {
            log.debug("No changes detected for order: {}", id);
            return mapToResponseWithDetails(order);
        }

        log.info("Order updated successfully: {}", id);

        // JPA dirty checking persists field changes; re-fetch for clean state
        CustomerOrder updated = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));
        return mapToResponseWithDetails(updated);
    }

    /**
     * Delete an order.
     * CascadeType.ALL on orderItems means items are deleted automatically.
     */
    @Override
    @Transactional
    @CacheEvict(value = ORDERS_CACHE, allEntries = true)
    public void deleteOrder(UUID id) {
        log.info("Deleting order: {}", id);

        if (!orderRepository.existsById(id)) {
            throw ResourceNotFoundException.forResource("Order", id);
        }

        try {
            orderRepository.deleteById(id);
            log.info("Order deleted successfully: {}", id);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while deleting order: {}", id, e);
            throw new DataIntegrityViolationException("Cannot delete order due to a data constraint violation: " + e.getMessage());
        }
    }

    /**
     * Count total orders.
     */
    @Override
    @Transactional(readOnly = true)
    public long countOrders() {
        return orderRepository.count();
    }

    /**
     * Count orders by status.
     */
    @Override
    @Transactional(readOnly = true)
    public long countOrdersByStatus(String status) {
        return orderRepository.countByStatus(status.toLowerCase());
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private OrderResponse mapToResponse(CustomerOrder order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .paymentMethodId(order.getPaymentMethodId())
                .shippingMethodId(order.getShippingMethodId())
                .subtotal(order.getSubtotal())
                .total(order.getTotal())
                .itemCount(order.getItemCount())
                .createdAt(order.getCreatedAt())
                .items(mapOrderItems(order.getOrderItems()))
                .build();
    }

    /**
     * Loads order items for the response.
     * Products are batch-fetched by collecting IDs first to avoid N+1 queries.
     */
    private OrderResponse mapToResponseWithDetails(CustomerOrder order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        order.setOrderItems(items);
        return mapToResponse(order);
    }

    /**
     * Maps order items to response DTOs.
     * Batch-fetches all products by ID to avoid N+1 queries.
     */
    private List<OrderResponse.OrderItemResponse> mapOrderItems(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }

        // Batch fetch all products to avoid N+1
        List<UUID> productIds = items.stream().map(OrderItem::getProductId).toList();
        Map<UUID, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        return items.stream()
                .map(item -> {
                    Product product = productMap.get(item.getProductId());
                    return OrderResponse.OrderItemResponse.builder()
                            .id(item.getId())
                            .productId(item.getProductId())
                            .productName(product != null ? product.getName() : null)
                            .unitPrice(item.getUnitPrice())
                            .quantity(item.getQuantity())
                            .totalPrice(item.getTotalPrice())
                            .build();
                })
                .toList();
    }

    private void validateStatusTransition(String currentStatus, String newStatus) {
        if (currentStatus == null || newStatus == null) {
            throw new BadRequestException("Status values must not be null");
        }

        String current = currentStatus.toLowerCase();
        String next = newStatus.toLowerCase();

        if (current.equals(next)) return;

        Set<String> allowed = Set.of("pending", "confirmed", "processing", "shipped",
                "out_for_delivery", "delivered", "cancelled", "refunded", "failed");
        if (!allowed.contains(next)) {
            throw new BadRequestException("Unknown target status: " + newStatus);
        }

        Set<String> terminal = Set.of("delivered", "cancelled", "refunded", "failed");
        if (terminal.contains(current)) {
            throw new BadRequestException(
                    String.format("Cannot transition from terminal status '%s' to '%s'", currentStatus, newStatus));
        }

        if ("delivered".equals(next) && !"shipped".equals(current) && !"out_for_delivery".equals(current)) {
            throw new BadRequestException(
                    String.format("Cannot transition from '%s' to 'delivered'", currentStatus));
        }

        if ("cancelled".equals(next)) {
            throw new BadRequestException("Use cancelOrder() to cancel an order");
        }
    }
}