# Contributing to Smart E-Commerce JPA

Thank you for your interest in contributing to this project! This document provides guidelines and best practices for contributors.

## 📋 Table of Contents

- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Code Style Guidelines](#code-style-guidelines)
- [Repository Guidelines](#repository-guidelines)
- [Transaction Guidelines](#transaction-guidelines)
- [Caching Guidelines](#caching-guidelines)
- [Testing Guidelines](#testing-guidelines)
- [Documentation Guidelines](#documentation-guidelines)
- [Pull Request Process](#pull-request-process)

## 🚀 Getting Started

### Prerequisites
- **Java 17+** with understanding of Spring Boot
- **PostgreSQL 15+** for local development
- **Maven 3.6+** for build management
- **Git** for version control
- **IDE**: IntelliJ IDEA or VS Code recommended

### Setup Instructions
```bash
# 1. Fork the repository
git clone https://github.com/your-username/smart-ecommerce-jpa.git

# 2. Create a feature branch
git checkout -b feature/your-feature-name

# 3. Set up development environment
cp src/main/resources/application-example.properties \
   src/main/resources/application.properties

# 4. Start the application
./mvn spring-boot:run
```

## 🔄 Development Workflow

### Branch Strategy
```
main                    ← Production-ready code
├── develop               ← Integration testing
├── feature/*             ← New features
├── hotfix/*              ← Bug fixes
└── release/*              ← Release preparation
```

### Commit Guidelines
- **Feature branches**: `feature/user-profile-enhancement`
- **Bug fixes**: `hotfix/order-validation-issue`
- **Refactoring**: `refactor/cache-optimization`
- **Documentation**: `docs/api-documentation-update`

### Commit Message Format
```
type(scope): brief description

Detailed explanation of changes and motivation.

Fixes #123
Closes #456
```

Examples:
```
feat(products): add product search with filters

- Add search by name, category, price range
- Implement pagination with sorting
- Add cache invalidation strategies

Fixes #123

feat(users): implement user profile caching

- Cache user profiles for 30 minutes
- Add cache warming on user updates
- Implement cache statistics monitoring

Closes #456
```

## 📝 Code Style Guidelines

### Java Code Style

#### Naming Conventions
```java
// Classes: PascalCase
public class ProductServiceImpl implements ProductService

// Methods: camelCase
public ProductResponse getProductById(UUID productId)

// Constants: UPPER_SNAKE_CASE
public static final String DEFAULT_PAGE_SIZE = "10";

// Variables: camelCase
private final ProductRepository productRepository;

// Packages: lowercase with dots
package com.miracle.smart_ecommerce_jpa.domain.product.service;
```

#### Method Organization
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    // 1. Public interface methods first
    @Override
    @CustomTransactional
    @Cacheable(value = CacheConfig.PRODUCTS_CACHE, key = "'id:' + #id")
    public ProductResponse getProductById(UUID id) {
        log.debug("Getting product by ID: {}", id);
    }

    // 2. Private helper methods
    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .build();
    }
}
```

#### Exception Handling
```java
// Use specific exceptions
throw new InsufficientStockException(productName, available, requested);

// Never catch generic Exception and continue
catch (SpecificException e) {
    log.error("Business logic error: {}", e.getMessage());
    throw e; // Re-throw for proper handling
}
```

#### JPA and Database
```java
// Use @Query for complex queries
@Query("SELECT p FROM Product p WHERE " +
        "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
Page<Product> search(@Param("keyword") String keyword, Pageable pageable);

// Use JOIN FETCH to prevent N+1
@Query("SELECT o FROM CustomerOrder o " +
       "LEFT JOIN FETCH o.user " +
       "LEFT JOIN FETCH o.items " +
       "WHERE o.id = :id")
Optional<CustomerOrder> findOrderWithDetails(@Param("id") UUID id);
```

### Documentation Standards

#### JavaDoc Format
```java
/**
 * Service for managing product operations.
 * 
 * <p>This service provides CRUD operations for products with caching,
 * transaction management, and performance optimization.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Product search with multiple filters</li>
 *   <li>Inventory management with stock tracking</li>
 *   <li>Price range queries with pagination</li>
 * </ul>
 * 
 * @author Miracle Adams
 * @version 1.0.0
 * @since 1.0.0
 */
@Service
public class ProductServiceImpl implements ProductService {
    
    /**
     * Retrieves a product by its unique identifier.
     * 
     * @param id the unique identifier of the product
     * @return the product response containing product details
     * @throws ResourceNotFoundException if the product is not found
     */
    @Override
    @Cacheable(value = CacheConfig.PRODUCTS_CACHE, key = "'id:' + #id")
    public ProductResponse getProductById(UUID id) {
        // Implementation
    }
}
```

#### Comment Standards
```java
// Method-level comments
/**
 * Validates product creation request and checks inventory.
 * Throws InsufficientStockException if stock is inadequate.
 */
private void validateStockAvailability(CreateProductRequest request) {
    // Implementation
}

// Inline comments for complex logic
if (product.getStockQuantity() < requestedQuantity) {
    // Check if we can backorder or need to reject
    if (product.isBackorderAllowed()) {
        // Allow backorder with notification
        notificationService.sendBackorderNotification(product, requestedQuantity);
    } else {
        // Reject order due to insufficient stock
        throw new InsufficientStockException(product.getName(), 
            product.getStockQuantity(), requestedQuantity);
    }
}
```

## 📚 Repository Guidelines

### Adding New Repositories

#### 1. Interface Definition
```java
@Repository
public interface NewEntityRepository extends JpaRepository<NewEntity, UUID> {
    
    // Always include JPA annotations
    @Entity
    @Table(name = "new_entity")
    public class NewEntity extends BaseModel {
        // Implementation
    }
}
```

#### 2. Derived Query Methods
```java
// Follow Spring Data naming conventions
Optional<NewEntity> findByFieldName(String value);
Page<NewEntity> findByFieldNameContainingIgnoreCase(String value, Pageable pageable);
List<NewEntity> findByFieldNameIn(List<String> values);
boolean existsByFieldName(String value);
long countByFieldName(String value);
long countByFieldNameAndOtherField(String value1, String value2);
```

#### 3. Custom Query Best Practices
```java
// Use named parameters for security
@Query("SELECT e FROM Entity e WHERE e.field = :value AND e.status = :status")
List<Entity> findByCustomCriteria(@Param("value") String value, @Param("status") String status);

// Use pagination for large result sets
@Query("SELECT e FROM Entity e WHERE e.category = :categoryId")
Page<Entity> findByCategoryWithPagination(@Param("categoryId") UUID categoryId, Pageable pageable);

// Use aggregation for reporting
@Query("SELECT c.name, COUNT(p) as productCount FROM Category c " +
       "LEFT JOIN Product p ON p.category = c.id " +
       "GROUP BY c.id")
List<Object[]> findCategoriesWithProductCount();
```

#### 4. Performance Considerations
```java
// Add indexes for frequently queried fields
@Table(name = "product", indexes = {
    @Index(name = "idx_product_category", columnList = "category_id"),
    @Index(name = "idx_product_name", columnList = "name"),
    @Index(name = "idx_product_price", columnList = "price")
})

// Use JOIN FETCH to prevent N+1
@Query("SELECT p FROM Product p " +
       "LEFT JOIN FETCH p.category " +
       "LEFT JOIN FETCH p.reviews " +
       "WHERE p.id = :id")
Optional<Product> findProductWithDetails(@Param("id") UUID id);

// Batch operations for performance
@Modifying
@Query("UPDATE Product p SET p.stock_quantity = p.stock_quantity - :quantity WHERE p.id = :id")
void updateStock(@Param("id") UUID id, @Param("quantity") int quantity);
```

## 🔄 Transaction Guidelines

### Transaction Boundaries

#### 1. Service Layer Transactions
```java
@Service
public class OrderServiceImpl implements OrderService {

    // All public methods should be transactional
    @Override
    @CustomTransactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        // Transaction starts here
        // All operations within this method are atomic
    }
    
    // Private helper methods don't need @Transactional
    private void updateInventory(List<OrderItem> items) {
        // Inherits transaction from calling method
    }
}
```

#### 2. Transaction Propagation
```java
// Default: REQUIRED (join existing or create new)
@CustomTransactional
public void defaultOperation() {
    // Standard business operations
}

// New transaction: REQUIRES_NEW (always create new)
@CustomTransactional(propagation = Propagation.REQUIRES_NEW)
public void auditOperation() {
    // Runs in separate transaction
}

// Read-only: SUPPORTS (use existing if available)
@CustomTransactional(readOnly = true, propagation = Propagation.SUPPORTS)
public List<ProductResponse> searchProducts(String keyword) {
    // Read-only operation
}
```

#### 3. Rollback Strategies
```java
@CustomTransactional(rollbackFor = {BusinessException.class})
public void businessOperation() {
    try {
        // Business logic
        riskyOperation();
    } catch (BusinessException e) {
        // Rollback occurs automatically
        log.error("Business operation failed: {}", e.getMessage());
        throw e;
    }
}

// Manual rollback control
@CustomTransactional
public void conditionalOperation() {
    if (shouldRollback) {
        // Set transaction for rollback only
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        return;
    }
    
    // Continue with operation
    performOperation();
}
```

## 🚀 Caching Guidelines

### Cache Key Strategies

#### 1. Single Entity Caching
```java
@Cacheable(value = CacheConfig.PRODUCTS_CACHE, key = "'id:' + #id")
public ProductResponse getProductById(UUID id) {
    // Simple key for single entity
}
```

#### 2. Collection Caching
```java
@Cacheable(value = CacheConfig.PRODUCTS_CACHE, key = "'category:' + #categoryId + ':page:' + #pageable.pageNumber")
public PageResponse<ProductResponse> getProductsByCategory(UUID categoryId, Pageable pageable) {
    // Cache paginated results
}
```

#### 3. Composite Key Caching
```java
@Cacheable(value = CacheConfig.ORDERS_CACHE, key = "'user:' + #userId + ':status:' + #status + ':page:' + #pageable.pageNumber")
public PageResponse<OrderResponse> getOrdersByUserAndStatus(UUID userId, String status, Pageable pageable) {
    // Complex key for multi-dimensional caching
}
```

### Cache Eviction Patterns

#### 1. Update Operations
```java
@CachePut(value = CacheConfig.USERS_CACHE, key = "'id:' + #result.id")
@CacheEvict(value = CacheConfig.USERS_CACHE, key = "'profile:*'")
public UserResponse updateUser(UUID id, UpdateUserRequest request) {
    // Update specific entry, evict profile lists
}
```

#### 2. Delete Operations
```java
@CacheEvict(value = CacheConfig.PRODUCTS_CACHE, allEntries = true)
public void deleteProduct(UUID id) {
    // Clear all product cache entries
}

@CacheEvict(value = {CacheConfig.PRODUCTS_CACHE, CacheConfig.CATEGORIES_CACHE})
public ProductResponse createProduct(CreateProductRequest request) {
    // Clear related caches
}
```

#### 3. Conditional Eviction
```java
@CacheEvict(value = CacheConfig.PRODUCTS_CACHE, condition = "#result.isActive == false")
public ProductResponse deactivateProduct(UUID id) {
    // Only evict if product becomes inactive
}
```

### Cache Performance Monitoring

#### Statistics Collection
```java
@Component
public class CacheMonitor {
    
    public void recordCacheHit(String cacheName, String key) {
        // Record hit for monitoring
        meterRegistry.counter("cache.hits", "cache", cacheName).increment();
    }
    
    public void recordCacheMiss(String cacheName, String key) {
        // Record miss for monitoring
        meterRegistry.counter("cache.misses", "cache", cacheName).increment();
    }
}
```

## 🧪 Testing Guidelines

### Unit Testing Structure
```java
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {
    
    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private CacheManager cacheManager;
    
    @InjectMocks
    private ProductServiceImpl productService;
    
    @Test
    @DisplayName("Should return product when found")
    void shouldReturnProductWhenFound() {
        // Given
        UUID productId = UUID.randomUUID();
        Product product = createTestProduct(productId);
        
        // When
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        
        // Then
        ProductResponse result = productService.getProductById(productId);
        
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(productId);
        
        // Verify cache interaction
        verify(cacheManager).getCache(CacheConfig.PRODUCTS_CACHE);
    }
}
```

### Integration Testing Structure
```java
@SpringBootTest
@TestMethodOrder(Ordered.Ordered.HIGHEST_PRECEDENCE)
@Transactional
class ProductIntegrationTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Test
    @DisplayName("Should create and retrieve product")
    void shouldCreateAndRetrieveProduct() {
        // Given
        CreateProductRequest request = createValidProductRequest();
        
        // When
        ProductResponse created = productService.createProduct(request);
        
        // Then
        entityManager.flush();
        entityManager.clear();
        
        Product found = entityManager.find(Product.class, created.getId());
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo(request.getName());
    }
}
```

### Performance Testing
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.cache.type=caffeine",
    "spring.cache.caffeine.spec=maximum-size=100,expire-after-write=1m"
})
class CachePerformanceTest {
    
    @Test
    @DisplayName("Should achieve 80% cache hit rate")
    void shouldAchieveCacheHitRate() {
        // Warm up cache
        productService.warmUpCache();
        
        // Perform operations
        for (int i = 0; i < 100; i++) {
            productService.getProductById(testProductId);
        }
        
        // Verify cache statistics
        CacheStats stats = cacheManager.getCache(CacheConfig.PRODUCTS_CACHE).getStats();
        assertThat(stats.hitRate()).isGreaterThan(0.8);
    }
}
```

## 📚 Documentation Guidelines

### README Updates
When adding new features:
1. Update the [Features](#features) section
2. Update the [Quick Start](#quick-start-guide) section
3. Add new API endpoints to [API Documentation](#api-documentation)
4. Update configuration examples

### Code Documentation
- Document all public APIs with examples
- Include request/response formats
- Document authentication requirements
- Include error scenarios

### Changelog Maintenance
```markdown
# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Added
- New feature description
- Another new feature

### Changed
- Updated existing feature

### Fixed
- Bug fix description

### Security
- Security improvement

## [1.0.0] - 2026-02-23

### Added
- Initial e-commerce platform
- Product management with caching
- User management with profiles
- Order processing with transactions
- Advanced caching strategies
```

## 🔄 Pull Request Process

### 1. Preparation
```bash
# Ensure your fork is up to date
git checkout main
git pull upstream main
git checkout -b feature/your-feature-name

# Run tests
./mvn clean test
```

### 2. Pull Request Requirements
- [ ] All tests pass
- [ ] Code follows style guidelines
- [ ] Documentation updated
- [ ] No merge conflicts
- [ ] Performance impact assessed

### 3. Pull Request Template
```markdown
## Description
Brief description of the change and why it's needed.

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review of the code completed
- [ ] Documentation updated accordingly
- [ ] No breaking changes (or documented)
- [ ] Performance impact considered

## Additional Notes
Any additional context or considerations for reviewers.

Closes #123
```

### 4. Review Process
#### Code Review Checklist
- [ ] Business logic is correct
- [ ] Transaction boundaries are appropriate
- [ ] Caching strategy is optimal
- [ ] Error handling is comprehensive
- [ ] Performance implications considered
- [ ] Security implications assessed

#### Approval Process
1. Create pull request
2. Request review from team members
3. Address feedback promptly
4. Update based on review comments
5. Merge to develop branch
6. Delete feature branch

## 🐛 Issue Reporting

### Bug Reports
When reporting bugs, please include:
- **Environment**: Java version, Spring Boot version, PostgreSQL version
- **Steps to reproduce**: Detailed reproduction steps
- **Expected behavior**: What should happen
- **Actual behavior**: What actually happens
- **Stack trace**: Full error logs
- **Possible solutions**: Any debugging attempts

### Feature Requests
When requesting features, please include:
- **Problem statement**: Current limitation or pain point
- **Proposed solution**: How you envision the feature
- **Use cases**: Specific scenarios where feature would be used
- **Acceptance criteria**: Definition of done
- **Priority**: High/Medium/Low

## 📞 Getting Help

### Discord Community
Join our Discord community for real-time discussions:
- **Code reviews**: Get feedback on your contributions
- **Questions**: Quick help from maintainers
- **Showcase**: Share your work and get feedback

### GitHub Issues
- Search existing issues before creating new ones
- Use appropriate labels: `bug`, `enhancement`, `documentation`
- Provide detailed reproduction steps
- Include environment information

---

Thank you for contributing to Smart E-Commerce JPA! Your contributions help make this project better for everyone. 🚀
