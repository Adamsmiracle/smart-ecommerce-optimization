package com.miracle.smart_ecommerce_jpa.domain.order.service;

import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.PaymentMethodRequest;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.PaymentMethodResponse;
import com.miracle.smart_ecommerce_jpa.domain.order.entity.PaymentMethod;
import com.miracle.smart_ecommerce_jpa.domain.order.repository.PaymentMethodRepository;
import com.miracle.smart_ecommerce_jpa.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentMethodServiceImpl implements PaymentMethodService {

    private final PaymentMethodRepository repository;

    @Override
    @Transactional
    public PaymentMethodResponse create(PaymentMethodRequest request) {
        PaymentMethod pm = PaymentMethod.builder()
                .userId(request.getUserId())
                .paymentType(request.getPaymentType())
                .provider(request.getProvider())
                .accountNumber(request.getAccountNumber())
                .expiryDate(request.getExpiryDate())
                .build();
        PaymentMethod saved = repository.save(pm);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentMethodResponse update(UUID id, PaymentMethodRequest request) {
        PaymentMethod existing = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("PaymentMethod", id));

        existing.setPaymentType(request.getPaymentType());
        existing.setProvider(request.getProvider());
        existing.setAccountNumber(request.getAccountNumber());
        existing.setExpiryDate(request.getExpiryDate());

        PaymentMethod updated = repository.update(existing);
        return toResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentMethodResponse getById(UUID id) {
        PaymentMethod pm = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("PaymentMethod", id));
        return toResponse(pm);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentMethodResponse> getByUserId(UUID userId, int page, int size) {
        List<PaymentMethod> list = repository.findByUserId(userId, page, size);
        long total = list.size();
        List<PaymentMethodResponse> responses = list.stream().map(this::toResponse).collect(Collectors.toList());
        return PageResponse.of(responses, page, size, total);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    private PaymentMethodResponse toResponse(PaymentMethod pm) {
        return PaymentMethodResponse.builder()
                .id(pm.getId())
                .userId(pm.getUserId())
                .paymentType(pm.getPaymentType())
                .provider(pm.getProvider())
                .maskedAccount(pm.getMaskedAccountNumber())
                .expiryDate(pm.getExpiryDate())
                .createdAt(pm.getCreatedAt())
                .build();
    }
}

