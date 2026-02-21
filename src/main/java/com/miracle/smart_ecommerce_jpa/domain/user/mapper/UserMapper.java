package com.miracle.smart_ecommerce_jpa.domain.user.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miracle.smart_ecommerce_jpa.common.util.JdbcUtils;
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
                .id(JdbcUtils.getUUID(rs, "id"))
                .emailAddress(rs.getString("email_address"))
                .firstName(rs.getString("first_name"))
                .lastName(rs.getString("last_name"))
                .phoneNumber(rs.getString("phone_number"))
                .passwordHash(rs.getString("password_hash"))
                .isActive(JdbcUtils.getBoolean(rs, "is_active"))
                .createdAt(JdbcUtils.getOffsetDateTime(rs, "created_at"))
                .updatedAt(JdbcUtils.getOffsetDateTime(rs, "updated_at"))
                .role(role)
                .build();
    }
}