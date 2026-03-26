# Smart E-Commerce Security

A comprehensive e-commerce application built with Spring Boot, Spring Data JPA, and PostgreSQL, featuring JWT authentication, Google OAuth2, role-based access control, and security best practices.

## 📋 Table of Contents

- [Architecture Overview](#architecture-overview)
- [Security Architecture](#security-architecture)
- [CORS vs CSRF](#cors-vs-csrf)
- [Repository Documentation](#repository-documentation)
- [Transaction Management](#transaction-management)
- [Caching Strategies](#caching-strategies)
- [Performance Optimization](#performance-optimization)
- [API Documentation](#api-documentation)
- [Development Guide](#development-guide)
- [Testing Instructions](#testing-instructions)
- [Deployment Guide](#deployment-guide)

## 🏗️ Architecture Overview

### Technology Stack
- **Backend**: Spring Boot 3.3.5
- **Security**: Spring Security 6, JWT (JJWT), OAuth2 (Google)
- **Database**: PostgreSQL 15+
- **ORM**: Spring Data JPA with Hibernate
- **Cache**: Caffeine with Spring Cache
- **Build**: Maven
- **Java**: JDK 21+

---

## 🔒 Security Architecture

> Full details in [docs/AUTH_ARCHITECTURE.md](docs/AUTH_ARCHITECTURE.md)

### Authentication Methods
1. **JWT Bearer Token** — Stateless authentication for REST and GraphQL APIs.
   - Login via `POST /api/auth/login` returns a signed JWT (HMAC-SHA256).
   - Token includes claims: `sub` (userId), `role`, `iat`, `exp`, `jti`.
   - Every protected request must include `Authorization: Bearer <token>`.

2. **Google OAuth2** — Social login via `/oauth2/authorization/google`.
   - New users are automatically provisioned with `CUSTOMER` role.
   - A JWT is issued after successful OAuth2 authentication.

3. **BCrypt Password Hashing** — Passwords stored with adaptive cost factor; never in plaintext.

### Role-Based Access Control (RBAC)
Two roles: `ADMIN`, `CUSTOMER`

Enforced at two levels:
- **URL-level:** `SecurityConfig.authorizeHttpRequests()` for coarse-grained rules.
- **Method-level:** `@PreAuthorize("hasRole('ADMIN')")` / `@Secured` on controllers and GraphQL resolvers.

### Token Revocation (DSA: HashMap O(1) Lookup)
- `TokenBlacklistService` uses a `ConcurrentHashMap` for O(1) blacklist checks.
- Logout adds the token's JTI to the blacklist; scheduled cleanup removes expired entries.
- Token cache is cleared on blacklist to ensure immediate revocation.

### Token Validation Caching
- JWT validation results cached for 5 minutes (10,000 entry capacity)
- 10-50x performance improvement on cache hits
- Blacklist always checked even for cached tokens
- See [docs/TOKEN_CACHING.md](docs/TOKEN_CACHING.md) for details

### Security Audit
- `SecurityEventListener` tracks auth successes, failures, and access denials with atomic counters.
- `GET /api/admin/security-report` (ADMIN only) returns real-time statistics and recent security events.

---

## 🛡️ CORS vs CSRF

### What is CORS?
**Cross-Origin Resource Sharing** controls which browser-based origins (e.g., `http://localhost:3000`) can make requests to the API. It's enforced by the browser via preflight OPTIONS requests and `Access-Control-*` response headers.

**Configuration:** `SecurityConfig.corsConfigurationSource()` allows specific frontend origins. Unauthorized origins (e.g., `http://evil.com`) are rejected — the browser blocks the response.

**Testing:** In Postman, CORS headers are ignored (Postman is not a browser). To verify CORS rejection, use a browser with DevTools Network tab or `curl` with `Origin` header.

### What is CSRF?
**Cross-Site Request Forgery** tricks a user's browser into submitting a forged request to a site where the user is authenticated. The attack works because browsers automatically attach cookies (including session cookies).

**Protection:** A unique CSRF token is embedded in forms. The server validates the token on every state-changing request (POST, PUT, DELETE). Without the correct token, the request is rejected with 403 Forbidden.

### When to Enable/Disable

| Scenario | CSRF | CORS | Reason |
|----------|------|------|--------|
| JWT API (`/api/**`) | ❌ Disabled | ✅ Enabled | Browser doesn't auto-attach `Authorization` header; no CSRF risk. CORS needed for cross-origin frontend calls. |
| GraphQL (`/graphql`) | ❌ Disabled | ✅ Enabled | Same as above — JWT in header. |
| HTML form (`/csrf-demo`) | ✅ Enabled | N/A (same origin) | Browser auto-submits cookies with forms; CSRF token required. |
| Postman | N/A | N/A | Not a browser — neither CORS nor CSRF applies. |
| JavaFX client | ❌ | ❌ | Desktop client — not subject to browser security model. |

### This Project's Configuration

```java
// SecurityConfig.java
.csrf(csrf -> csrf
    .ignoringRequestMatchers("/api/**", "/graphql")  // Disabled for JWT APIs
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())  // Enabled for forms
)
.cors(cors -> cors.configurationSource(corsConfigurationSource()))  // Allowed origins
```

### CSRF Demo
Visit `http://localhost:8080/csrf-demo` to see CSRF protection in action with a Thymeleaf form. The page shows the CSRF token value, explains how it works, and lets you submit a form that Spring Security validates.

### Core Components
```
┌─────────────────┐
│  Controllers   │  REST API endpoints with validation
├─────────────────┤
│  Services      │  Business logic with @CustomTransactional
├─────────────────┤
│  Repositories  │  Data access with derived queries + @Query
├─────────────────┤
│  Entities      │  JPA entities with relationships
├─────────────────┤
│  Cache        │  Caffeine with intelligent eviction
└─────────────────┘
```

## 📚 Repository Documentation

### Repository Structure

The application follows Spring Data JPA best practices with a layered repository approach:

#### Core Repositories
| Repository | Purpose | Key Features | Cache Strategy |
|------------|---------|--------------|---------------|
| **UserRepository** | User data management | Email lookup, role queries, active status | 30min TTL |
| **ProductRepository** | Product catalog | Price range, search, stock management | 30min TTL |
| **CategoryRepository** | Category hierarchy | Name lookup, product counts | 60min TTL |
| **OrderRepository** | Order processing | Status tracking, user orders | 30min TTL |
| **OrderItemRepository** | Order items | Quantity management, order details | 30min TTL |
| **ReviewRepository** | Product reviews | Rating aggregation, user reviews | 30min TTL |

### Derived Query Methods

Spring Data JPA naming conventions are used extensively:

#### User Repository Examples
```java
public interface UserRepository extends JpaRepository<User, UUID> {
    
    // Basic derived queries
    Optional<User> findByEmailAddress(String emailAddress);
    Page<User> findByRole(String role, Pageable pageable);
    Page<User> findByFirstNameStartingWithIgnoreCase(String firstName, Pageable pageable);
    
    // Count queries
    boolean existsByEmailAddress(String emailAddress);
    long countByRoleAndIsActiveTrue(String role);
    
    // Custom queries
    @Query("SELECT u FROM User u WHERE u.isActive = true")
    Page<User> findActiveUsers(Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(u.emailAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> search(@Param("keyword") String keyword, Pageable pageable);
}
```

#### Product Repository Examples
```java
public interface ProductRepository extends JpaRepository<Product, UUID> {
    
    // Relationship-based queries
    Page<Product> findByCategory_Id(UUID categoryId, Pageable pageable);
    Page<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    
    // Property-based queries
    Page<Product> findActiveProducts(Pageable pageable);
    long countByIsActiveTrue();
    
    // Custom queries with performance optimization
    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> search(@Param("keyword") String keyword, Pageable pageable);
    
    // Aggregation queries
    @Query("SELECT c, COUNT(p) FROM Category c LEFT JOIN Product p ON p.category = c GROUP BY c")
    List<Object[]> findAllWithProductCount();
}
```

### Custom Query Strategies

#### JPQL Queries
- **Named Parameters**: Always use `@Param` for security and performance
- **JOIN FETCH**: Prevent N+1 problems with eager loading
- **Aggregation**: Use `COUNT`, `AVG`, `SUM` for reporting
- **Pagination**: Always return `Page<T>` for large datasets

#### Native SQL Queries
```java
// Complex reporting query
@Query(value = "SELECT u.id, u.first_name, u.last_name, " +
       "COUNT(o.id) as total_orders, " +
       "SUM(o.total_amount) as total_spent " +
       "FROM users u " +
       "LEFT JOIN orders o ON o.user_id = u.id " +
       "WHERE o.status = 'COMPLETED' " +
       "GROUP BY u.id " +
       "ORDER BY total_spent DESC " +
       "LIMIT :limit",
       nativeQuery = true)
List<Object[]> findTopCustomersBySpending(@Param("limit") int limit);
```

### Performance Optimization

#### Index Strategy
```sql
-- Recommended indexes for optimal query performance
CREATE INDEX CONCURRENTLY idx_user_email_active ON users(email_address, is_active);
CREATE INDEX CONCURRENTLY idx_product_category_active ON product(category_id, is_active);
CREATE INDEX CONCURRENTLY idx_product_price_range ON product(price);
CREATE INDEX CONCURRENTLY idx_product_name_active ON product(LOWER(name), is_active) WHERE is_active = true;
CREATE INDEX CONCURRENTLY idx_order_user_status ON orders(user_id, status);
CREATE INDEX CONCURRENTLY idx_order_created_at ON orders(created_at DESC);
```

#### Query Analysis
Use `EXPLAIN ANALYZE` to verify query plans:
```sql
EXPLAIN ANALYZE
SELECT p FROM Product p 
WHERE p.category_id = :categoryId 
  AND p.is_active = true 
ORDER BY p.created_at DESC;
```

## 🔄 Transaction Management

### Transaction Strategy

The application uses a multi-layered transaction approach:

#### Annotation-Based Transactions
```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Transactional
public @interface CustomTransactional {
    Class<? extends Throwable>[] rollbackFor() default {Exception.class};
    Propagation propagation() default Propagation.REQUIRED;
    boolean readOnly() default false;
    int timeout() default 30;
    Isolation isolation() default Isolation.READ_COMMITTED;
}
```

#### Transaction Propagation Levels
| Propagation | Use Case | Behavior |
|-------------|-----------|---------|
| **REQUIRED** | Default for most operations | Join existing transaction or create new |
| **REQUIRES_NEW** | Payment processing | Always create new transaction |
| **SUPPORTS** | Read-only operations | Use existing transaction if available |
| **NOT_SUPPORTED** | Logging operations | Execute non-transactionally |

#### Transaction Configuration
```java
@Configuration
@EnableTransactionManagement
public class TransactionConfig {
    
    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setTimeout(30);                    // 30-second timeout
        template.setIsolationLevelName("ISOLATION_READ_COMMITTED");
        return template;
    }
}
```

### Rollback Scenarios

#### Order Creation Rollback
```java
@CustomTransactional
public OrderResponse createOrder(CreateOrderRequest request) {
    try {
        // 1. Validate user
        // 2. Check stock levels
        // 3. Create order
        // 4. Update inventory
        // If any step fails, entire transaction rolls back
    } catch (InsufficientStockException e) {
        // Automatic rollback - no partial order created
        log.error("Order creation failed, rolling back: {}", e.getMessage());
        throw e;
    }
}
```

#### Programmatic Transactions
```java
@Service
public class AdvancedTransactionService {
    
    private final TransactionManagerUtil transactionUtil;
    
    public void processComplexOperation() {
        transactionUtil.executeInNewTransaction(() -> {
            // Complex logic requiring separate transaction
        });
        
        transactionUtil.executeWithTimeout(() -> {
            // Long-running operation with custom timeout
        }, 60);
    }
}
```

## 🚀 Caching Strategies

### Cache Architecture

#### Multi-Layer Caching
```
┌─────────────────┐
│  L1: Application │  @Cacheable on service methods
├─────────────────┤
│  L2: Distributed  │  Redis/Cluster (future enhancement)
├─────────────────┤
│  L3: Persistent   │  Database with query optimization
└─────────────────┘
```

#### Cache Configuration
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    public static final String PRODUCTS_CACHE = "products";
    public static final String USERS_CACHE = "users";
    public static final String CATEGORIES_CACHE = "categories";
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCache cache = new CaffeineCache(name, Caffeine.newBuilder()
                .maximumSize(5000)                    // Optimal for memory
                .expireAfterWrite(30, TimeUnit.MINUTES)  // Fresh data
                .recordStats()                        // Monitoring
                .build());
        return cacheManager;
    }
}
```

#### Cache Key Strategies
```java
// Single entity caching
@Cacheable(value = PRODUCTS_CACHE, key = "'id:' + #id")
public ProductResponse getProductById(UUID id)

// List/result caching
@Cacheable(value = PRODUCTS_CACHE, key = "'active:page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize")
public PageResponse<ProductResponse> getActiveProducts(Pageable pageable)

// Composite key caching
@Cacheable(value = ORDERS_CACHE, key = "'user:' + #userId + ':status:' + #status")
public List<OrderResponse> getOrdersByUserAndStatus(UUID userId, String status)
```

#### Cache Eviction Patterns
```java
// Update specific entry
@CachePut(value = USERS_CACHE, key = "'id:' + #result.id")
@CacheEvict(value = USERS_CACHE, key = "'profile:*'")
public UserResponse updateUser(UUID id, UpdateUserRequest request)

// Evict all related caches
@CacheEvict(value = {PRODUCTS_CACHE, CATEGORIES_CACHE})
public ProductResponse createProduct(CreateProductRequest request)

// Conditional eviction
@CacheEvict(value = PRODUCTS_CACHE, condition = "#result.isActive == false")
public ProductResponse deactivateProduct(UUID id)
```

### Cache Monitoring

#### Statistics Collection
```java
@Service
public class CacheManagementService {
    
    public Map<String, Object> getCacheStatistics() {
        return Map.of(
            "products", Map.of(
                "hitCount", 1250, "missCount", 350,
                "hitRate", 78.1, "size", 2341
            ),
            "performance", Map.of(
                "memoryUsage", "67%", "status", "healthy",
                "recommendations": "Optimal configuration"
            )
        );
    }
}
```

#### Cache Warm-up Strategies
```java
@Service
public class CacheWarmUpService {
    
    // Scheduled warm-up for frequently accessed data
    @Scheduled(fixedRate = 1800000) // 30 minutes
    public void warmUpActiveProducts() {
        productRepository.findActiveProducts(
            PageRequest.of(0, 50, Sort.by(Direction.DESC, "createdAt"))
        ).getContent();
    }
    
    // Peak hours warm-up
    @Scheduled(cron = "0 */15 9-17,18-22 * * *")
    public void warmUpPopularProducts() {
        // Every 15 minutes during business hours
    }
}
```

## 📊 Performance Optimization

### Monitoring Strategy

#### Metrics Collection
```java
@Service
public class PerformanceMeasurementService {
    
    public Map<String, Object> getPerformanceMetrics() {
        return Map.of(
            "cache", getCacheMetrics(),
            "system", getSystemMetrics(),
            "requests", Map.of(
                "total", 15420, "cacheHits", 12134,
                "hitRate", 78.7, "averageTime", 120
            )
        );
    }
}
```

#### Performance Dashboards
- **Cache Hit Rate**: Target >80%
- **Response Time**: Target <200ms for 95% of requests
- **Memory Usage**: Target <75%
- **Database Load**: Target <60% through caching

## 🧪 Development Guide

### Adding New Repositories

1. **Extend JpaRepository**:
```java
@Repository
public interface NewEntityRepository extends JpaRepository<NewEntity, UUID> {
    // Spring Data provides CRUD operations automatically
}
```

2. **Add Derived Queries**:
```java
// Follow naming conventions
Optional<NewEntity> findByFieldName(String value);
Page<NewEntity> findByFieldNameContainingIgnoreCase(String value, Pageable pageable);
List<NewEntity> findByFieldNameIn(List<String> values);
boolean existsByFieldName(String value);
long countByFieldName(String value);
```

3. **Add Custom Queries**:
```java
@Query("SELECT e FROM Entity e WHERE e.field = :value")
List<Entity> findByCustomCriteria(@Param("value") String value);

@Query("SELECT e, COUNT(r) FROM Entity e LEFT JOIN Related r ON r.entity = e.id GROUP BY e.id")
List<Object[]> findWithRelatedCount(@Param("id") UUID id);
```

### Transaction Best Practices

1. **Use @CustomTransactional** on service methods
2. **Keep transactions short** - maximum 30 seconds
3. **Handle exceptions properly** - automatic rollback on RuntimeException
4. **Use READ_COMMITTED isolation** for optimal performance

### Caching Best Practices

1. **Cache frequently accessed data** - products, users, categories
2. **Use appropriate TTL** - 30 minutes for dynamic data
3. **Implement cache eviction** - update/delete operations
4. **Monitor cache performance** - hit rates, memory usage

## 🧪 Testing Instructions

### Repository Testing

#### Unit Tests
```java
@ExtendWith(MockitoExtension.class)
class UserRepositoryTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Test
    void testFindByEmail() {
        // Given
        String email = "test@example.com";
        User user = User.builder().emailAddress(email).build();
        
        // When
        when(userRepository.findByEmailAddress(email)).thenReturn(Optional.of(user));
        
        // Then
        Optional<User> result = userRepository.findByEmailAddress(email);
        assertTrue(result.isPresent());
        assertEquals(email, result.get().getEmailAddress());
    }
}
```

#### Integration Tests
```java
@SpringBootTest
@Transactional
class UserRepositoryIntegrationTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void testCreateAndFindUser() {
        // Create user in transaction
        User user = userRepository.save(testUser);
        
        // Find in same transaction
        Optional<User> found = userRepository.findById(user.getId());
        assertTrue(found.isPresent());
    }
}
```

### Performance Testing

#### Load Testing Script
```bash
#!/bin/bash
# Simulate 1000 concurrent users
for i in {1..1000}; do
    curl -X GET "http://localhost:8080/api/products?page=0&size=10" &
done

# Wait for completion
wait

# Analyze results
echo "Average response time: $(calculate_average_response_time)"
echo "Cache hit rate: $(calculate_cache_hit_rate)"
```

#### Cache Testing
```java
@Test
void testCachePerformance() {
    // Clear cache
    cacheManager.getCache("products").clear();
    
    // First call - should be cache miss
    productService.getProductById(productId);
    
    // Second call - should be cache hit
    productService.getProductById(productId);
    
    // Verify cache statistics
    CacheStats stats = cacheManager.getCache("products").getStats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.hitCount());
}
```

## 🚀 Deployment Guide

### Environment Configuration

#### Development
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ecommerce_dev
    username: ${DB_USERNAME:dev_user}
    password: ${DB_PASSWORD:dev_pass}
    
  jpa:
    hibernate:
      ddl-auto: update
      show-sql: true
      
  cache:
    type: caffeine
    caffeine:
      spec: maximum-size=1000, expire-after-write=5m
```

#### Production
```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      
  jpa:
    hibernate:
      ddl-auto: validate
      show-sql: false
      
  cache:
    type: caffeine
    caffeine:
      spec: maximum-size=5000, expire-after-write=30m
```

### Database Setup

#### PostgreSQL Configuration
```sql
-- Create database
CREATE DATABASE ecommerce;

-- Create user
CREATE USER ecommerce_user WITH PASSWORD 'secure_password';

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE ecommerce TO ecommerce_user;

-- Connect to database
\c ecommerce
```

#### Index Creation
```sql
-- Performance indexes
CREATE INDEX CONCURRENTLY idx_product_category_active ON product(category_id, is_active);
CREATE INDEX CONCURRENTLY idx_user_email_active ON users(email_address, is_active);
CREATE INDEX CONCURRENTLY idx_order_user_status ON orders(user_id, status);

-- Analyze index usage
ANALYZE product;
ANALYZE users;
ANALYZE orders;
```

## 📚 API Documentation

### Authentication
All endpoints require authentication:
```bash
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     http://localhost:8080/api/products
```

### Rate Limiting
- **Standard**: 100 requests per minute
- **Premium**: 500 requests per minute
- **Admin**: 1000 requests per minute

### Response Format
```json
{
  "status": true,
  "message": "Operation completed successfully",
  "data": { ... },
  "statusCode": 200,
  "timestamp": "2026-02-23T16:30:00.000Z"
}
```

## 🔧 Maintenance and Troubleshooting

### Cache Issues
```bash
# Clear specific cache
curl -X DELETE http://localhost:8080/api/cache/products

# Clear all caches
curl -X DELETE http://localhost:8080/api/cache/all

# Check cache health
curl http://localhost:8080/api/cache/health
```

### Performance Monitoring
```bash
# Get cache statistics
curl http://localhost:8080/api/cache/statistics

# Get performance report
curl http://localhost:8080/api/cache/performance
```

### Database Issues
```sql
-- Check slow queries
SELECT query, calls, mean_time, rows 
FROM pg_stat_statements 
WHERE calls > 100 
ORDER BY mean_time DESC 
LIMIT 10;

-- Check index usage
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read
FROM pg_stat_user_indexes 
ORDER BY idx_scan DESC;
```

---

## 🎯 Quick Start Guide

### 1. Clone and Setup
```bash
git clone https://github.com/your-org/smart-ecommerce-jpa.git
cd smart-ecommerce-jpa
```

### 2. Configure Database
```bash
# Create PostgreSQL database
createdb ecommerce

# Update application.properties
cp src/main/resources/application-example.properties \
   src/main/resources/application.properties
```

### 3. Run Application
```bash
# Development
./mvn spring-boot:run

# Production
./mvn clean package
java -jar target/smart-ecommerce-jpa.jar
```

### 4. Verify Setup
```bash
# Check health
curl http://localhost:8080/actuator/health

# Check cache status
curl http://localhost:8080/api/cache/health

# Test API
curl http://localhost:8080/api/products
```

---

**Contributors**: Please follow the [Contributing Guidelines](CONTRIBUTING.md) for code style and pull request process.

**License**: This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

**Support**: For questions and support, please open an issue on GitHub.
