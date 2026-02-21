package com.miracle.smart_ecommerce_jpa.domain.order.service;

import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.order.entity.CustomerOrder;
import com.miracle.smart_ecommerce_jpa.domain.order.entity.CustomerOrder.OrderStatus;
import com.miracle.smart_ecommerce_jpa.domain.order.entity.CustomerOrder.PaymentStatus;
import com.miracle.smart_ecommerce_jpa.domain.order.entity.OrderItem;
import com.miracle.smart_ecommerce_jpa.domain.order.repository.OrderItemRepository;
import com.miracle.smart_ecommerce_jpa.domain.order.repository.OrderRepository;
import com.miracle.smart_ecommerce_jpa.domain.order.repository.ShippingMethodRepository;
import com.miracle.smart_ecommerce_jpa.domain.product.entity.Product;
import com.miracle.smart_ecommerce_jpa.domain.product.repository.ProductRepository;
import com.miracle.smart_ecommerce_jpa.domain.user.entity.User;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.CreateOrderRequest;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.OrderResponse;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.UpdateOrderRequest;
import com.miracle.smart_ecommerce_jpa.domain.user.repository.UserRepository;
import com.miracle.smart_ecommerce_jpa.exception.ResourceNotFoundException;
import com.miracle.smart_ecommerce_jpa.exception.BadRequestException;
import com.miracle.smart_ecommerce_jpa.domain.order.entity.ShippingMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.miracle.smart_ecommerce_jpa.config.CacheConfig.*;

/**
 * Implementation of OrderService.
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
    private final CacheManager cacheManager;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for user: {}", request.getUserId());

        // Verify user exists
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> ResourceNotFoundException.forResource("User", request.getUserId()));

        // Generate order number
        String orderNumber = CustomerOrder.generateOrderNumber();

        // Calculate order totals from items
        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> ResourceNotFoundException.forResource("Product", itemRequest.getProductId()));

            // Validate stock
            if (product.getStockQuantity() < itemRequest.getQuantity()) {
                throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
            }

            BigDecimal unitPrice = product.getPrice();
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            subtotal = subtotal.add(totalPrice);

            OrderItem orderItem = OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .unitPrice(unitPrice)
                    .quantity(itemRequest.getQuantity())
                    .build();

            orderItems.add(orderItem);
        }

        // Fetch shipping method if provided
        ShippingMethod shippingMethod = null;
        BigDecimal shippingCost = BigDecimal.ZERO;
        if (request.getShippingMethodId() != null) {
            shippingMethod = shippingMethodRepository.findById(request.getShippingMethodId())
                    .orElseThrow(() -> ResourceNotFoundException.forResource("ShippingMethod", request.getShippingMethodId()));
            shippingCost = shippingMethod.getPrice() != null ? shippingMethod.getPrice() : BigDecimal.ZERO;
        }

        // Total equals subtotal + shipping cost
        BigDecimal total = subtotal.add(shippingCost);

        // Create order
        CustomerOrder order = CustomerOrder.builder()
                .userId(request.getUserId())
                .orderNumber(orderNumber)
                .status(OrderStatus.PENDING.name().toLowerCase())
                .paymentStatus(PaymentStatus.PENDING.name().toLowerCase())
                .paymentMethodId(request.getPaymentMethodId())
                .shippingMethodId(request.getShippingMethodId())
                .subtotal(subtotal)
                .total(total)
                .build();

        CustomerOrder savedOrder = orderRepository.save(order);
        log.info("Order created with ID: {} and order number: {}", savedOrder.getId(), orderNumber);

        // Save order items
        for (OrderItem item : orderItems) {
            item.setOrderId(savedOrder.getId());
            orderItemRepository.save(item);
        }

        // Update product stock
        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            productRepository.updateStock(itemRequest.getProductId(), -itemRequest.getQuantity());
        }

        // Load items for response
        savedOrder.setOrderItems(orderItemRepository.findByOrderId(savedOrder.getId()));
        savedOrder.setUser(user);
        savedOrder.setShippingMethod(shippingMethod);

        OrderResponse response = mapToResponse(savedOrder);

        // Update caches with new order
        Cache byIdCache = cacheManager.getCache(ORDERS_CACHE);
        if (byIdCache != null) {
            byIdCache.put("id:" + savedOrder.getId(), response);
        }

        Cache byNumberCache = cacheManager.getCache(ORDERS_CACHE);
        if (byNumberCache != null) {
            byNumberCache.put("number:" + savedOrder.getOrderNumber(), response);
        }

        // Clear list cache
        evictCache(ORDERS_CACHE);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ORDERS_CACHE, key = "'id:' + #id")
    public OrderResponse getOrderById(UUID id) {
        log.debug("Getting order by ID: {}", id);
        CustomerOrder order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));
        return mapToResponseWithDetails(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        log.debug("Getting order by order number: {}", orderNumber);
        CustomerOrder order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        return mapToResponseWithDetails(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(int page, int size) {
        log.debug("Getting all orders - page: {}, size: {}", page, size);
        List<CustomerOrder> orders = orderRepository.findAll(page, size);
        long total = orderRepository.count();

        List<OrderResponse> responses = orders.stream()
                .map(this::mapToResponseWithDetails)
                .collect(Collectors.toList());

        return PageResponse.of(responses, page, size, total);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrdersByUserId(UUID userId, int page, int size) {
        log.debug("Getting orders for user: {} - page: {}, size: {}", userId, page, size);
        List<CustomerOrder> orders = orderRepository.findByUserId(userId, page, size);
        long total = orderRepository.countByUserId(userId);

        List<OrderResponse> responses = orders.stream()
                .map(this::mapToResponseWithDetails)
                .collect(Collectors.toList());

        return PageResponse.of(responses, page, size, total);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrdersByStatus(String status, int page, int size) {
        log.debug("Getting orders by status: {} - page: {}, size: {}", status, page, size);
        List<CustomerOrder> orders = orderRepository.findByStatus(status.toLowerCase(), page, size);
        long total = orderRepository.countByStatus(status.toLowerCase());

        List<OrderResponse> responses = orders.stream()
                .map(this::mapToResponseWithDetails)
                .collect(Collectors.toList());

        return PageResponse.of(responses, page, size, total);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(UUID id, String status) {
        log.info("Updating order status: {} to {}", id, status);

        CustomerOrder order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));

        String orderNumber = order.getOrderNumber();

        // Special-case cancellation: only allow when order.canBeCancelled()
        if ("cancelled".equalsIgnoreCase(status)) {
            if (!order.canBeCancelled()) {
                throw new BadRequestException("Order cannot be cancelled. Current status: " + order.getStatus());
            }
        } else {
            // Validate status transition for other statuses
            validateStatusTransition(order.getStatus(), status);
        }

        orderRepository.updateStatus(id, status.toLowerCase());

        // Refresh order
        order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));

        log.info("Order status updated successfully: {}", id);
        OrderResponse response = mapToResponseWithDetails(order);

        // Update caches
        Cache byIdCache = cacheManager.getCache(ORDERS_CACHE);
        if (byIdCache != null) {
            byIdCache.put("id:" + id, response);
        }

        Cache byNumberCache = cacheManager.getCache(ORDERS_CACHE);
        if (byNumberCache != null && orderNumber != null) {
            byNumberCache.put("number:" + orderNumber, response);
        }

        // Clear list cache
        evictCache(ORDERS_CACHE);

        return response;
    }

    @Override
    @Transactional
    public OrderResponse updatePaymentStatus(UUID id, String paymentStatus) {
        log.info("Updating payment status for order: {} to {}", id, paymentStatus);

        CustomerOrder order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));

        String orderNumber = order.getOrderNumber();

        orderRepository.updatePaymentStatus(id, paymentStatus.toLowerCase());

        // If payment is successful, update order status to confirmed
        if ("paid".equalsIgnoreCase(paymentStatus) &&
            OrderStatus.PENDING.name().equalsIgnoreCase(order.getStatus())) {
            orderRepository.updateStatus(id, OrderStatus.CONFIRMED.name().toLowerCase());
        }

        // Refresh order
        order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));

        log.info("Payment status updated successfully for order: {}", id);
        OrderResponse response = mapToResponseWithDetails(order);

        // Update caches
        Cache byIdCache = cacheManager.getCache(ORDERS_CACHE);
        if (byIdCache != null) {
            byIdCache.put("id:" + id, response);
        }

        Cache byNumberCache = cacheManager.getCache(ORDERS_CACHE);
        if (byNumberCache != null && orderNumber != null) {
            byNumberCache.put("number:" + orderNumber, response);
        }

        // Clear list cache
        evictCache(ORDERS_CACHE);

        return response;
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID id) {
        log.info("Cancelling order: {}", id);

        CustomerOrder order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));

        String orderNumber = order.getOrderNumber();

        // Check if order can be cancelled
        if (!order.canBeCancelled()) {
            throw new BadRequestException("Order cannot be cancelled. Current status: " + order.getStatus());
        }

        // Update status to cancelled
        orderRepository.updateStatus(id, OrderStatus.CANCELLED.name().toLowerCase());

        // Restore product stock
        List<OrderItem> items = orderItemRepository.findByOrderId(id);
        for (OrderItem item : items) {
            productRepository.updateStock(item.getProductId(), item.getQuantity());
            // Evict product caches for updated stock
            Cache productCache = cacheManager.getCache(PRODUCTS_CACHE);
            if (productCache != null) {
                productCache.evict("id:" + item.getProductId());
            }
        }

        // Refresh order
        order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));

        log.info("Order cancelled successfully: {}", id);
        OrderResponse response = mapToResponseWithDetails(order);

        // Update order caches
        Cache byIdCache = cacheManager.getCache(ORDERS_CACHE);
        if (byIdCache != null) {
            byIdCache.put("id:" + id, response);
        }

        Cache byNumberCache = cacheManager.getCache(ORDERS_CACHE);
        if (byNumberCache != null && orderNumber != null) {
            byNumberCache.put("number:" + orderNumber, response);
        }

        return response;
    }

    @Override
    @Transactional
    public void deleteOrder(UUID id) {
        log.info("Deleting order: {}", id);

        CustomerOrder order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));

        String orderNumber = order.getOrderNumber();

        // Delete order items first
        orderItemRepository.deleteByOrderId(id);

        // Delete order
        orderRepository.deleteById(id);
        log.info("Order deleted successfully: {}", id);

        // Evict from id and number caches
        Cache byIdCache = cacheManager.getCache(ORDERS_CACHE);
        if (byIdCache != null) {
            byIdCache.evict("id:" + id);
        }

        Cache byNumberCache = cacheManager.getCache(ORDERS_CACHE);
        if (byNumberCache != null && orderNumber != null) {
            byNumberCache.evict("number:" + orderNumber);
        }

        // Clear list cache
        evictCache(ORDERS_CACHE);
    }

    @Override
    @Transactional(readOnly = true)
    public long countOrders() {
        return orderRepository.count();
    }

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
                .customerName(order.getUser() != null ? order.getUser().getFullName() : null)
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
                .shippingMethod(mapShippingMethod(order.getShippingMethod()))
                .build();
    }

    private OrderResponse mapToResponseWithDetails(CustomerOrder order) {
        // Load order items
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        order.setOrderItems(items);

        // Load user
        userRepository.findById(order.getUserId()).ifPresent(order::setUser);

        // Load shipping method
        if (order.getShippingMethodId() != null) {
            // shipping method loader not implemented fully; keep transient if available
            // shipping method may be loaded elsewhere
        }

        return mapToResponse(order);
    }

    private List<OrderResponse.OrderItemResponse> mapOrderItems(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }
        return items.stream()
                .map(item -> {
                    // Fetch product details for name/sku
                    Product product = productRepository.findById(item.getProductId()).orElse(null);
                    return OrderResponse.OrderItemResponse.builder()
                            .id(item.getId())
                            .productId(item.getProductId())
                            .productName(product != null ? product.getName() : null)
                            .unitPrice(item.getUnitPrice())
                            .quantity(item.getQuantity())
                            .totalPrice(item.getTotalPrice())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private OrderResponse.ShippingMethodResponse mapShippingMethod(com.miracle.smart_ecommerce_jpa.domain.order.entity.ShippingMethod shippingMethod) {
        if (shippingMethod == null) return null;
        return OrderResponse.ShippingMethodResponse.builder()
                .id(shippingMethod.getId())
                .name(shippingMethod.getName())
                .price(shippingMethod.getPrice())
                .estimatedDelivery(shippingMethod.getEstimatedDays() != null ? shippingMethod.getEstimatedDays() + " days" : null)
                .build();
    }

    private void validateStatusTransition(String currentStatus, String newStatus) {
        // Basic validation
        if (currentStatus == null || newStatus == null) {
            throw new BadRequestException("Status values must not be null");
        }

        String current = currentStatus.toLowerCase();
        String next = newStatus.toLowerCase();

        if (current.equals(next)) {
            return; // No change
        }

        // Allowed statuses
        Set<String> allowed = Set.of("pending", "confirmed", "processing", "shipped", "delivered", "cancelled");
        if (!allowed.contains(next)) {
            throw new BadRequestException("Unknown target status: " + newStatus + ". Allowed: " + String.join(", ", allowed));
        }

        // Terminal states cannot transition to anything else
        Set<String> terminal = Set.of("delivered", "cancelled");
        if (terminal.contains(current)) {
            throw new BadRequestException(String.format("Invalid status transition from '%s' to '%s'", currentStatus, newStatus));
        }

        // If target is 'delivered', require current be 'shipped'
        if ("delivered".equals(next)) {
            if (!"shipped".equals(current)) {
                throw new BadRequestException(String.format("Invalid status transition from '%s' to '%s'", currentStatus, newStatus));
            }
            return;
        }

        // If target is 'cancelled' we expect caller to have validated canBeCancelled; reject here to be safe
        if ("cancelled".equals(next)) {
            throw new BadRequestException("Cancellation must be validated via order.canBeCancelled()");
        }

        // For non-terminal targets (pending, confirmed, processing, shipped) allow transition from any non-terminal current state
        // This lets admins move orders among progress statuses
        return;
    }

    /**
     * Helper method to evict all entries from a cache
     */
    private void evictCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    @Override
    @Transactional
    public OrderResponse updateOrder(UUID id, UpdateOrderRequest request) {
        log.info("Updating order {} with request: {}", id, request);

        CustomerOrder order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));

        boolean changed = false;
        String orderNumber = order.getOrderNumber();

        if (request.getPaymentMethodId() != null && !request.getPaymentMethodId().equals(order.getPaymentMethodId())) {
            order.setPaymentMethodId(request.getPaymentMethodId());
            changed = true;
        }
        if (request.getShippingMethodId() != null && !request.getShippingMethodId().equals(order.getShippingMethodId())) {
            order.setShippingMethodId(request.getShippingMethodId());
            changed = true;
        }

        // Process item updates if provided
        if (request.getItems() != null) {
            // Load existing items
            List<OrderItem> existingItems = orderItemRepository.findByOrderId(id);

            // Map existing items by id for quick lookup
            java.util.Map<java.util.UUID, OrderItem> existingById = existingItems.stream()
                    .filter(it -> it.getId() != null)
                    .collect(Collectors.toMap(OrderItem::getId, it -> it));

            List<OrderItem> resultingItems = new ArrayList<>();

            java.util.Map<UUID, Integer> stockDeltas = new java.util.HashMap<>();

            for (UpdateOrderRequest.OrderItemUpdateRequest itemReq : request.getItems()) {
                if (itemReq.getId() != null && existingById.containsKey(itemReq.getId())) {
                    // Update existing item
                    OrderItem existing = existingById.get(itemReq.getId());
                    if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                        // delete this item: stock should be restored by +existing.quantity
                        stockDeltas.merge(existing.getProductId(), existing.getQuantity(), Integer::sum);
                        // don't add to resultingItems
                        changed = true;
                        continue;
                    }

                    if (!existing.getQuantity().equals(itemReq.getQuantity())) {
                        int qtyDiff = itemReq.getQuantity() - existing.getQuantity();
                        // reduce stock by qtyDiff (can be negative to restore stock)
                        stockDeltas.merge(existing.getProductId(), -qtyDiff, Integer::sum);
                        existing.setQuantity(itemReq.getQuantity());
                        changed = true;
                    }
                    resultingItems.add(existing);
                } else {
                    // New item to add
                    if (itemReq.getProductId() == null || itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                        // ignore invalid
                        continue;
                    }
                    Product product = productRepository.findById(itemReq.getProductId())
                            .orElseThrow(() -> ResourceNotFoundException.forResource("Product", itemReq.getProductId()));

                    // validate stock
                    if (product.getStockQuantity() < itemReq.getQuantity()) {
                        throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
                    }

                    OrderItem newItem = OrderItem.fromProduct(product, itemReq.getQuantity());
                    newItem.setOrderId(id);
                    // will be saved below
                    resultingItems.add(newItem);
                    // reduce stock by quantity
                    stockDeltas.merge(product.getId(), -itemReq.getQuantity(), Integer::sum);
                    changed = true;
                }
            }

            // Apply stock deltas
            for (java.util.Map.Entry<UUID, Integer> e : stockDeltas.entrySet()) {
                UUID pid = e.getKey();
                int delta = e.getValue();
                if (delta == 0) continue;
                productRepository.updateStock(pid, delta);
            }

            // Persist item changes: delete all existing and re-insert resultingItems for simplicity
            orderItemRepository.deleteByOrderId(id);
            for (OrderItem item : resultingItems) {
                item.setOrderId(id);
                OrderItem saved = orderItemRepository.save(item);
                // update id if needed
                item.setId(saved.getId());
            }

            // attach items to order
            order.setOrderItems(resultingItems);

            // Recalculate subtotal/total with shipping
            BigDecimal newSubtotal = resultingItems.stream()
                    .map(OrderItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            order.setSubtotal(newSubtotal);
            
            // Calculate total with shipping cost
            BigDecimal shippingCost = BigDecimal.ZERO;
            if (order.getShippingMethodId() != null) {
                ShippingMethod shippingMethod = shippingMethodRepository.findById(order.getShippingMethodId()).orElse(null);
                if (shippingMethod != null && shippingMethod.getPrice() != null) {
                    shippingCost = shippingMethod.getPrice();
                }
            }
            order.setTotal(newSubtotal.add(shippingCost));
        }

        // Recalculate total if shipping method changed but items didn't
        if (changed && request.getItems() == null && request.getShippingMethodId() != null && 
            !request.getShippingMethodId().equals(order.getShippingMethodId())) {
            // Load current order items to get subtotal
            List<OrderItem> currentItems = orderItemRepository.findByOrderId(id);
            BigDecimal currentSubtotal = currentItems.stream()
                    .map(OrderItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Calculate new shipping cost
            BigDecimal newShippingCost = BigDecimal.ZERO;
            ShippingMethod newShippingMethod = shippingMethodRepository.findById(request.getShippingMethodId()).orElse(null);
            if (newShippingMethod != null && newShippingMethod.getPrice() != null) {
                newShippingCost = newShippingMethod.getPrice();
            }
            
            order.setSubtotal(currentSubtotal);
            order.setTotal(currentSubtotal.add(newShippingCost));
        }

        if (!changed) {
            log.debug("No changes detected for order {}", id);
            return mapToResponseWithDetails(order);
        }

        // Persist order changes
        orderRepository.update(order);

        // Refresh and prepare response
        CustomerOrder updated = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));

        OrderResponse response = mapToResponseWithDetails(updated);

        // Update caches
        Cache byIdCache = cacheManager.getCache(ORDERS_CACHE);
        if (byIdCache != null) {
            byIdCache.put("id:" + id, response);
        }
        Cache byNumberCache = cacheManager.getCache(ORDERS_CACHE);
        if (byNumberCache != null && orderNumber != null) {
            byNumberCache.put("number:" + orderNumber, response);
        }

        // Clear list cache
        evictCache(ORDERS_CACHE);

        log.info("Order {} updated successfully", id);
        return response;
    }
}
