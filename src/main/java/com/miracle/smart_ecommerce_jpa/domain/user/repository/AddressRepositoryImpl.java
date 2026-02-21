package com.miracle.smart_ecommerce_jpa.domain.user.repository;

import java.time.OffsetDateTime;

import com.miracle.smart_ecommerce_jpa.domain.user.entity.Address;
import com.miracle.smart_ecommerce_jpa.domain.user.mapper.AddressMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC implementation of AddressRepository.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class AddressRepositoryImpl implements AddressRepository {

    private final JdbcTemplate jdbcTemplate;
    private final AddressMapper addressMapper;

    @Override
    @Transactional
    public Address save(Address address) {
        String sql = """
            INSERT INTO address (user_id, address_line, city, region, country, postal_code, address_type, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id, user_id, address_line, city, region, country, postal_code, address_type, created_at
            """;

        OffsetDateTime now = OffsetDateTime.now();
        return jdbcTemplate.queryForObject(sql, addressMapper,
                address.getUserId(),
                address.getAddressLine(),
                address.getCity(),
                address.getRegion(),
                address.getCountry(),
                address.getPostalCode(),
                address.getAddressType() != null ? address.getAddressType() : "shipping",
                Timestamp.from(now.toInstant())
        );
    }

    @Override
    @Transactional
    public Address update(Address address) {
        String sql = """
            UPDATE address 
            SET address_line = ?, city = ?, region = ?, country = ?, postal_code = ?, address_type = ?
            WHERE id = ?
            RETURNING id, user_id, address_line, city, region, country, postal_code, address_type, created_at
            """;

        return jdbcTemplate.queryForObject(sql, addressMapper,
                address.getAddressLine(),
                address.getCity(),
                address.getRegion(),
                address.getCountry(),
                address.getPostalCode(),
                address.getAddressType(),
                address.getId()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Address> findById(UUID id) {
        String sql = "SELECT * FROM address WHERE id = ?";
        try {
            Address address = jdbcTemplate.queryForObject(sql, addressMapper, id);
            return Optional.ofNullable(address);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Address> findAll() {
        String sql = "SELECT id, user_id, address_line, city, region, country, postal_code, address_type, created_at FROM address ORDER BY created_at DESC";
        List<Address> list = jdbcTemplate.query(sql, addressMapper);
        log.debug("findAll addresses SQL executed, returned {} rows", list.size());
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Address> findByUserId(UUID userId) {
        String sql = "SELECT id, user_id, address_line, city, region, country, postal_code, address_type, created_at FROM address WHERE user_id = ? ORDER BY created_at DESC";
        List<Address> list = jdbcTemplate.query(sql, addressMapper, userId);
        log.info("findByUserId executed for userId={} SQL='{}' returned {} rows", userId, sql, list.size());
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Address> findByUserIdAndType(UUID userId, String addressType) {
        // Compare lower-case and treat NULL as 'shipping' so older rows without type are considered shipping by default
        String sql = "SELECT id, user_id, address_line, city, region, country, postal_code, address_type, created_at FROM address " +
                     "WHERE user_id = ? AND LOWER(COALESCE(address_type, 'shipping')) = LOWER(?) ORDER BY created_at DESC";
        List<Address> list = jdbcTemplate.query(sql, addressMapper, userId, addressType);
        log.info("findByUserIdAndType executed for userId={} type={} returned {} rows", userId, addressType, list.size());
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Address> findDefaultByUserId(UUID userId) {
        // Schema doesn't include is_default. Return most recent address as default.
        String sql = "SELECT id, user_id, address_line, city, region, country, postal_code, address_type, created_at FROM address WHERE user_id = ? ORDER BY created_at DESC LIMIT 1";
        try {
            Address address = jdbcTemplate.queryForObject(sql, addressMapper, userId);
            return Optional.ofNullable(address);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        String sql = "SELECT COUNT(*) FROM address WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        String sql = "DELETE FROM address WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    @Transactional
    public void clearDefaultForUserAndType(UUID userId, String addressType) {
        // No is_default column in schema. This method is a no-op.
    }
}
