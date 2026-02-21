package com.miracle.smart_ecommerce_jpa.domain.order.repository;

import com.miracle.smart_ecommerce_jpa.common.util.JdbcUtils;
import com.miracle.smart_ecommerce_jpa.domain.order.entity.PaymentMethod;
import com.miracle.smart_ecommerce_jpa.domain.order.mapper.PaymentMethodMapper;
import com.miracle.smart_ecommerce_jpa.exception.ResourceNotFoundException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PaymentMethodRepositoryImpl implements PaymentMethodRepository {

    private final JdbcTemplate jdbcTemplate;
    private final PaymentMethodMapper mapper;

    public PaymentMethodRepositoryImpl(JdbcTemplate jdbcTemplate, PaymentMethodMapper mapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public PaymentMethod save(PaymentMethod pm) {
        String sql = """
            INSERT INTO payment_method (user_id, payment_type, provider, account_number, expiry_date, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING *
            """;
        OffsetDateTime now = OffsetDateTime.now();
        return jdbcTemplate.queryForObject(sql, mapper,
                pm.getUserId(), pm.getPaymentType(), pm.getProvider(), pm.getAccountNumber(), pm.getExpiryDate(), Timestamp.from(now.toInstant())
        );
    }

    @Override
    @Transactional
    public PaymentMethod update(PaymentMethod pm) {
        String sql = """
            UPDATE payment_method
            SET payment_type = ?, provider = ?, account_number = ?, expiry_date = ?, created_at = ?
            WHERE id = ?
            RETURNING *
            """;
        try {
            return jdbcTemplate.queryForObject(sql, mapper,
                    pm.getPaymentType(), pm.getProvider(), pm.getAccountNumber(), pm.getExpiryDate(), Timestamp.from(OffsetDateTime.now().toInstant()), pm.getId()
            );
        } catch (EmptyResultDataAccessException e) {
            throw ResourceNotFoundException.forResource("PaymentMethod", pm.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaymentMethod> findById(UUID id) {
        String sql = "SELECT * FROM payment_method WHERE id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, mapper, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentMethod> findByUserId(UUID userId, int page, int size) {
        JdbcUtils.validatePagination(page, size);
        String sql = "SELECT * FROM payment_method WHERE user_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, mapper, userId, size, JdbcUtils.calculateOffset(page, size));
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        String sql = "DELETE FROM payment_method WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        if (rows == 0) throw ResourceNotFoundException.forResource("PaymentMethod", id);
    }
}

