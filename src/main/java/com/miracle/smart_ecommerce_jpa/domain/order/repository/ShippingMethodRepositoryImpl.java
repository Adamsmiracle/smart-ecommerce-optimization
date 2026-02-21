package com.miracle.smart_ecommerce_jpa.domain.order.repository;

import com.miracle.smart_ecommerce_jpa.common.util.JdbcUtils;
import com.miracle.smart_ecommerce_jpa.domain.order.entity.ShippingMethod;
import com.miracle.smart_ecommerce_jpa.domain.order.mapper.ShippingMethodMapper;
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
public class ShippingMethodRepositoryImpl implements ShippingMethodRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ShippingMethodMapper mapper;

    public ShippingMethodRepositoryImpl(JdbcTemplate jdbcTemplate, ShippingMethodMapper mapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public ShippingMethod save(ShippingMethod shippingMethod) {
        String sql = """
            INSERT INTO shipping_method (name, description, price, estimated_days, created_at)
            VALUES (?, ?, ?, ?, ?)
            RETURNING *
            """;
        OffsetDateTime now = OffsetDateTime.now();
        Object[] params = new Object[]{
                shippingMethod.getName(),
                shippingMethod.getDescription(),
                shippingMethod.getPrice(),
                shippingMethod.getEstimatedDays(),
                Timestamp.from(now.toInstant())
        };
        return jdbcTemplate.queryForObject(sql, params, mapper);
    }

    @Override
    @Transactional
    public ShippingMethod update(ShippingMethod shippingMethod) {
        String sql = """
            UPDATE shipping_method
            SET name = ?, description = ?, price = ?, estimated_days = ?,
                created_at = ?
            WHERE id = ?
            RETURNING *
            """;
        try {
            Object[] params = new Object[]{
                    shippingMethod.getName(),
                    shippingMethod.getDescription(),
                    shippingMethod.getPrice(),
                    shippingMethod.getEstimatedDays(),
                    Timestamp.from(OffsetDateTime.now().toInstant()),
                    shippingMethod.getId()
            };
            return jdbcTemplate.queryForObject(sql, params, mapper);
        } catch (EmptyResultDataAccessException e) {
            throw ResourceNotFoundException.forResource("ShippingMethod", shippingMethod.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ShippingMethod> findById(UUID id) {
        String sql = "SELECT * FROM shipping_method WHERE id = ?";
        try {
            ShippingMethod sm = jdbcTemplate.queryForObject(sql, new Object[]{id}, mapper);
            return Optional.ofNullable(sm);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShippingMethod> findAll(int page, int size) {
        JdbcUtils.validatePagination(page, size);
        String sql = "SELECT * FROM shipping_method ORDER BY created_at DESC LIMIT ? OFFSET ?";
        Object[] params = new Object[]{size, JdbcUtils.calculateOffset(page, size)};
        return jdbcTemplate.query(sql, params, mapper);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        String sql = "DELETE FROM shipping_method WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        if (rows == 0) throw ResourceNotFoundException.forResource("ShippingMethod", id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        String sql = "SELECT COUNT(*) FROM shipping_method WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }
}
