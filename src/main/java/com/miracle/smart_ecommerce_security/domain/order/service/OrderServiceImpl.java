package com.miracle.smart_ecommerce_security.domain.order.service;

import com.miracle.smart_ecommerce_security.domain.order.entity.CustomerOrder;
import com.miracle.smart_ecommerce_security.domain.order.entity.CustomerOrder.OrderStatus;
import com.miracle.smart_ecommerce_security.domain.order.entity.OrderItem;
import com.miracle.smart_ecommerce_security.domain.order.entity.ShippingMethod;
import com.miracle.smart_ecommerce_security.domain.order.repository.OrderItemRepository;
import com.miracle.smart_ecommerce_security.domain.order.repository.OrderRepository;
import com.miracle.smart_ecommerce_security.domain.order.repository.ShippingMethodRepository;
import com.miracle.smart_ecommerce_security.domain.order.repository.PaymentMethodRepository;
import com.miracle.smart_ecommerce_security.domain.product.entity.Product;
import com.miracle.smart_ecommerce_security.domain.product.repository.ProductRepository;
import com.miracle.smart_ecommerce_security.domain.order.dto.CreateOrderRequest;
import com.miracle.smart_ecommerce_security.domain.order.dto.OrderResponse;
import com.miracle.smart_ecommerce_security.domain.order.dto.TopCustomerReport;
import com.miracle.smart_ecommerce_security.domain.order.dto.UpdateOrderRequest;
import com.miracle.smart_ecommerce_security.domain.user.repository.UserRepository;
import com.miracle.smart_ecommerce_security.exception.BadRequestException;
import com.miracle.smart_ecommerce_security.exception.InsufficientStockException;
import com.miracle.smart_ecommerce_security.exception.ResourceNotFoundException;
import com.miracle.smart_ecommerce_security.service.StockManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.miracle.smart_ecommerce_security.config.CacheConfig.*;

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
    private final PaymentMethodRepository paymentMethodRepository;
    private final StockManagementService stockManagementService; // NEW FIELD


    @Override
    @Transactional(
        propagation = Propagation.REQUIRED,
        isolation = Isolation.READ_COMMITTED,
        rollbackFor = Exception.class
    )
    @CacheEvict(value = ORDERS_CACHE, allEntries = true)
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for user: {}", request.getUserId());

        if (!userRepository.existsById(request.getUserId())) {
            throw ResourceNotFoundException.forResource("User", request.getUserId());
        }

        String orderNumber = CustomerOrder.generateOrderNumber();

        Map<String, Integer> orderItems = request.getItems().stream()
                .collect(Collectors.toMap(
                    item -> item.getProductId().toString(),
                    CreateOrderRequest.OrderItemRequest::getQuantity
                ));

        // Validate stock availability using StockManagementService
        try {
            stockManagementService.validateOrderStock(orderItems, orderNumber);
        } catch (InsufficientStockException e) {
            log.warn("Order creation failed due to insufficient stock - orderNumber: {}, error: {}", orderNumber, e.getMessage());
            throw e;
        }

        // Resolve and validate all products upfront to fail fast before any DB writes
        List<OrderItem> orderItemsList = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> ResourceNotFoundException.forResource("Product", itemRequest.getProductId()));

            // Additional validation using Product.canBeOrdered method
            if (!product.canBeOrdered(itemRequest.getQuantity())) {
                throw InsufficientStockException.forOrderCreation(
                    itemRequest.getProductId().toString(),
                    product.getName(),
                    product.getStockQuantity(),
                    itemRequest.getQuantity(),
                    orderNumber
                );
            }

            OrderItem item = OrderItem.fromProduct(product, itemRequest.getQuantity());
            orderItemsList.add(item);
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
            // Reserve stock for the order
            StockManagementService.StockReservationResult reservationResult = 
                stockManagementService.reserveStock(orderItems, orderNumber);
            
            if (!reservationResult.isSuccess()) {
                log.error("Stock reservation failed during order creation - orderNumber: {}, error: {}", 
                    orderNumber, reservationResult.getException().getMessage());
                // Wrap any checked exception into an unchecked exception so the transaction can roll back
                throw new RuntimeException(reservationResult.getException());
            }

            // Save order
            CustomerOrder order = CustomerOrder.builder()
                    .user(userRepository.findById(request.getUserId()).orElseThrow(() -> ResourceNotFoundException.forResource("User", request.getUserId())))
                    .orderNumber(orderNumber)
                    .status(OrderStatus.PENDING.name().toLowerCase())
                    .paymentMethod(request.getPaymentMethodId() != null ?
                            paymentMethodRepository.findById(request.getPaymentMethodId()).orElse(null) : null)
                    .shippingMethod(request.getShippingMethodId() != null ?
                            shippingMethodRepository.findById(request.getShippingMethodId()).orElse(null) : null)
                    .subtotal(subtotal)
                    .total(total)
                    .build();

            CustomerOrder saved = orderRepository.save(order);

            // Save order items (stock already reserved)
            for (int i = 0; i < orderItemsList.size(); i++) {
                OrderItem item = orderItemsList.get(i);

                // set the owning order before saving
                item.setOrder(saved);
                orderItemRepository.save(item);
            }

            log.info("Order created with ID: {} and number: {}", saved.getId(), saved.getOrderNumber());

            // Use mapToResponseWithDetails — fetches items separately without touching the managed collection
            return mapToResponseWithDetails(saved);

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
    @Cacheable(value = ORDERS_CACHE, key = "#id")
    public OrderResponse getOrderById(UUID id) {
        log.debug("Getting order by ID: {}", id);
        CustomerOrder order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ORDERS_CACHE, key = "#orderNumber")
    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        log.debug("Getting order by order number: {}", orderNumber);
        CustomerOrder order = orderRepository.findByOrderNumberWithDetails(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        return mapToResponse(order);
    }


    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ORDERS_CACHE,
            key = "'all:page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize")
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        log.debug("Getting all orders - pageable: {}", pageable);

        Page<CustomerOrder> orderPage = orderRepository.findAll(pageable);
        return mapOrderPageWithBatchItems(orderPage);
    }

    /**
     * Get orders by user ID with pagination.
     * Cached by userId + page + size.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ORDERS_CACHE,
            key = "'user:' + #userId + ':page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize")
    public Page<OrderResponse> getOrdersByUserId(UUID userId, Pageable pageable) {
        log.debug("Getting orders for user: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw ResourceNotFoundException.forResource("User", userId);
        }

        Page<CustomerOrder> orderPage = orderRepository.findByUserId(userId, pageable);
        return mapOrderPageWithBatchItems(orderPage);
    }

    /**
     * Get orders by status with pagination.
     * Cached by status + page + size.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ORDERS_CACHE,
            key = "'status:' + #status + ':page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize")
    public Page<OrderResponse> getOrdersByStatus(String status, Pageable pageable) {
        log.debug("Getting orders by status: {}", status);

        Page<CustomerOrder> orderPage = orderRepository.findByStatus(status.toLowerCase(), pageable);
        return mapOrderPageWithBatchItems(orderPage);
    }

    /**
     * Update the status of an order.
     * Validates status transitions before applying.
     * All cache entries evicted after update.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
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

        CustomerOrder updated = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));
        return mapToResponse(updated);
    }

    /**
     * Update the payment status of an order.
     * Automatically confirms order if payment is marked as paid.
     * All cache entries evicted after update.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
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

        CustomerOrder updated = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));
        return mapToResponse(updated);
    }

    /**
     * Cancel an order and restore product stock.
     * Only cancellable orders (PENDING, CONFIRMED, PROCESSING) can be cancelled.
     * Stock restoration and status update are atomic within the same transaction.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = ORDERS_CACHE, allEntries = true)
    public OrderResponse cancelOrder(UUID id) {
        log.info("Cancelling order: {}", id);

        CustomerOrder order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));

        if (!order.canBeCancelled()) {
            throw new BadRequestException("Order cannot be cancelled. Current status: " + order.getStatus());
        }

        orderRepository.updateStatus(id, OrderStatus.CANCELLED.name().toLowerCase());

        // Restore product stock using StockManagementService
        List<OrderItem> items = orderItemRepository.findByOrderId(id);
        Map<String, Integer> orderItems = items.stream()
                .collect(Collectors.toMap(
                    item -> item.getProduct().getId().toString(),
                    OrderItem::getQuantity
                ));

        try {
            stockManagementService.releaseReservedStock(orderItems, order.getOrderNumber());
            log.info("Order cancelled and stock restored for order: {}", id);
        } catch (Exception e) {
            log.error("Failed to restore stock for cancelled order {}: {} - CID: {}", id, e.getMessage(), MDC.get("correlationId"));
            // Don't fail the cancellation if stock restoration fails, but log the error
        }

        CustomerOrder updated = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));
        return mapToResponse(updated);
    }

    /**
     * Update editable fields of an order (payment method, shipping method, items).
     * Recalculates subtotal and total after item changes.
     * Stock is adjusted for quantity changes and new/removed items.
     * All changes are atomic within the same transaction.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = ORDERS_CACHE, allEntries = true)
    public OrderResponse updateOrder(UUID id, UpdateOrderRequest request) {
        log.info("Updating order: {}", id);

        CustomerOrder order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));

        boolean changed = false;

        if (request.getPaymentMethodId() != null &&
                !request.getPaymentMethodId().equals(order.getPaymentMethod() != null ? order.getPaymentMethod().getId() : null)) {
            order.setPaymentMethod(request.getPaymentMethodId() != null ? paymentMethodRepository.findById(request.getPaymentMethodId()).orElse(null) : null);
            changed = true;
        }

        if (request.getShippingMethodId() != null &&
                !request.getShippingMethodId().equals(order.getShippingMethod() != null ? order.getShippingMethod().getId() : null)) {
            if (!shippingMethodRepository.existsById(request.getShippingMethodId())) {
                throw ResourceNotFoundException.forResource("ShippingMethod", request.getShippingMethodId());
            }
            order.setShippingMethod(shippingMethodRepository.findById(request.getShippingMethodId()).orElse(null));
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
                        stockDeltas.merge(existing.getProduct().getId(), existing.getQuantity(), Integer::sum);
                        changed = true;
                        continue;
                    }

                    if (!existing.getQuantity().equals(itemReq.getQuantity())) {
                        int qtyDiff = itemReq.getQuantity() - existing.getQuantity();
                        stockDeltas.merge(existing.getProduct().getId(), -qtyDiff, Integer::sum);
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
                    // ensure order relation set
                    item.setOrder(order);
                    orderItemRepository.save(item);
                }

                // Update the managed collection in-place — never replace it
                order.getOrderItems().clear();
                order.getOrderItems().addAll(resultingItems);

                // Recalculate subtotal
                BigDecimal newSubtotal = resultingItems.stream()
                        .map(OrderItem::getTotalPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                order.setSubtotal(newSubtotal);

                // Recalculate total with shipping
                BigDecimal shippingCost = BigDecimal.ZERO;
                UUID shippingId = order.getShippingMethod() != null ? order.getShippingMethod().getId() : null;
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

        // JPA dirty checking persists field changes; re-fetch for clean state with eager associations
        CustomerOrder updated = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));
        return mapToResponse(updated);
    }

    /**
     * Delete an order.
     * CascadeType.ALL on orderItems means items are deleted automatically.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = ORDERS_CACHE, allEntries = true)
    public void deleteOrder(UUID id) {
        log.info("Deleting order: {}", id);

        CustomerOrder order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));

        try {
            // Restore product stock before deleting the order
            List<OrderItem> items = orderItemRepository.findByOrderId(id);
            Map<String, Integer> orderItems = items.stream()
                    .collect(Collectors.toMap(
                            item -> item.getProduct().getId().toString(),
                            OrderItem::getQuantity
                    ));

            try {
                stockManagementService.releaseReservedStock(orderItems, order.getOrderNumber());
                log.info("Stock restored for deleted order: {}", id);
            } catch (Exception e) {
                log.error("Failed to restore stock for deleted order {}: {} - CID: {}", id, e.getMessage(), MDC.get("correlationId"));
                // Don't fail the deletion if stock restoration fails, but log the error
            }

            // Delete order items first to avoid foreign key constraint issues
            orderItemRepository.deleteByOrderId(id);
            
            // Then delete the order
            orderRepository.deleteById(id);
            
            log.info("Order deleted successfully with stock restoration: {}", id);
            
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

    /**
     * Native SQL report: top N customers ranked by total spending.
     * Delegates to the native {@code nativeQuery = true} query in {@link OrderRepository}.
     * Maps raw Object[] rows to {@link TopCustomerReport} DTOs.
     *
     * @param limit maximum number of customers to return (must be >= 1)
     * @return list of TopCustomerReport sorted by total_spent descending
     */
    @Override
    @Transactional(readOnly = true)
    public List<TopCustomerReport> getTopCustomersBySpending(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be at least 1");
        }
        log.debug("Fetching top {} customers by spending (native SQL)", limit);
        return orderRepository.findTopCustomersBySpending(limit)
                .stream()
                .map(TopCustomerReport::fromRow)
                .toList();
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private OrderResponse mapToResponse(CustomerOrder order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .paymentMethodId(order.getPaymentMethod() != null ? order.getPaymentMethod().getId() : null)
                .shippingMethodId(order.getShippingMethod() != null ? order.getShippingMethod().getId() : null)
                .subtotal(order.getSubtotal())
                .total(order.getTotal())
                .itemCount(order.getItemCount())
                .createdAt(order.getCreatedAt())
                .items(mapOrderItems(order.getOrderItems()))
                .build();
    }

    /**
     * Loads order items (with products) for the response using a single JOIN FETCH query.
     * Does NOT replace the managed collection — maps items directly into the response.
     */
    private OrderResponse mapToResponseWithDetails(CustomerOrder order) {
        List<OrderItem> items = orderItemRepository.findByOrderIdWithProduct(order.getId());
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .paymentMethodId(order.getPaymentMethod() != null ? order.getPaymentMethod().getId() : null)
                .shippingMethodId(order.getShippingMethod() != null ? order.getShippingMethod().getId() : null)
                .subtotal(order.getSubtotal())
                .total(order.getTotal())
                .itemCount(order.getItemCount())
                .createdAt(order.getCreatedAt())
                .items(mapOrderItems(items))
                .build();
    }

    /**
     * Maps a page of orders to a Page of responses by batch-loading all items (with products) for the
     * entire page in a single query, then grouping them back per order.
     * This eliminates the N+1 pattern of calling findByOrderId once per order.
     * Items are mapped directly into responses — the managed collection is never replaced.
     */
    private Page<OrderResponse> mapOrderPageWithBatchItems(Page<CustomerOrder> orderPage) {
        List<CustomerOrder> orders = orderPage.getContent();
        if (orders.isEmpty()) {
            return orderPage.map(o -> mapToResponse(o));
        }
        List<UUID> orderIds = orders.stream().map(CustomerOrder::getId).toList();
        List<OrderItem> allItems = orderItemRepository.findByOrderIdInWithProduct(orderIds);

        Map<UUID, List<OrderItem>> itemsByOrderId = allItems.stream()
                .collect(Collectors.groupingBy(oi -> oi.getOrder().getId()));

        List<OrderResponse> responses = orders.stream()
                .map(order -> {
                    List<OrderItem> items = itemsByOrderId.getOrDefault(order.getId(), new ArrayList<>());
                    return OrderResponse.builder()
                            .id(order.getId())
                            .userId(order.getUser().getId())
                            .orderNumber(order.getOrderNumber())
                            .status(order.getStatus())
                            .paymentStatus(order.getPaymentStatus())
                            .paymentMethodId(order.getPaymentMethod() != null ? order.getPaymentMethod().getId() : null)
                            .shippingMethodId(order.getShippingMethod() != null ? order.getShippingMethod().getId() : null)
                            .subtotal(order.getSubtotal())
                            .total(order.getTotal())
                            .itemCount(order.getItemCount())
                            .createdAt(order.getCreatedAt())
                            .items(mapOrderItems(items))
                            .build();
                })
                .toList();

        return new PageImpl<>(responses, orderPage.getPageable(), orderPage.getTotalElements());
    }

    /**
     * Maps order items to response DTOs.
     * Products are already eagerly loaded via JOIN FETCH in the repository queries.
     */
    private List<OrderResponse.OrderItemResponse> mapOrderItems(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }

        return items.stream()
                .map(item -> {
                    Product product = item.getProduct();
                    return OrderResponse.OrderItemResponse.builder()
                            .id(item.getId())
                            .productId(product.getId())
                            .productName(product.getName())
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
