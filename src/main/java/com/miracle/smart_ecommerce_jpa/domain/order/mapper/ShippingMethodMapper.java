package com.miracle.smart_ecommerce_jpa.domain.order.mapper;

import com.miracle.smart_ecommerce_jpa.common.util.Utils;
import com.miracle.smart_ecommerce_jpa.domain.order.entity.ShippingMethod;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class ShippingMethodMapper implements RowMapper<ShippingMethod> {

    @Override
    public ShippingMethod mapRow(ResultSet rs, int rowNum) throws SQLException {
        return ShippingMethod.builder()
                .id(Utils.getUUID(rs, "id"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .price(rs.getBigDecimal("price"))
                .estimatedDays(rs.getObject("estimated_days") == null ? null : rs.getInt("estimated_days"))
                .createdAt(Utils.getOffsetDateTime(rs, "created_at"))
                .build();
    }
}

