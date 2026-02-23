package com.miracle.smart_ecommerce_jpa.domain.user.mapper;

import com.miracle.smart_ecommerce_jpa.common.util.Utils;
import com.miracle.smart_ecommerce_jpa.domain.user.entity.Address;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * RowMapper for Address domain model.
 * Maps ResultSet rows to Address objects.
 */
@Component
public class AddressMapper implements RowMapper<Address> {

    @Override
    public Address mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Address.builder()
                .id(Utils.getUUID(rs, "id"))
                .userId(Utils.getUUID(rs, "user_id"))
                .addressLine(rs.getString("address_line"))
                .city(rs.getString("city"))
                .region(rs.getString("region"))
                .country(rs.getString("country"))
                .postalCode(rs.getString("postal_code"))
                .addressType(rs.getString("address_type"))
                .createdAt(Utils.getOffsetDateTime(rs, "created_at"))
                .build();
    }
}
