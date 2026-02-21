package com.miracle.smart_ecommerce_jpa.domain.user.repository;

import java.time.OffsetDateTime;

import com.miracle.smart_ecommerce_jpa.common.util.JdbcUtils;
import com.miracle.smart_ecommerce_jpa.domain.user.entity.User;
import com.miracle.smart_ecommerce_jpa.exception.ResourceNotFoundException;
import com.miracle.smart_ecommerce_jpa.domain.user.mapper.UserMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC implementation of UserRepository.
 * Uses JdbcTemplate for all database operations.
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    private final JdbcTemplate jdbcTemplate;
    private final UserMapper userRowMapper;

    public UserRepositoryImpl(JdbcTemplate jdbcTemplate,
                              UserMapper userRowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRowMapper = userRowMapper;
    }

    @Override
    @Transactional
    public User save(User user) {
        String sql = """
            INSERT INTO app_user (email_address, first_name, last_name, phone_number, password_hash, is_active, created_at, updated_at, role)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id, email_address, first_name, last_name, phone_number, password_hash, is_active, created_at, updated_at, role
            """;

        OffsetDateTime now = OffsetDateTime.now();
        try {
            String role = user.getRole();

            return jdbcTemplate.queryForObject(sql, userRowMapper,
                    user.getEmailAddress(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getPhoneNumber(),
                    user.getPasswordHash(),
                    user.getIsActive() != null ? user.getIsActive() : true,
                    Timestamp.from(now.toInstant()),
                    Timestamp.from(now.toInstant()),
                    role
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist role", e);
        }
    }

    @Override
    @Transactional
    public User update(User user) {
        String sql = """
            UPDATE app_user 
            SET email_address = ?, first_name = ?, last_name = ?, phone_number = ?, 
                password_hash = ?, is_active = ?, updated_at = ?, role = ?
            WHERE id = ?
            RETURNING id, email_address, first_name, last_name, phone_number, password_hash, is_active, created_at, updated_at, role
            """;

        try {
            String role = user.getRole();

            return jdbcTemplate.queryForObject(sql, userRowMapper,
                    user.getEmailAddress(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getPhoneNumber(),
                    user.getPasswordHash(),
                    user.getIsActive(),
                    Timestamp.from(OffsetDateTime.now().toInstant()),
                    role,
                    user.getId()
            );
        } catch (EmptyResultDataAccessException e) {
            throw ResourceNotFoundException.forResource("User", user.getId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist role", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        String sql = "SELECT * FROM app_user WHERE id = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, id);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM app_user WHERE email_address = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, email);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        String sql = "SELECT * FROM app_user ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, userRowMapper);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll(int page, int size) {
        JdbcUtils.validatePagination(page, size);
        String sql = "SELECT * FROM app_user ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, userRowMapper, size, JdbcUtils.calculateOffset(page, size));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findActiveUsers(int page, int size) {
        JdbcUtils.validatePagination(page, size);
        String sql = "SELECT * FROM app_user WHERE is_active = true ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, userRowMapper, size, JdbcUtils.calculateOffset(page, size));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> search(String keyword, int page, int size) {
        JdbcUtils.validatePagination(page, size);
        String sql = """
            SELECT * FROM app_user 
            WHERE LOWER(first_name) LIKE LOWER(?) 
               OR LOWER(last_name) LIKE LOWER(?) 
               OR LOWER(email_address) LIKE LOWER(?)
            ORDER BY created_at DESC 
            LIMIT ? OFFSET ?
            """;
        String searchPattern = "%" + keyword + "%";
        return jdbcTemplate.query(sql, userRowMapper,
                searchPattern, searchPattern, searchPattern,
                size, JdbcUtils.calculateOffset(page, size));
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        String sql = "DELETE FROM app_user WHERE id = ?";
        int rowsAffected = jdbcTemplate.update(sql, id);
        if (rowsAffected == 0) {
            throw ResourceNotFoundException.forResource("User", id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        String sql = "SELECT COUNT(*) FROM app_user WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM app_user WHERE email_address = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        String sql = "SELECT COUNT(*) FROM app_user";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public long countActive() {
        String sql = "SELECT COUNT(*) FROM app_user WHERE is_active = true";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public long countByKeyword(String keyword) {
        String sql = """
            SELECT COUNT(*) FROM app_user 
            WHERE LOWER(first_name) LIKE LOWER(?) 
               OR LOWER(last_name) LIKE LOWER(?) 
               OR LOWER(email_address) LIKE LOWER(?)
            """;
        String searchPattern = "%" + keyword + "%";
        Long count = jdbcTemplate.queryForObject(sql, Long.class,
                searchPattern, searchPattern, searchPattern);
        return count != null ? count : 0;
    }

    @Override
    @Transactional
    public void setActiveStatus(UUID id, boolean isActive) {
        String sql = "UPDATE app_user SET is_active = ?, updated_at = ? WHERE id = ?";
        int rowsAffected = jdbcTemplate.update(sql, isActive, Timestamp.from(OffsetDateTime.now().toInstant()), id);
        if (rowsAffected == 0) {
            throw ResourceNotFoundException.forResource("User", id);
        }
    }
}

