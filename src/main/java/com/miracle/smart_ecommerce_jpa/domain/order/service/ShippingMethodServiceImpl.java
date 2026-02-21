package com.miracle.smart_ecommerce_jpa.domain.order.service;

import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.ShippingMethodRequest;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.ShippingMethodResponse;
import com.miracle.smart_ecommerce_jpa.domain.order.entity.ShippingMethod;
import com.miracle.smart_ecommerce_jpa.domain.order.repository.ShippingMethodRepository;
import com.miracle.smart_ecommerce_jpa.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShippingMethodServiceImpl implements ShippingMethodService {

    private final ShippingMethodRepository repository;

    @Override
    @Transactional
    public ShippingMethodResponse create(ShippingMethodRequest request) {
        ShippingMethod sm = ShippingMethod.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .estimatedDays(request.getEstimatedDays())
                .build();

        ShippingMethod saved = repository.save(sm);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ShippingMethodResponse update(UUID id, ShippingMethodRequest request) {
        ShippingMethod existing = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("ShippingMethod", id));

        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setPrice(request.getPrice());
        existing.setEstimatedDays(request.getEstimatedDays());

        ShippingMethod updated = repository.update(existing);
        return toResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public ShippingMethodResponse getById(UUID id) {
        ShippingMethod sm = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("ShippingMethod", id));
        return toResponse(sm);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ShippingMethodResponse> getAll(int page, int size) {
        List<ShippingMethod> methods = repository.findAll(page, size);
        long total = methods.size();
        List<ShippingMethodResponse> responses = methods.stream().map(this::toResponse).collect(Collectors.toList());
        return PageResponse.of(responses, page, size, total);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    private ShippingMethodResponse toResponse(ShippingMethod sm) {
        return ShippingMethodResponse.builder()
                .id(sm.getId())
                .name(sm.getName())
                .description(sm.getDescription())
                .price(sm.getPrice())
                .estimatedDays(sm.getEstimatedDays())
                .createdAt(sm.getCreatedAt())
                .build();
    }
}

