package com.miracle.smart_ecommerce_security.common.util;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Utility class for JDBC operations.
 * Provides helper methods for extracting values from ResultSet.
 */
public final class Utils {

    private Utils() {
        // Utility class, prevent instantiation
    }

    /**
     * Get UUID from ResultSet, handling null values
     */
    public static UUID getUUID(ResultSet rs, String columnName) throws SQLException {
        Object obj = rs.getObject(columnName);
        if (obj == null) {
            return null;
        }
        if (obj instanceof UUID) {
            return (UUID) obj;
        }
        return UUID.fromString(obj.toString());
    }

    /**
     * Get OffsetDateTime from ResultSet, handling null values.
     * Always returns an OffsetDateTime in UTC when a TIMESTAMP is present.
     */
    public static OffsetDateTime getLocalDateTime(ResultSet rs, String columnName) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnName);
        if (timestamp == null) {
            return null;
        }
        // Use Instant -> UTC offset to avoid LocalDateTime/Zone issues
        return timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }

    /**
     * Alias for getLocalDateTime to make intent explicit (return OffsetDateTime)
     */
    public static OffsetDateTime getOffsetDateTime(ResultSet rs, String columnName) throws SQLException {
        return getLocalDateTime(rs, columnName);
    }

    /**
     * Get LocalDate from ResultSet, handling null values
     */
    public static LocalDate getLocalDate(ResultSet rs, String columnName) throws SQLException {
        java.sql.Date date = rs.getDate(columnName);
        return date != null ? date.toLocalDate() : null;
    }

    /**
     * Get Boolean from ResultSet, handling null values
     */
    public static Boolean getBoolean(ResultSet rs, String columnName) throws SQLException {
        boolean value = rs.getBoolean(columnName);
        return rs.wasNull() ? null : value;
    }

    /**
     * Get Integer from ResultSet, handling null values
     */
    public static Integer getInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    /**
     * Get Long from ResultSet, handling null values
     */
    public static Long getLong(ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }

    /**
     * Get String list from PostgreSQL JSONB array
     */
    public static List<String> getStringListFromJsonb(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        if (json == null || json.isEmpty() || json.equals("null")) {
            return new ArrayList<>();
        }

        // Simple JSON array parsing (for arrays like ["url1", "url2"])
        // Remove brackets and split by comma
        json = json.trim();
        if (json.startsWith("[") && json.endsWith("]")) {
            json = json.substring(1, json.length() - 1);
        }

        if (json.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        // Split by comma, handling quoted strings
        String[] parts = json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String part : parts) {
            String cleaned = part.trim();
            // Remove quotes
            if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
                cleaned = cleaned.substring(1, cleaned.length() - 1);
            }
            if (!cleaned.isEmpty()) {
                result.add(cleaned);
            }
        }
        return result;
    }

    /**
     * Convert String list to PostgreSQL JSONB format
     */
    public static String toJsonbArray(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJsonString(list.get(i))).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Escape special characters in JSON string
     */
    private static String escapeJsonString(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * Get String array from PostgreSQL Array type
     */
    public static List<String> getStringArray(ResultSet rs, String columnName) throws SQLException {
        Array array = rs.getArray(columnName);
        if (array == null) {
            return new ArrayList<>();
        }
        String[] strings = (String[]) array.getArray();
        return new ArrayList<>(Arrays.asList(strings));
    }

    /**
     * Calculate offset for pagination
     */
    public static int calculateOffset(int page, int size) {
        try {
            return Math.multiplyExact(page, size);
        } catch (ArithmeticException e) {
            // Overflow detected — surface a clear error to callers
            throw new IllegalArgumentException("Pagination offset overflow for page=" + page + " size=" + size, e);
        }
    }

    /**
     * Validate pagination parameters
     */
    public static void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Page size must be at least 1");
        }
        if (size > 100) {
            throw new IllegalArgumentException("Page size cannot exceed 100");
        }
    }
}

