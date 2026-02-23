package com.miracle.smart_ecommerce_jpa.domain.user.mapper;

import com.miracle.smart_ecommerce_jpa.common.util.Utils;
import com.miracle.smart_ecommerce_jpa.domain.user.entity.User;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * RowMapper for User domain model.
 * Maps ResultSet rows to User objects.
 */
@Component
public class UserMapper implements RowMapper<User> {
    @Override
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {

        String role = null;
        try {
            role = rs.getString("roles");
        } catch (SQLException ex) {
            // column may be 'role' instead of 'roles' depending on DB migrations
            try {
                role = rs.getString("role");
            } catch (SQLException ex2) {
                role = null;
            }
        }

        return User.builder()
                .id(Utils.getUUID(rs, "id"))
                .emailAddress(rs.getString("email_address"))
                .firstName(rs.getString("first_name"))
                .lastName(rs.getString("last_name"))
                .phoneNumber(rs.getString("phone_number"))
                .passwordHash(rs.getString("password_hash"))
                .isActive(Utils.getBoolean(rs, "is_active"))
                .createdAt(Utils.getOffsetDateTime(rs, "created_at"))
                .updatedAt(Utils.getOffsetDateTime(rs, "updated_at"))
                .role(role)
                .build();
    }
}