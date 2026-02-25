# Data Seeding and Migration Guide

## 🌱 Overview

This project uses **Flyway** for database migrations and includes comprehensive data seeding for development and testing environments.

## 📁 File Structure

```
src/main/resources/db/migration/
├── V1__Initial_Data_Seeding.sql    # Initial data seeding with real UUIDs
└── V2__...                        # Future migrations
```

## 🆔 UUID Strategy

All seeded data uses **consistent, real UUIDs** for reliable testing:

### Categories (5 entities)
- `550e8400-e29b-41d4-a716-446655440000` - Electronics
- `550e8400-e29b-41d4-a716-446655440001` - Clothing  
- `550e8400-e29b-41d4-a716-446655440002` - Books
- `550e8400-e29b-41d4-a716-4466554403` - Home & Garden
- `550e8400-e29b-41d4-a716-4466554404` - Sports

### Users (5 entities)
- `660e8400-e29b-41d4-a716-446655440000` - John Doe (Customer)
- `660e8400-e29b-41d4-a716-4466554401` - Jane Smith (Customer)
- `660e8400-e29b-41d4-a716-4466554402` - Admin User (Admin)
- `660e8400-e29b-41d4-a716-4466554403` - Sarah Johnson (Customer)
- `660e8400-e29b-41d4-a716-4466554404` - Mike Wilson (Customer)

### Products (5 entities)
- `770e8400-e29b-41d4-a716-446655440000` - Laptop Pro 15" (Electronics)
- `770e8400-e29b-41d4-a716-446655440001` - Wireless Mouse (Electronics)
- `770e8400-e29b-41d4-a716-4466554402` - Winter Jacket (Clothing)
- `770e8400-e29b-41d4-a716-4466554403` - Programming Book (Books)
- `770e8400-e29b-41d4-a716-4466554404` - Garden Tool Set (Home & Garden)

## 🔗 Relationship Mapping

### Foreign Key Relationships
```
Users → Orders → Order_Items → Products
  ↓         ↓           ↓
John → ORD-001 → Laptop Pro
Jane → ORD-002 → Winter Jacket

Users → Reviews → Products
  ↓         ↓
John → 5⭐ → Laptop Pro
Sarah → 5⭐ → Winter Jacket

Users → Addresses
  ↓
John → 123 Main St
Jane → 456 Oak Ave

Users → Cart_Items → Products
  ↓         ↓
John → Mouse → Wireless Mouse
Mike → Book → Programming Book
```

## 🚀 Running the Application

### Development Mode (with seeding)
```bash
# Run with data seeding
./mvn spring-boot:run -Dspring.profiles.active=dev

# Or using IDE
# Set VM Options: -Dspring.profiles.active=dev
```

### Test Mode (verification only)
```bash
# Run with data verification
./mvn spring-boot:run -Dspring.profiles.active=test

# Or using IDE
# Set VM Options: -Dspring.profiles.active=test
```

### Production Mode (no seeding)
```bash
# Run production without seeding
./mvn spring-boot:run -Dspring.profiles.active=prod
```

## 📊 Seeded Data Summary

| Entity Type | Count | Relationships | Status |
|-------------|--------|--------------|---------|
| Categories  | 5      | → Products    | ✅ Complete |
| Users       | 5      | → Orders, Reviews, Addresses, Cart Items, Payment Methods | ✅ Complete |
| Products    | 5      | ← Categories, → Order Items, Reviews, Cart Items | ✅ Complete |
| Orders      | 3      | ← Users, → Order Items | ✅ Complete |
| Order Items | 4      | ← Orders, → Products | ✅ Complete |
| Reviews     | 5      | ← Users, → Products | ✅ Complete |
| Addresses   | 3      | ← Users | ✅ Complete |
| Cart Items  | 3      | ← Users, → Products | ✅ Complete |
| Payment Methods | 2    | ← Users | ✅ Complete |
| Shipping Methods | 3    | Standalone | ✅ Complete |

## 🧪 Flyway Configuration

### Migration Process
1. **Automatic**: Flyway runs migrations on application startup
2. **Validation**: Validates existing schema before applying
3. **Baseline**: Creates baseline if no migrations exist
4. **Ordering**: Migrations run in version order (V1, V2, etc.)

### Migration Files
- **Naming**: `V{number}__{description}.sql`
- **Versioning**: Sequential numbers (V1, V2, V3...)
- **Description**: Descriptive name with underscores
- **Idempotent**: Safe to run multiple times

## 🔍 Data Verification

The `DataVerification` class validates:
- ✅ **UUID Generation**: All sample UUIDs are valid
- ✅ **Relationship Integrity**: All foreign keys properly reference existing entities
- ✅ **Data Consistency**: No orphaned records
- ✅ **Type Safety**: All UUIDs are properly formatted

## 🛠️ Adding New Data

### 1. Create New Migration
```sql
-- V2__Add_Additional_Data.sql
INSERT INTO categories (id, category_name, description, created_at, updated_at)
VALUES 
    ('550e8400-e29b-41d4-a716-4466554405', 'Toys', 'Children toys and games', NOW(), NOW());
```

### 2. Update DataSeeder
```java
// Add new UUID to SampleUUIDs class
public static final UUID TOYS_CATEGORY = UUID.fromString("550e8400-e29b-41d4-a716-4466554405");
```

### 3. Update Verification
```java
// Add new verification in DataVerification
assert SampleUUIDs.TOYS_CATEGORY != null;
```

## 🧪 Testing Relationships

### API Testing Examples
```bash
# Test user with orders
curl "http://localhost:8080/api/users/660e8400-e29b-41d4-a716-446655440000/orders"

# Test product with reviews  
curl "http://localhost:8080/api/products/770e8400-e29b-41d4-a716-446655440000/reviews"

# Test category with products
curl "http://localhost:8080/api/categories/550e8400-e29b-41d4-a716-446655440000/products"
```

### Database Verification
```sql
-- Verify all relationships exist
SELECT 
    c.category_name,
    COUNT(p.id) as product_count
FROM categories c
LEFT JOIN products p ON p.category_id = c.id
GROUP BY c.id, c.category_name;

-- Verify user orders
SELECT 
    u.first_name,
    u.last_name,
    COUNT(o.id) as order_count
FROM users u
LEFT JOIN orders o ON o.user_id = u.id
GROUP BY u.id, u.first_name, u.last_name;
```

## 🎯 Benefits

### Development Benefits
- **🔄 Consistent Data**: Same UUIDs across all environments
- **🧪 Reliable Testing**: Predictable test scenarios
- **📊 Complete Coverage**: All entity types represented
- **🔗 Relationship Testing**: Foreign keys properly established
- **🚀 Fast Setup**: One-command data initialization

### Production Benefits
- **🧪 Migration Safety**: Flyway prevents schema drift
- **📈 Version Control**: Database changes tracked
- **🔄 Rollback Support**: Easy to undo changes
- **✅ Validation**: Prevents invalid migrations

## 🔧 Troubleshooting

### Common Issues
1. **Flyway Errors**: Check migration file naming and SQL syntax
2. **UUID Issues**: Verify UUID format and uniqueness
3. **Relationship Errors**: Ensure foreign keys exist before referencing
4. **Profile Issues**: Verify active Spring profile

### Debug Commands
```bash
# Check Flyway status
./mvn flyway:info

# Repair Flyway metadata
./mvn flyway:repair

# Validate migrations
./mvn flyway:validate
```

---

**🎉 Your database is now ready for development and testing with comprehensive seeded data!**
