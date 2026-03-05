# Security Implementation — What Was Done & Why

> **Project:** Smart E-Commerce Security  
> **Date:** March 3, 2026  
> **Stack:** Spring Boot 3.3.5 · Spring Security 6 · Java 21 · JJWT 0.12.6 · OAuth2 (Google) · PostgreSQL  
> **Result:** ✅ Clean compile · 25 unit tests passing · BUILD SUCCESS

---

## Table of Contents

1. [Problem Statement](#1-problem-statement)
2. [What Was Deleted (and Why)](#2-what-was-deleted-and-why)
3. [What Was Created (and Why)](#3-what-was-created-and-why)
4. [What Was Modified (and Why)](#4-what-was-modified-and-why)
5. [Database Changes](#5-database-changes)
6. [Epic-by-Epic Mapping](#6-epic-by-epic-mapping)
7. [DSA Concepts Applied](#7-dsa-concepts-applied)
8. [How To Test Everything](#8-how-to-test-everything)

---

## 1. Problem Statement

The project had a **working but messy** security layer. There were multiple deprecated files still in the codebase (`@Deprecated` annotations, `@Component` removed but file still present), duplicate configurations, and a custom AOP-based authorization system (`@RequireRoles` + `AuthorizationAspect`) that was **no longer functional** because:

- The `AuthorizationAspect` had its `@Component` removed, so it was never loaded by Spring.
- It read roles from `MDC` (logging context), which is fragile and not how Spring Security works.
- Meanwhile, `@PreAuthorize` was already being used on REST controllers but **not** on GraphQL resolvers — meaning **GraphQL mutations had zero role enforcement**.

The old `ConsolidatedAuthFilter` used custom `X-Auth-Token` / `X-User-Id` headers instead of the standard `Authorization: Bearer` pattern. The `SimpleTokenService` generated UUID-based tokens instead of proper JWTs. The `AuthServiceImpl` bypassed Spring Security's `AuthenticationManager` entirely.

**Goal:** Remove all legacy code. Implement a clean, professional Spring Security architecture using JWT + OAuth2 + `@PreAuthorize` across the entire application (REST + GraphQL), add CSRF demonstration, add admin audit reporting, and write tests.

---

## 2. What Was Deleted (and Why)

### 2.1 `ConsolidatedAuthFilter.java`
**Path:** `domain/auth/filter/ConsolidatedAuthFilter.java`  
**Why deleted:**
- Already marked `@Deprecated` with `@Component` removed (not active).
- Used custom `X-Auth-Token` and `X-User-Id` headers — non-standard.
- Set authentication via `MDC.put("userId", ...)` instead of `SecurityContextHolder`.
- **Replaced by:** `JwtAuthenticationFilter.java` which extracts `Authorization: Bearer <jwt>`, validates via `JwtTokenService`, and populates `SecurityContextHolder` with `ROLE_<role>` authorities — the standard Spring Security way.

### 2.2 `SimpleTokenService.java`
**Path:** `domain/auth/service/impl/SimpleTokenService.java`  
**Why deleted:**
- Already marked `@Deprecated` but still had `@Service` — creating a competing bean with `JwtTokenService` (which needed `@Primary` to win).
- Generated tokens as `userId:role:UUID` — not a real JWT, no expiry, no cryptographic signature.
- **Replaced by:** `JwtTokenService.java` which generates proper JWTs signed with HMAC-SHA256, with claims (`sub`, `role`, `iat`, `exp`, `jti`) and blacklist support.

### 2.3 `AuthorizationAspect.java`
**Path:** `aspects/AuthorizationAspect.java`  
**Why deleted:**
- Already marked `@Deprecated` with `@Component` commented out (inactive).
- Read roles from `MDC.get("userRole")` — the logging context, not the security context. This is unreliable and breaks if any filter clears MDC.
- Used custom `@RequireRoles` annotation instead of Spring Security's standard `@PreAuthorize`.
- **Replaced by:** Spring Security's `@PreAuthorize("hasRole('ADMIN')")` / `@PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")` annotations on every controller and GraphQL resolver. These read from `SecurityContextHolder`, which is the authoritative source populated by `JwtAuthenticationFilter`.

### 2.4 `AuthService.java` + `AuthServiceImpl.java`
**Path:** `domain/auth/service/AuthService.java`, `domain/auth/service/impl/AuthServiceImpl.java`  
**Why deleted:**
- `AuthServiceImpl.authenticate()` manually called `userRepository.findByEmailAddress()` and `passwordEncoder.matches()` — completely bypassing Spring Security's `AuthenticationManager`.
- This meant Spring Security events (`AuthenticationSuccessEvent`, `AuthenticationFailureBadCredentialsEvent`) were **never fired**, so `SecurityEventListener` never captured login attempts.
- No references to this interface existed anywhere else in the codebase (confirmed via grep).
- **Replaced by:** `AuthController` now calls `authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password))`, which delegates to `DaoAuthenticationProvider` → `CustomUserDetailsService` → `BCryptPasswordEncoder`. This fires proper Spring Security events.

### 2.5 `ApiCorsConfig.java`
**Path:** `config/ApiCorsConfig.java`  
**Why deleted:**
- Defined CORS via `WebMvcConfigurer.addCorsMappings()` — but CORS was **already configured** in `SecurityConfig.corsConfigurationSource()`.
- Having two CORS configurations is confusing and can cause conflicts (Spring Security's CORS config takes precedence for secured endpoints, but the `WebMvcConfigurer` one applies to unsecured endpoints, leading to inconsistent behavior).
- **Consolidated into:** `SecurityConfig.corsConfigurationSource()` — a single source of truth for CORS policy.

---

## 3. What Was Created (and Why)

### 3.1 `CsrfDemoController.java`
**Path:** `domain/auth/controller/CsrfDemoController.java`  
**Why created:**
- **Requirement (Epic 3, Story 3.1):** "CSRF token mechanism demonstrated in one form endpoint for illustration."
- The REST API uses stateless JWT (CSRF disabled) — but the project needed to **demonstrate understanding** of when and why CSRF matters.
- This `@Controller` (not `@RestController`) serves a Thymeleaf HTML form at `/csrf-demo`.
- `GET /csrf-demo` renders the form with the CSRF token visible on screen.
- `POST /csrf-demo` processes the form — Spring Security validates the `_csrf` hidden field automatically. If tampered → 403 Forbidden.
- Uses `@Hidden` to exclude from Swagger (it's a UI demo, not an API).

### 3.2 `csrf-demo.html`
**Path:** `src/main/resources/templates/csrf-demo.html`  
**Why created:**
- The Thymeleaf template for the CSRF demo controller.
- Shows: CSRF vs CORS comparison table, live CSRF token value, a working form with `th:action` (auto-injects `_csrf`), and explanation of `SecurityConfig` CSRF settings.
- **Practical demonstration** — visit `http://localhost:8080/csrf-demo` in a browser, open DevTools, submit the form, inspect the `_csrf` parameter in the POST request.

### 3.3 `SecurityReportService.java`
**Path:** `domain/auth/service/SecurityReportService.java`  
**Why created:**
- **Requirement (Epic 5, Story 5.2):** "Security reports include token usage and endpoint access frequency. Logs reviewed to detect unusual access or brute-force attempts."
- Aggregates data from three sources:
  - `SecurityEventListener` — auth success/failure/denied counters + recent events
  - `TokenBlacklistService` — count of revoked tokens
  - `TokenActivityService` — active token count + total validations
- Returns a `SecurityReport` record containing `AuthStats`, `TokenStats`, and `List<SecurityEvent>`.
- All data sources use thread-safe structures (`AtomicLong`, `ConcurrentHashMap`, `ConcurrentLinkedDeque`) — no locking needed.

### 3.4 `SecurityReportController.java`
**Path:** `domain/auth/controller/SecurityReportController.java`  
**Why created:**
- **Requirement (Epic 5, Story 5.2):** An admin endpoint to view security audit reports.
- `GET /api/admin/security-report` — returns the full security report as JSON.
- **Defense-in-depth:** Protected by BOTH:
  1. URL-level: `SecurityConfig` → `.requestMatchers("/api/admin/**").hasRole("ADMIN")`
  2. Method-level: `@PreAuthorize("hasRole('ADMIN')")` on the class
- Even if one layer is misconfigured, the other blocks unauthorized access.

### 3.5 `JwtTokenServiceTest.java`
**Path:** `src/test/java/.../domain/auth/service/JwtTokenServiceTest.java`  
**Why created:**
- **Requirement (Epic 2):** Verify JWT generation, validation, expiry, tampering, and blacklisting.
- 11 tests covering:
  - Token generation: valid JWT structure, correct claims (sub, role)
  - Validation: success, null, blank, tampered signature, expired, blacklisted, wrong signing key
  - JTI extraction: valid token, invalid token
- Uses Mockito to mock `TokenBlacklistService` and `TokenActivityService`.

### 3.6 `TokenBlacklistServiceTest.java`
**Path:** `src/test/java/.../domain/auth/service/TokenBlacklistServiceTest.java`  
**Why created:**
- **Requirement (Epic 5, Story 5.1):** Verify the HashMap-based blacklist works correctly.
- 6 tests covering: blacklist + check, unknown JTI, multiple tokens, purge expired, purge no-op, empty size.

### 3.7 `SecurityEventListenerTest.java`
**Path:** `src/test/java/.../domain/auth/service/SecurityEventListenerTest.java`  
**Why created:**
- **Requirement (Epic 5, Story 5.2):** Verify security event tracking.
- 6 tests covering: success counter, failure counter, newest-first ordering, bounded buffer (100 cap), unmodifiable list, initial state.

### 3.8 `SecurityReportServiceTest.java`
**Path:** `src/test/java/.../domain/auth/service/SecurityReportServiceTest.java`  
**Why created:**
- Verifies the report aggregation service correctly combines data from all three sources.
- 2 tests: full aggregation, recent events inclusion.

---

## 4. What Was Modified (and Why)

### 4.1 `SecurityConfig.java`
**Path:** `config/SecurityConfig.java`  
**What changed:**
- **CSRF:** Changed from `.csrf(AbstractHttpConfigurer::disable)` (blanket disable) to:
  ```java
  .csrf(csrf -> csrf
      .ignoringRequestMatchers("/api/**", "/graphql")
      .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
  )
  ```
  **Why:** CSRF is correctly disabled for stateless JWT endpoints but **enabled** for the `/csrf-demo` form path. The `CookieCsrfTokenRepository` stores the token in a cookie for SPA compatibility.

- **Authorization rules:** Added:
  - `.requestMatchers("/csrf-demo/**").permitAll()` — CSRF demo is public
  - `.requestMatchers("/api/admin/**").hasRole("ADMIN")` — admin endpoints require ADMIN role at URL level
  
- **Exception handling:** Added custom `authenticationEntryPoint` (401 JSON) and `accessDeniedHandler` (403 JSON) so API clients get structured error responses instead of HTML redirect pages.

- **Javadoc:** Added comprehensive documentation explaining every architecture decision (CSRF, CORS, sessions, method security, JWT filter, OAuth2, password hashing, roles).

### 4.2 `SecurityEventListener.java`
**Path:** `config/SecurityEventListener.java`  
**What changed:**
- **Before:** Only had `@EventListener` methods that logged to SLF4J. No counters, no event storage.
- **After:** Added:
  - `AtomicLong successCount`, `failureCount`, `deniedCount` — lock-free thread-safe counters
  - `ConcurrentLinkedDeque<SecurityEvent> recentEvents` — bounded at 100 entries, newest-first
  - Public getters: `getSuccessCount()`, `getFailureCount()`, `getDeniedCount()`, `getRecentEvents()`
  - Inner types: `EventType` enum, `SecurityEvent` record

**Why:** The `SecurityReportService` needs to read these metrics. The `AtomicLong` counters provide O(1) lock-free increment. The `ConcurrentLinkedDeque` provides O(1) addFirst/removeLast with a bounded cap to prevent unbounded memory growth. These are **DSA concepts** required by Epic 5.

### 4.3 `JwtTokenService.java`
**Path:** `domain/auth/service/impl/JwtTokenService.java`  
**What changed:**
- Removed `@Primary` annotation — no longer needed since `SimpleTokenService` (the competing `TokenService` bean) was deleted.
- Removed `import org.springframework.context.annotation.Primary`.
- Enhanced Javadoc with `@see` references and DSA concept callouts.

**Why:** `@Primary` was only necessary because two `TokenService` implementations existed. With `SimpleTokenService` deleted, there's only one implementation — Spring auto-wires it without ambiguity.

### 4.4 `RequireRoles.java`
**Path:** `annotation/RequireRoles.java`  
**What changed:** Added `@Deprecated` annotation and Javadoc explaining it's replaced by `@PreAuthorize`.

**Why:** The annotation is no longer used by any code (all resolvers migrated to `@PreAuthorize`), but keeping it with `@Deprecated` provides a reference trail for anyone reading the git history.

### 4.5 All 7 GraphQL Resolvers
**Files changed:**
- `graphql/resolver/CartResolver.java`
- `graphql/resolver/CategoryResolver.java`
- `graphql/resolver/OrderResolver.java`
- `graphql/resolver/ProductResolver.java`
- `graphql/resolver/UserResolver.java`
- `graphql/resolver/AddressResolver.java`
- `graphql/resolver/ReviewResolver.java`

**What changed:** Every `@RequireRoles({"ADMIN", "CUSTOMER"})` annotation was replaced with `@PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")`.

**Why this was critical:**
1. The `AuthorizationAspect` that enforced `@RequireRoles` had its `@Component` removed — it was **never loaded by Spring**. This meant **every GraphQL mutation was unprotected**. Any unauthenticated request could create products, delete users, place orders, etc.
2. `@PreAuthorize` is enforced by Spring Security's method security proxy (enabled by `@EnableMethodSecurity` in `SecurityConfig`). It reads authorities from `SecurityContextHolder`, which is populated by `JwtAuthenticationFilter`. This is the correct, standard approach.
3. Class-level `@PreAuthorize` provides a default for all methods. Method-level overrides (e.g., `@PreAuthorize("hasRole('ADMIN')")` on mutation methods) provide fine-grained control.

**Migration details per resolver:**

| Resolver | Class-level | Method-level overrides |
|----------|-------------|------------------------|
| `CartResolver` | `hasAnyRole('ADMIN', 'CUSTOMER')` | `clearCart` → `hasRole('CUSTOMER')` |
| `CategoryResolver` | `hasAnyRole('ADMIN', 'CUSTOMER')` | All mutations → `hasRole('ADMIN')` |
| `OrderResolver` | `hasAnyRole('ADMIN', 'CUSTOMER')` | `orders` (list all) → `hasRole('ADMIN')`, `deleteOrder` → `hasRole('ADMIN')`, `cancelOrder` / `updatePaymentStatus` → `hasRole('CUSTOMER')` |
| `ProductResolver` | `hasAnyRole('ADMIN', 'CUSTOMER')` | All mutations → `hasRole('ADMIN')` |
| `UserResolver` | None (method-level only) | Queries split: `user()` → ADMIN+CUSTOMER, `userByEmail`/`users`/`searchUsers` → ADMIN. Mutations: `createUser`/`updateUser` → ADMIN+CUSTOMER, `deleteUser`/`activateUser`/`deactivateUser` → ADMIN |
| `AddressResolver` | `hasAnyRole('ADMIN', 'CUSTOMER')` | None (all operations available to both roles) |
| `ReviewResolver` | `hasAnyRole('ADMIN', 'CUSTOMER')` | None |

### 4.6 `README.md`
**What changed:** Added three new sections at the top:
- **Security Architecture** — overview of JWT, OAuth2, BCrypt, RBAC, token revocation, and security audit
- **CORS vs CSRF** — explains what each is, when to enable/disable, comparison table, this project's configuration, and how to access the CSRF demo

**Why:** Requirement (Epic 3, Story 3.2): "Technical documentation describing CORS and CSRF interaction included in README."

### 4.7 `AUTH_ARCHITECTURE.md`
**Path:** `docs/AUTH_ARCHITECTURE.md`  
**What changed:** Complete rewrite. The old version documented the `ConsolidatedAuthFilter` → `MDC` → `AuthorizationAspect` flow that no longer exists.

**New content covers:**
- Component table (what technology does what)
- Every security component with file path and responsibility
- Request flow diagrams for JWT auth, login, and Google OAuth2
- RBAC roles table and endpoint security matrix
- CSRF and CORS configuration explanation
- DSA concepts table (where each concept is applied)
- Mermaid sequence diagram
- Files overview table
- Security hardening notes

**Why:** The documentation must match the actual implementation. The old doc would confuse anyone trying to understand the system.

---

## 5. Database Changes

The database schema is managed by **Flyway** — a migration tool that applies versioned SQL scripts in order on startup. There are two migration files relevant to security.

### V1__Initial_schema.sql — The Baseline
**Path:** `src/main/resources/db/migration/V1__Initial_schema.sql`

This is the original schema. The `app_user` table was created with:

```sql
CREATE TABLE app_user (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email_address  VARCHAR(255) NOT NULL UNIQUE,
    first_name     VARCHAR(100),
    last_name      VARCHAR(100),
    phone_number   VARCHAR(20),
    password_hash  VARCHAR(255) NOT NULL,   -- ← was NOT NULL (no OAuth2 support)
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    role           VARCHAR(50) NOT NULL DEFAULT 'CUSTOMER',
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP WITH TIME ZONE
);
```

**Security-relevant indexes already in V1:**
| Index | Column(s) | Purpose |
|-------|-----------|---------|
| `idx_user_email` | `email_address` | Fast user lookup during login |
| `idx_user_role` | `role` | Fast RBAC role filtering |
| `idx_user_email_active` | `email_address, is_active` | Auth filter user lookup — checks active status at the same time |

**The problem with V1 for OAuth2:** `password_hash VARCHAR(255) NOT NULL` — this constraint prevents storing Google OAuth2 users who have no local password. This was fixed in V2.

---

### V2__Add_oauth_provider_columns.sql — The Security Migration
**Path:** `src/main/resources/db/migration/V2__Add_oauth_provider_columns.sql`

This migration makes **4 changes** to the database, all required to support Google OAuth2 login and RBAC testing.

```sql
-- Change 1: Add OAuth2 provider name column (e.g., "google")
ALTER TABLE app_user ADD COLUMN oauth_provider VARCHAR(50);

-- Change 2: Add OAuth2 provider's unique user ID (Google's "sub" claim)
ALTER TABLE app_user ADD COLUMN oauth_provider_id VARCHAR(255);

-- Change 3: Allow password_hash to be NULL for OAuth2 users
ALTER TABLE app_user ALTER COLUMN password_hash DROP NOT NULL;

-- Change 4: Composite index for fast OAuth2 user lookup
CREATE INDEX idx_user_oauth ON app_user(oauth_provider, oauth_provider_id);

-- Change 5: Seed test users (ADMIN and CUSTOMER) — see V3__Seed_test_users.sql
```

#### Why Each Change Was Made

**Change 1 & 2 — `oauth_provider` and `oauth_provider_id` columns:**
- When a user logs in with Google, `CustomOAuth2UserService` receives the user's Google profile.
- It looks them up using `userRepository.findByOauthProviderAndOauthProviderId("google", sub)`.
- These two columns store which provider authenticated the user (`"google"`) and what their unique ID is at that provider (Google's `sub` claim from the OAuth2 token, e.g., `"118400307685904897001"`).
- This allows one user account to be linked to multiple OAuth2 providers in the future (extensible design).

**Change 3 — `DROP NOT NULL` on `password_hash`:**
- V1 required every user to have a password hash.
- OAuth2 users authenticate through Google — they never set a local password, so there is no hash to store.
- Making `password_hash` nullable allows these users to be persisted without a password.
- In `CustomUserDetailsService`, the code handles this: `user.getPasswordHash() != null ? user.getPasswordHash() : ""` — it returns an empty string so Spring Security doesn't crash.

**Change 4 — `idx_user_oauth` composite index:**
- Every Google login triggers `findByOauthProviderAndOauthProviderId(provider, providerId)` — a two-column lookup.
- Without an index this is a full table scan — O(n).
- The composite index makes this O(log n) and is essential for performance as the user table grows.
- The index covers both columns together because queries always filter on both simultaneously.

**Change 5 — Seed users (moved to V3):**
- Two roles exist: `ADMIN` and `CUSTOMER`.
- Test users are seeded via `V3__Seed_test_users.sql`: `admin@smartecommerce.com` (ADMIN), `john.doe@example.com` and `jane.smith@example.com` (CUSTOMER). All use password `password123`.
- `ON CONFLICT (email_address) DO NOTHING` makes the insert idempotent — safe to run repeatedly.

---

### What the `app_user` Table Looks Like After Both Migrations

```sql
CREATE TABLE app_user (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email_address    VARCHAR(255) NOT NULL UNIQUE,
    first_name       VARCHAR(100),
    last_name        VARCHAR(100),
    phone_number     VARCHAR(20),
    password_hash    VARCHAR(255),          -- ← now NULLABLE (OAuth2 users have no password)
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    role             VARCHAR(50) NOT NULL DEFAULT 'CUSTOMER',
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE,
    oauth_provider   VARCHAR(50),           -- ← NEW: "google" or NULL
    oauth_provider_id VARCHAR(255)          -- ← NEW: Google's "sub" or NULL
);
```

**Indexes on `app_user` after both migrations:**
| Index | Column(s) | Type | Used By |
|-------|-----------|------|---------|
| `idx_user_email` | `email_address` | Single | Login lookup in `CustomUserDetailsService` |
| `idx_user_role` | `role` | Single | RBAC role filtering queries |
| `idx_user_email_active` | `email_address, is_active` | Composite | Auth filter — checks active status with lookup |
| `idx_user_oauth` | `oauth_provider, oauth_provider_id` | Composite | Google OAuth2 login lookup in `CustomOAuth2UserService` |

---

### How Flyway Applies These Changes

On application startup, Flyway:
1. Checks the `flyway_schema_history` table in the database.
2. Finds which migrations have already been applied.
3. Runs any new migrations in version order (V1 → V2 → ...).
4. If V1 is already applied and V2 is not, only V2 runs.
5. If both are already applied, nothing runs (idempotent).

This means the schema evolves safely across environments (dev, test, prod) without manual SQL execution.

---

## 6. Epic-by-Epic Mapping

### Epic 1: Security Configuration and Access Policies

| Acceptance Criteria | How It's Met | File(s) |
|---|---|---|
| SecurityFilterChain configured with custom access rules | `securityFilterChain()` bean with URL matchers, JWT filter, OAuth2, exception handlers | `SecurityConfig.java` |
| Public and restricted endpoints defined | `permitAll()` for auth/products/categories/swagger/graphql; `hasRole('ADMIN')` for admin; `authenticated()` for rest | `SecurityConfig.java` |
| Passwords stored using BCryptPasswordEncoder | `PasswordConfig` exposes `BCryptPasswordEncoder` bean; `DaoAuthenticationProvider` uses it | `PasswordConfig.java`, `SecurityConfig.java` |

### Epic 2: JWT-Based Authentication

| Acceptance Criteria | How It's Met | File(s) |
|---|---|---|
| `/api/auth/login` generates signed JWTs with claims | `AuthController.authenticate()` → `AuthenticationManager` → `JwtTokenService.generateToken()` with sub, role, iat, exp, jti | `AuthController.java`, `JwtTokenService.java` |
| Tokens validated on each protected request | `JwtAuthenticationFilter` extracts Bearer token, calls `validateToken()`, sets `SecurityContextHolder` | `JwtAuthenticationFilter.java` |
| Tampered or expired tokens rejected with 401 | `JwtTokenService.validateToken()` catches `ExpiredJwtException`, `JwtException`; filter leaves context empty → Spring returns 401 | `JwtTokenService.java`, `SecurityConfig.java` (authenticationEntryPoint) |
| JWT includes subject, issued time, expiration | `.subject(userId)`, `.issuedAt(now)`, `.expiration(expiry)`, `.id(jti)` | `JwtTokenService.java` |
| HMAC SHA-256 used for signature | `.signWith(signingKey, Jwts.SIG.HS256)` | `JwtTokenService.java` |
| Token payload viewable in Postman | Standard JWT — paste into jwt.io or Postman's built-in decoder | N/A (standard JWT format) |

### Epic 3: CSRF and Session Security

| Acceptance Criteria | How It's Met | File(s) |
|---|---|---|
| CSRF disabled for stateless JWT APIs | `.ignoringRequestMatchers("/api/**", "/graphql")` | `SecurityConfig.java` |
| When CSRF should be enabled documented | Javadoc in `CsrfDemoController`, README CORS vs CSRF section, `csrf-demo.html` explanation cards | `CsrfDemoController.java`, `README.md`, `csrf-demo.html` |
| CSRF token mechanism demonstrated | `/csrf-demo` Thymeleaf form with visible token, `th:action` auto-inject, POST validation | `CsrfDemoController.java`, `csrf-demo.html` |
| CORS and CSRF documentation in README | New "CORS vs CSRF" section with comparison table, when to enable/disable, project config | `README.md` |

### Epic 4: OAuth2 and Role-Based Access Control

| Acceptance Criteria | How It's Met | File(s) |
|---|---|---|
| OAuth2 login with Google | `CustomOAuth2UserService` + `OAuth2AuthenticationSuccessHandler` configured in `SecurityConfig` | `CustomOAuth2UserService.java`, `OAuth2AuthenticationSuccessHandler.java`, `SecurityConfig.java` |
| User details fetched and persisted | `CustomOAuth2UserService.loadUser()` fetches Google profile, creates/links User entity | `CustomOAuth2UserService.java` |
| Roles assigned after OAuth2 auth | New OAuth2 users get `CUSTOMER` role by default | `CustomOAuth2UserService.java` |
| Roles defined (ADMIN, CUSTOMER) | Used in `@PreAuthorize` across all controllers and resolvers, seeded via Flyway migration | All controllers/resolvers, `V3__Seed_test_users.sql` |
| Endpoints annotated with `@PreAuthorize` | All 7 REST controllers and 7 GraphQL resolvers have `@PreAuthorize` | All controller and resolver files |
| Role-based access verified with tests | Unit tests for SecurityEventListener verify event tracking; manual Postman tests per API docs | Test files, `API_ENDPOINTS.md` |

### Epic 5: DSA and Security Optimization

| Acceptance Criteria | How It's Met | File(s) |
|---|---|---|
| Hashing for password security | BCrypt via `BCryptPasswordEncoder` (adaptive cost factor + salt) | `PasswordConfig.java` |
| Hashing for token validation | HMAC-SHA256 for JWT signatures — O(1) verification | `JwtTokenService.java` |
| HashMap for token blacklisting | `ConcurrentHashMap<String, Instant>` — O(1) insert and lookup | `TokenBlacklistService.java` |
| Scheduled purge of expired tokens | `@Scheduled(fixedRate = 900_000)` removes entries where expiry < now | `TokenBlacklistService.java` |
| Logging for auth success/failure | `SecurityEventListener` with `@EventListener` for all Spring Security events | `SecurityEventListener.java` |
| Security reports with token usage | `GET /api/admin/security-report` returns auth stats, token metrics, recent events | `SecurityReportController.java`, `SecurityReportService.java` |
| Detect unusual access / brute-force | Failure counter + recent events buffer show repeated failed logins per principal | `SecurityEventListener.java`, `SecurityReportService.java` |

---

## 7. DSA Concepts Applied

| DSA Concept | Where | Why | Complexity |
|-------------|-------|-----|------------|
| **Hashing (HMAC-SHA256)** | `JwtTokenService` — JWT signature | Tamper-proof tokens; verifier recomputes hash and compares | O(1) sign + verify |
| **Hashing (BCrypt)** | `PasswordConfig` — password storage | Adaptive cost factor prevents rainbow tables; random salt per password | O(2^cost) intentionally slow |
| **HashMap (ConcurrentHashMap)** | `TokenBlacklistService` — revoked token JTIs | O(1) blacklist check on every request; O(1) insert on logout | O(1) avg |
| **HashMap (ConcurrentHashMap)** | `TokenActivityService` — active token tracking | O(1) update per validation; O(n) aggregation for reports | O(1) per op |
| **Deque (ConcurrentLinkedDeque)** | `SecurityEventListener` — recent events buffer | O(1) addFirst for newest; O(1) removeLast to enforce cap; bounded at 100 | O(1) per op |
| **Atomic counters (AtomicLong)** | `SecurityEventListener` — success/failure/denied counts | Lock-free thread-safe counting via CAS (compare-and-swap) | O(1) |
| **Scheduled cleanup** | `TokenBlacklistService.purgeExpired()` | Prevents unbounded HashMap growth; runs every 15 min | O(n) scan |

---

## 8. How To Test Everything

### Run All Unit Tests
```bash
./mvnw test -Dtest="JwtTokenServiceTest,TokenBlacklistServiceTest,SecurityEventListenerTest,SecurityReportServiceTest"
```
Expected: **25 tests, 0 failures, BUILD SUCCESS**

### Test JWT Login (Postman)
```
POST http://localhost:8080/api/auth/login
Body: { "email": "admin@smartecommerce.com", "password": "password123" }
→ 200 OK with { userId, role, token }
```

### Test Protected Endpoint Without Token
```
GET http://localhost:8080/api/users
→ 401 { "message": "Authentication required. Provide a valid Bearer JWT token." }
```

### Test Protected Endpoint With Token (Wrong Role)
```
GET http://localhost:8080/api/admin/security-report
Authorization: Bearer <customer-token>
→ 403 { "message": "Access denied. Insufficient role privileges." }
```

### Test Admin Security Report
```
GET http://localhost:8080/api/admin/security-report
Authorization: Bearer <admin-token>
→ 200 { authenticationStats, tokenStats, recentSecurityEvents }
```

### Test CSRF Demo
1. Open `http://localhost:8080/csrf-demo` in a browser
2. See the CSRF token displayed on screen
3. Submit the form → success message
4. Open DevTools, manually remove the `_csrf` field, resubmit → 403 Forbidden

### Test CORS Rejection
```bash
curl -H "Origin: http://evil.com" -H "Access-Control-Request-Method: GET" \
  -X OPTIONS http://localhost:8080/api/products
→ No Access-Control-Allow-Origin header → browser blocks
```

### Test Token Blacklisting (Logout)
```
POST http://localhost:8080/api/auth/logout
Authorization: Bearer <token>
→ 200 "Logged out successfully"

# Reuse the same token:
GET http://localhost:8080/api/users/me
Authorization: Bearer <same-token>
→ 401 (token is blacklisted)
```

### Test Expired Token
```
# Wait for token to expire (or set jwt.expiration-ms=1000 in test profile)
GET http://localhost:8080/api/users
Authorization: Bearer <expired-token>
→ 401
```

### Test Google OAuth2
```
1. Navigate to: http://localhost:8080/oauth2/authorization/google
2. Complete Google login
3. Receive JSON: { userId, role, token, email }
4. Use the token for subsequent API calls
```

