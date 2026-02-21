package com.miracle.smart_ecommerce_jpa.domain.category.repository;

import com.miracle.smart_ecommerce_jpa.domain.category.entity.Category;
import com.miracle.smart_ecommerce_jpa.exception.ResourceNotFoundException;
import com.miracle.smart_ecommerce_jpa.domain.category.mapper.CategoryMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC implementation of CategoryRepository.
 */
@Repository
public class CategoryRepositoryImpl implements CategoryRepository {

    private final JdbcTemplate jdbcTemplate;
    private final CategoryMapper categoryRowMapper;

    public CategoryRepositoryImpl(JdbcTemplate jdbcTemplate, CategoryMapper categoryRowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.categoryRowMapper = categoryRowMapper;
    }

    @Override
    @Transactional
    public Category save(Category category) {
        String sql = """
            INSERT INTO product_category (category_name)
            VALUES (?)
            RETURNING *
            """;

        return jdbcTemplate.queryForObject(sql, categoryRowMapper,
                category.getCategoryName()
        );
    }

    @Override
    @Transactional
    public Category update(Category category) {
        String sql = """
            UPDATE product_category
            SET category_name = ?
            WHERE id = ?
            RETURNING *
            """;

        try {
            return jdbcTemplate.queryForObject(sql, categoryRowMapper,
                    category.getCategoryName(),
                    category.getId()
            );
        } catch (EmptyResultDataAccessException e) {
            throw ResourceNotFoundException.forResource("Category", category.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Category> findById(UUID id) {
        String sql = "SELECT * FROM product_category WHERE id = ?";
        try {
            Category category = jdbcTemplate.queryForObject(sql, categoryRowMapper, id);
            return Optional.ofNullable(category);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Category> findByName(String name) {
        String sql = "SELECT * FROM product_category WHERE category_name = ?";
        try {
            Category category = jdbcTemplate.queryForObject(sql, categoryRowMapper, name);
            return Optional.ofNullable(category);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findAll() {
        String sql = "SELECT * FROM product_category ORDER BY category_name";
        return jdbcTemplate.query(sql, categoryRowMapper);
    }


    @Override
    @Transactional(readOnly = true)
    public List<Category> findByParentId(UUID parentId) {
        // parent_category_id not present in current schema; return empty list
        return List.of();
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        String sql = "DELETE FROM product_category WHERE id = ?";
        int rowsAffected = jdbcTemplate.update(sql, id);
        if (rowsAffected == 0) {
            throw ResourceNotFoundException.forResource("Category", id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        String sql = "SELECT COUNT(*) FROM product_category WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        String sql = "SELECT COUNT(*) FROM product_category WHERE category_name = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, name);
        return count != null && count > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        String sql = "SELECT COUNT(*) FROM product_category";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

}
