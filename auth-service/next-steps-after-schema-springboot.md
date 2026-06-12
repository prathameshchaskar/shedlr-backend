# Next Steps After Creating the Schema in a Spring Boot Project

This document describes the recommended next implementation steps after database schema creation for a production-ready Spring Boot project that supports authentication, RBAC, workspaces, projects, and workflow-driven business operations.[cite:1]

## Why schema is not enough

A schema gives the application a stable persistence foundation, but the product cannot function until the database structure is connected to entities, repositories, services, security rules, controllers, and tests.[cite:1] The product document also emphasizes DTO-based APIs, service-layer authorization, Flyway migration discipline, auditability, and transactional consistency, so the next steps should establish those layers in a deliberate order instead of jumping directly into feature screens or random endpoints.[cite:1]

## Recommended implementation order

The safest production sequence is to build the backend in vertical layers, starting from persistence mapping and moving upward into business logic and security.[cite:1]

| Order | Step | Why it comes next |
|---|---|---|
| 1 | Create JPA entities | Schema must be mapped into code before business logic can use it [cite:1] |
| 2 | Create repositories | Login, role lookup, token lookup, and profile reads depend on repository access [cite:1] |
| 3 | Create DTOs and validators | Controllers should use DTOs, not entities, and validate input with Bean Validation [cite:1] |
| 4 | Build service layer | Business rules, authorization checks, token lifecycle, and workflows belong here [cite:1] |
| 5 | Configure Spring Security and JWT | Protected APIs and RBAC require authentication infrastructure [cite:1] |
| 6 | Expose controllers | Controllers should stay thin and delegate work to services [cite:1] |
| 7 | Add email and token flows | Signup, verification, and password reset are incomplete without them [cite:1] |
| 8 | Write integration tests | The product requires reliability, auditability, and strong workflow integrity [cite:1] |
| 9 | Seed role and admin data | RBAC cannot be exercised meaningfully without roles and initial access setup [cite:1] |
| 10 | Move to project and change-request modules | Auth and identity should stabilize before broader workflow features [cite:1] |

## Step 1: Map schema to JPA entities

The first code layer after schema creation should be the entity model.[cite:1] Each core identity and access table should be mapped to a JPA entity so Spring Data and service logic can work with a consistent domain model.[cite:1]

### Core entities to implement first

- `Workspace`
- `UserAccount`
- `UserProfile`, if profile data is split out
- `Project`
- `RoleAssignment`
- `EmailVerificationToken`
- `PasswordResetToken`
- `UserSession`, if refresh-token or multi-device logout is planned from the beginning[cite:1]

### Important mapping rules

- Use `@ManyToOne(fetch = FetchType.LAZY)` for references like workspace, user, and project.
- Add `@Enumerated(EnumType.STRING)` for status and role columns.
- Use a shared auditable base entity for `created_at`, `updated_at`, and `version` if possible.
- Match nullable and non-nullable columns carefully to the schema.[cite:1]

## Step 2: Create repositories

Repositories should be created immediately after entities because the next service layer depends on common lookup patterns for authentication, verification, sessions, and roles.[cite:1]

### First repositories to build

- `UserAccountRepository`
- `WorkspaceRepository`
- `ProjectRepository`
- `RoleAssignmentRepository`
- `EmailVerificationTokenRepository`
- `PasswordResetTokenRepository`
- `UserSessionRepository`, if session tracking is enabled[cite:1]

### Typical repository methods

- `findByEmail(String email)`
- `existsByEmail(String email)`
- `findByTokenHash(String tokenHash)`
- `findByUserIdAndActiveTrue(Long userId)`
- `findByWorkspaceId(...)` or `findByWorkspaceCode(...)` for tenant-aware access[cite:1]

## Step 3: Create DTOs and validation contracts

The product guidance explicitly recommends using DTOs in controllers and Bean Validation for input validation, so this layer should come before controller implementation.[cite:1]

### Auth request DTOs

- `SignupRequest`
- `LoginRequest`
- `ForgotPasswordRequest`
- `ResetPasswordRequest`
- `VerifyEmailRequest`
- `ResendVerificationRequest`
- `ChangePasswordRequest`
- `UpdateProfileRequest`[cite:1]

### Auth response DTOs

- `AuthResponse`
- `UserSummaryResponse`
- `UserProfileResponse`
- `GenericMessageResponse`
- `ErrorResponse` for stable API failures[cite:1]

### Validation rules to add

- Required fields with `@NotBlank`
- Email format with `@Email`
- Length checks with `@Size`
- Terms acceptance with `@AssertTrue`
- Cross-field password matching in the service or with custom validator[cite:1]

## Step 4: Build service layer first, not controllers first

The next major layer should be services because business-critical rules belong there, not inside controllers.[cite:1] The product document specifically expects authorization in the service layer, transactional consistency, and proper handling of sensitive actions like approval or role-sensitive updates.[cite:1]

### First services to implement

| Service | Responsibility |
|---|---|
| `AuthService` | Signup, login, verify email, forgot password, reset password, logout [cite:1] |
| `JwtService` or `TokenService` | Generate and validate JWT access tokens and refresh tokens [cite:1] |
| `UserProfileService` | Profile retrieval, profile update, password change |
| `RoleService` | Resolve effective roles for workspace or project access [cite:1] |
| `EmailService` | Send verification and reset mails |
| `SessionService` | Track refresh sessions and revoke them as needed |

### Signup flow inside service layer

A production signup flow should do the following in one coherent application use case:[cite:1]

1. Normalize and validate email.[cite:1]
2. Check duplicate account rules.[cite:1]
3. Encode password using BCrypt or Argon2 as recommended in the product guidance.[cite:1]
4. Create `UserAccount` in an unverified or pending state.[cite:1]
5. Create profile and default role records if required by onboarding design.[cite:1]
6. Create email verification token.[cite:1]
7. Send verification mail after successful persistence.[cite:1]

## Step 5: Configure Spring Security and JWT

After service contracts are stable, Spring Security should be introduced so the project starts with correct protected route behavior rather than retrofitting security later.[cite:1] The product plan already expects JWT access tokens, refresh strategy, RBAC, and restricted sensitive actions, so auth infrastructure is not optional.[cite:1]

### Security components to implement

- Custom `UserDetailsService` or principal resolver
- Password encoder bean such as BCrypt or Argon2
- JWT utility/service for token generation and validation
- Authentication filter that reads bearer tokens
- Security configuration with public and protected route rules
- Authentication entry point for stable unauthorized responses[cite:1]

### Initial route categories

| Route type | Examples |
|---|---|
| Public | `/api/v1/auth/signup`, `/login`, `/verify-email`, `/forgot-password`, `/reset-password` [cite:1] |
| Authenticated | `/api/v1/users/me`, `/api/v1/users/me/change-password` [cite:1] |
| Role-protected | workspace admin, role management, project admin routes [cite:1] |

## Step 6: Create thin controllers

Only after services and security are in place should controllers be written.[cite:1] This keeps controllers simple, testable, and aligned with the product’s API design guidance that controllers should expose stable DTO-driven endpoints rather than embed domain logic.[cite:1]

### First controllers to expose

- `AuthController`
- `UserProfileController`
- `WorkspaceController`, only if initial workspace creation or selection is part of MVP[cite:1]

### Controller responsibilities

- Accept validated DTOs.
- Call the appropriate service.
- Return response DTOs.
- Convert domain errors into stable API error payloads.[cite:1]

Controllers should not encode passwords, check role rules directly, issue ad hoc SQL, or manage token business logic.[cite:1]

## Step 7: Complete email-based lifecycle flows

At this stage, the backend becomes meaningfully usable for the frontend because signup and recovery flows start working end to end.[cite:1]

### Required email flows

- Email verification after signup
- Resend verification with anti-abuse limits
- Forgot password with generic success response
- Reset password with expiry and one-time-use tokens[cite:1]

### Important implementation details

- Persist only token hashes, not raw tokens.
- Store expiry timestamps.
- Mark tokens as used when consumed.
- Reject expired, reused, or revoked tokens.
- Consider revoking active sessions after password reset.[cite:1]

## Step 8: Seed roles and bootstrap access

RBAC is difficult to validate without seed data or bootstrap onboarding rules.[cite:1] The product document explicitly suggests default roles and seed migrations, so role setup should be part of the early implementation phase rather than an afterthought.[cite:1]

### What to seed early

- Role values or role reference records, if a lookup table is used
- Initial admin user or admin invitation flow
- Default workspace data in local and test environments only, where appropriate[cite:1]

### Practical early rule

The first signed-up user in a new workspace is often assigned an admin-level workspace role so setup can continue without manual SQL intervention.[cite:1]

## Step 9: Add testing before expanding scope

Once the auth slice is implemented, automated testing should be added before moving into the broader workflow domain.[cite:1] The product emphasizes reliability, auditable writes, transactional safety, and transition correctness, all of which benefit from integration-heavy testing from the beginning.[cite:1]

### First tests to write

| Test type | What to verify |
|---|---|
| Repository tests | Email lookup, token lookup, active role queries |
| Service tests | Signup rules, duplicate email checks, token expiration logic |
| Security tests | Public vs protected endpoints, unauthorized access, role checks |
| Integration tests | Signup, verify email, login, forgot password, reset password, `/users/me` [cite:1] |
| Flyway tests | Schema starts correctly and migrations apply in order [cite:1] |

## Step 10: Only then move to project and workflow modules

The product document separates identity, change management, workflow, release tracking, notifications, and audit into bounded modules, and the auth module is the natural first completed slice.[cite:1] Once signup, login, role loading, profile management, and workspace-aware authorization are stable, the project can safely move into `Project`, `ChangeRequest`, `ApprovalWorkflow`, and release features.[cite:1]

## Suggested folder structure for the next phase

A good next-step code structure after schema creation is shown below.[cite:1]

```text
src/main/java/com/yourapp
  ├── common
  │   ├── config
  │   ├── exception
  │   └── audit
  ├── identity
  │   ├── controller
  │   ├── dto
  │   │   ├── request
  │   │   └── response
  │   ├── entity
  │   ├── enumtype
  │   ├── mapper
  │   ├── repository
  │   ├── security
  │   ├── service
  │   └── validation
  ├── workspace
  │   ├── controller
  │   ├── entity
  │   ├── repository
  │   └── service
  ├── project
  │   ├── entity
  │   ├── repository
  │   └── service
  └── change
      ├── entity
      ├── repository
      └── service
```

## Practical 7-day execution plan

This short execution plan keeps the work focused and realistic for the current stage.[cite:1]

| Day | Focus |
|---|---|
| Day 1 | Finalize entities and enums from schema |
| Day 2 | Create repositories and DTOs |
| Day 3 | Implement signup, login, and profile services |
| Day 4 | Add email verification and password reset flow |
| Day 5 | Configure Spring Security and JWT |
| Day 6 | Expose controllers and standard error handling |
| Day 7 | Write integration tests and seed initial roles/admin [cite:1] |

## Common mistakes to avoid

The next phase often goes wrong when development jumps directly to controllers, frontend integration, or random feature endpoints before the identity slice is stable.[cite:1] The most common mistakes are listed below.[cite:1]

- Returning entities directly from controllers instead of DTOs.[cite:1]
- Writing business logic in controllers.[cite:1]
- Adding role checks only in the frontend and not in the service layer.[cite:1]
- Storing raw reset or verification tokens instead of hashes.
- Skipping integration tests for auth and token flows.[cite:1]
- Moving to change-request modules before auth, profile, and RBAC are solid.[cite:1]

## Recommended immediate action list

The most practical next actions after schema creation are:[cite:1]

1. Implement all auth-related entities and enums.[cite:1]
2. Build repositories for user, role, token, and session lookup.[cite:1]
3. Create request and response DTOs with Bean Validation.[cite:1]
4. Implement `AuthService`, `UserProfileService`, and `JwtService`.[cite:1]
5. Add Spring Security configuration and JWT filter.[cite:1]
6. Build `AuthController` and `UserProfileController`.[cite:1]
7. Add verification/reset email flows and integration tests.[cite:1]
8. Only then start `Project` and `ChangeRequest` modules.[cite:1]

## Final recommendation

The best next step is not “build everything after schema,” but “finish one complete identity slice.”[cite:1] In practice, that means schema to entity to repository to DTO to service to security to controller to tests, ending with a stable signup-login-profile-RBAC foundation that the rest of the system can safely build on.[cite:1]
