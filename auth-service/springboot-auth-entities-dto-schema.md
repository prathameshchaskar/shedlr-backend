# Production-Ready Spring Boot Auth, Profile, DTO, and Schema Design

This document defines a production-ready identity and access design for a Spring Boot application that supports authentication, signup, email verification, forgot password, Google login, profile management, workspace membership, and role-based access control for a multi-user workflow platform.[cite:1]

## Scope

The design is intentionally centered on the authentication and profile lifecycle rather than the full change-request domain.[cite:1] It aligns with the product direction where users belong to workspaces, roles are assigned at workspace or project level, sensitive actions must be auditable, and the backend should use DTOs instead of exposing JPA entities directly from controllers.[cite:1]

## Recommended module structure

A clean modular monolith structure for the auth and profile layer should separate persistence, API contracts, service logic, and security integration.[cite:1]

```text
com.shedlr
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
  └── workspace
      ├── entity
      └── repository
```

## Domain design overview

The product document already establishes the core identity concepts needed for this flow: `User`, `Workspace`, and `RoleAssignment`, with support for RBAC, traceability, and workspace-aware authorization.[cite:1] For a production-ready auth lifecycle, that base should be extended with verification tokens, password reset tokens, refresh-token or session records, and optional external identity linkage for Google login.[cite:1]

### Core auth entities

| Entity | Purpose |
|---|---|
| `UserAccount` | Stores login identity, password hash, verification status, and lifecycle flags [cite:1] |
| `Workspace` | Represents organization or company boundary [cite:1] |
| `RoleAssignment` | Assigns roles at workspace or project scope [cite:1] |
| `EmailVerificationToken` | One-time token for email verification |
| `PasswordResetToken` | One-time token for password reset |
| `UserSession` | Tracks refresh token or session metadata for logout/session management |
| `ExternalIdentity` | Links Google or other OAuth provider to the local account |
| `UserProfile` | Optional extension table for profile metadata not essential to login |

## Enums

The auth model is safer and easier to validate when state-like columns are represented as enums in Java and constrained in SQL.[cite:1]

```java
public enum UserStatus {
    PENDING_VERIFICATION,
    ACTIVE,
    LOCKED,
    DISABLED,
    DELETED
}

public enum RoleType {
    FOUNDER,
    PM,
    PRODUCT_MANAGER,
    ENGINEERING_LEAD,
    DEVELOPER,
    QA,
    RELEASE_MANAGER,
    ADMIN
}

public enum RoleScopeType {
    WORKSPACE,
    PROJECT
}

public enum TokenPurpose {
    EMAIL_VERIFICATION,
    PASSWORD_RESET
}

public enum ExternalProvider {
    GOOGLE
}

public enum SessionStatus {
    ACTIVE,
    REVOKED,
    EXPIRED
}
```

## Base entity recommendation

A shared audited base class keeps core timestamps and optimistic locking consistent across entities, which fits the product requirement for traceability and update safety.[cite:1]

```java
package com.shedlr.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
```

## Entity design

### Workspace entity

The workspace remains the top-level tenant boundary in the product model and should be referenced by users, projects, and workspace-scoped roles.[cite:1]

```java
package com.shedlr.workspace.entity;

import com.shedlr.common.audit.AuditableEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "workspace")
public class Workspace extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "code", nullable = false, length = 50, unique = true)
    private String code;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    // getters/setters
}
```

### UserAccount entity

`UserAccount` should contain only account and security information that is required for login, verification, and authorization.[cite:1] Using a separate profile table for optional display fields is cleaner than making the login entity absorb every future user preference.[cite:1]

```java
package com.shedlr.identity.entity;

import com.shedlr.common.audit.AuditableEntity;
import com.shedlr.identity.enumtype.UserStatus;
import com.shedlr.workspace.entity.Workspace;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "user_account",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_account_email", columnNames = "email")
    },
    indexes = {
        @Index(name = "idx_user_account_workspace_id", columnList = "workspace_id"),
        @Index(name = "idx_user_account_status", columnList = "status"),
        @Index(name = "idx_user_account_email_verified", columnList = "email_verified")
    }
)
public class UserAccount extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private UserStatus status;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "email_verified_at")
    private OffsetDateTime emailVerifiedAt;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    // getters/setters
}
```

### UserProfile entity

`UserProfile` is optional but recommended when profile data is expected to grow beyond identity essentials such as time zone, locale, or notification preferences.[cite:1]

```java
package com.shedlr.identity.entity;

import com.shedlr.common.audit.AuditableEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "user_profile")
public class UserProfile extends AuditableEntity {

    @Id
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @Column(name = "job_title", length = 100)
    private String jobTitle;

    @Column(name = "time_zone", length = 60)
    private String timeZone;

    @Column(name = "locale", length = 20)
    private String locale;

    @Column(name = "email_notifications_enabled", nullable = false)
    private boolean emailNotificationsEnabled;

    @Column(name = "in_app_notifications_enabled", nullable = false)
    private boolean inAppNotificationsEnabled;

    // getters/setters
}
```

### RoleAssignment entity

The product document requires a user to belong to multiple projects with different roles and also supports workspace-level control, so role assignment should support both scopes in one table.[cite:1]

```java
package com.shedlr.identity.entity;

import com.shedlr.common.audit.AuditableEntity;
import com.shedlr.identity.enumtype.RoleScopeType;
import com.shedlr.identity.enumtype.RoleType;
import com.shedlr.workspace.entity.Workspace;
import jakarta.persistence.*;

@Entity
@Table(
    name = "role_assignment",
    indexes = {
        @Index(name = "idx_role_assignment_user_id", columnList = "user_id"),
        @Index(name = "idx_role_assignment_workspace_id", columnList = "workspace_id"),
        @Index(name = "idx_role_assignment_project_id", columnList = "project_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_role_assignment_scope",
            columnNames = {"user_id", "role_type", "scope_type", "workspace_id", "project_id"}
        )
    }
)
public class RoleAssignment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "project_id")
    private Long projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false, length = 50)
    private RoleType roleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 20)
    private RoleScopeType scopeType;

    @Column(name = "active", nullable = false)
    private boolean active;

    // getters/setters
}
```

### EmailVerificationToken entity

Verification tokens should be one-time use, expiring, auditable records rather than transient strings with no persistence.[cite:1]

```java
package com.shedlr.identity.entity;

import com.shedlr.common.audit.AuditableEntity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "email_verification_token",
    indexes = {
        @Index(name = "idx_email_verification_token_user_id", columnList = "user_id"),
        @Index(name = "idx_email_verification_token_expires_at", columnList = "expires_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_email_verification_token_token_hash", columnNames = "token_hash")
    }
)
public class EmailVerificationToken extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "sent_to_email", nullable = false, length = 320)
    private String sentToEmail;

    // getters/setters
}
```

### PasswordResetToken entity

Password reset should use a dedicated table so tokens can be revoked, expired, and audited independently from email verification.[cite:1]

```java
package com.shedlr.identity.entity;

import com.shedlr.common.audit.AuditableEntity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "password_reset_token",
    indexes = {
        @Index(name = "idx_password_reset_token_user_id", columnList = "user_id"),
        @Index(name = "idx_password_reset_token_expires_at", columnList = "expires_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_password_reset_token_token_hash", columnNames = "token_hash")
    }
)
public class PasswordResetToken extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "requested_from_ip", length = 64)
    private String requestedFromIp;

    @Column(name = "requested_user_agent", length = 500)
    private String requestedUserAgent;

    // getters/setters
}
```

### ExternalIdentity entity

Google login should not create duplicate accounts, so an external identity table should map provider users to local users with uniqueness enforced at the database layer.[cite:1]

```java
package com.shedlr.identity.entity;

import com.shedlr.common.audit.AuditableEntity;
import com.shedlr.identity.enumtype.ExternalProvider;
import jakarta.persistence.*;

@Entity
@Table(
    name = "external_identity",
    indexes = {
        @Index(name = "idx_external_identity_user_id", columnList = "user_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_external_identity_provider_subject", columnNames = {"provider", "provider_subject"})
    }
)
public class ExternalIdentity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30)
    private ExternalProvider provider;

    @Column(name = "provider_subject", nullable = false, length = 255)
    private String providerSubject;

    @Column(name = "provider_email", length = 320)
    private String providerEmail;

    @Column(name = "provider_email_verified", nullable = false)
    private boolean providerEmailVerified;

    // getters/setters
}
```

### UserSession entity

A session table becomes valuable when the system wants refresh-token rotation, logout-from-all-devices, security review, or session revocation after password reset.[cite:1]

```java
package com.shedlr.identity.entity;

import com.shedlr.common.audit.AuditableEntity;
import com.shedlr.identity.enumtype.SessionStatus;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "user_session",
    indexes = {
        @Index(name = "idx_user_session_user_id", columnList = "user_id"),
        @Index(name = "idx_user_session_status", columnList = "status"),
        @Index(name = "idx_user_session_expires_at", columnList = "expires_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_session_refresh_token_hash", columnNames = "refresh_token_hash")
    }
)
public class UserSession extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(name = "refresh_token_hash", nullable = false, length = 255)
    private String refreshTokenHash;

    @Column(name = "device_name", length = 120)
    private String deviceName;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SessionStatus status;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    // getters/setters
}
```

## DTO design

The product guidance explicitly recommends DTOs in controllers, Bean Validation on inputs, and stable API contracts, so request and response objects should be explicit and use-case-specific rather than generic dump objects.[cite:1]

### Request DTOs

```java
package com.shedlr.identity.dto.request;

import jakarta.validation.constraints.*;

public record SignupRequest(
    @NotBlank @Size(max = 150) String fullName,
    @NotBlank @Email @Size(max = 320) String email,
    @NotBlank @Size(min = 8, max = 100) String password,
    @NotBlank @Size(min = 8, max = 100) String confirmPassword,
    @AssertTrue Boolean acceptTerms
) {}
```

```java
package com.shedlr.identity.dto.request;

import jakarta.validation.constraints.*;

public record LoginRequest(
    @NotBlank @Email @Size(max = 320) String email,
    @NotBlank @Size(max = 100) String password,
    Boolean rememberMe
) {}
```

```java
package com.shedlr.identity.dto.request;

import jakarta.validation.constraints.*;

public record ForgotPasswordRequest(
    @NotBlank @Email @Size(max = 320) String email
) {}
```

```java
package com.shedlr.identity.dto.request;

import jakarta.validation.constraints.*;

public record ResetPasswordRequest(
    @NotBlank String token,
    @NotBlank @Size(min = 8, max = 100) String newPassword,
    @NotBlank @Size(min = 8, max = 100) String confirmPassword
) {}
```

```java
package com.shedlr.identity.dto.request;

import jakarta.validation.constraints.*;

public record VerifyEmailRequest(
    @NotBlank String token
) {}
```

```java
package com.shedlr.identity.dto.request;

import jakarta.validation.constraints.*;

public record ResendVerificationRequest(
    @NotBlank @Email @Size(max = 320) String email
) {}
```

```java
package com.shedlr.identity.dto.request;

import jakarta.validation.constraints.*;

public record UpdateProfileRequest(
    @NotBlank @Size(max = 150) String fullName,
    @Size(max = 500) String avatarUrl,
    @Size(max = 100) String jobTitle,
    @Size(max = 60) String timeZone,
    @Size(max = 20) String locale,
    Boolean emailNotificationsEnabled,
    Boolean inAppNotificationsEnabled
) {}
```

```java
package com.shedlr.identity.dto.request;

import jakarta.validation.constraints.*;

public record ChangePasswordRequest(
    @NotBlank String currentPassword,
    @NotBlank @Size(min = 8, max = 100) String newPassword,
    @NotBlank @Size(min = 8, max = 100) String confirmPassword
) {}
```

```java
package com.shedlr.identity.dto.request;

import jakarta.validation.constraints.*;

public record GoogleLoginRequest(
    @NotBlank String idToken
) {}
```

### Response DTOs

```java
package com.shedlr.identity.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresInSeconds,
    UserSummaryResponse user
) {}
```

```java
package com.shedlr.identity.dto.response;

import java.util.List;

public record UserSummaryResponse(
    Long id,
    String email,
    String fullName,
    boolean emailVerified,
    String status,
    List<String> roles,
    List<WorkspaceMembershipResponse> workspaces
) {}
```

```java
package com.shedlr.identity.dto.response;

public record WorkspaceMembershipResponse(
    Long workspaceId,
    String workspaceName,
    String workspaceCode
) {}
```

```java
package com.shedlr.identity.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record UserProfileResponse(
    Long id,
    String email,
    String fullName,
    boolean emailVerified,
    OffsetDateTime emailVerifiedAt,
    String avatarUrl,
    String jobTitle,
    String timeZone,
    String locale,
    boolean emailNotificationsEnabled,
    boolean inAppNotificationsEnabled,
    List<String> linkedProviders,
    List<String> roles
) {}
```

```java
package com.shedlr.identity.dto.response;

public record GenericMessageResponse(
    String message
) {}
```

## Validation rules beyond annotations

Bean Validation annotations are necessary but not sufficient for auth flows.[cite:1] Business validation should also enforce the rules below in the service layer.[cite:1]

- `password` and `confirmPassword` must match for signup and reset password.
- Email should be normalized to lowercase and trimmed before uniqueness checks.
- Signup should reject duplicate local accounts unless the product intentionally supports invitation-claim flows.
- A password-based login should be blocked for users without a password hash if the account was created only through Google.
- Email change should create a new verification cycle before marking the new email trusted.
- Expired, used, or revoked tokens must be rejected.[cite:1]

## Mapper approach

Using mappers keeps controllers thin and avoids accidentally returning internal fields like `passwordHash`, token hashes, or lock metadata.[cite:1]

```java
package com.shedlr.identity.mapper;

import com.shedlr.identity.dto.response.UserProfileResponse;
import com.shedlr.identity.entity.UserAccount;
import com.shedlr.identity.entity.UserProfile;
import java.util.List;

public final class UserProfileMapper {

    private UserProfileMapper() {}

    public static UserProfileResponse toResponse(
        UserAccount user,
        UserProfile profile,
        List<String> linkedProviders,
        List<String> roles
    ) {
        return new UserProfileResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.isEmailVerified(),
            user.getEmailVerifiedAt(),
            user.getAvatarUrl(),
            profile != null ? profile.getJobTitle() : null,
            profile != null ? profile.getTimeZone() : null,
            profile != null ? profile.getLocale() : null,
            profile != null && profile.isEmailNotificationsEnabled(),
            profile != null && profile.isInAppNotificationsEnabled(),
            linkedProviders,
            roles
        );
    }
}
```

## Repository recommendations

Repository design should focus on common lookup paths used by login, verification, and reset flows.[cite:1]

```java
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmail(String email);
    boolean existsByEmail(String email);
}

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
}

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}

public interface RoleAssignmentRepository extends JpaRepository<RoleAssignment, Long> {
    List<RoleAssignment> findByUserIdAndActiveTrue(Long userId);
}

public interface ExternalIdentityRepository extends JpaRepository<ExternalIdentity, Long> {
    Optional<ExternalIdentity> findByProviderAndProviderSubject(ExternalProvider provider, String providerSubject);
}
```

## Suggested PostgreSQL schema

The schema below is designed for Flyway SQL migrations, uses explicit constraints and indexes, and follows the product guidance of surrogate keys, auditable records, stable contracts, and disciplined migrations.[cite:1]

```sql
-- V1__init_identity_schema.sql

create table workspace (
    id bigserial primary key,
    name varchar(150) not null,
    code varchar(50) not null unique,
    description varchar(500),
    status varchar(30) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0
);

create table user_account (
    id bigserial primary key,
    workspace_id bigint references workspace(id),
    email varchar(320) not null,
    password_hash varchar(255),
    full_name varchar(150) not null,
    status varchar(40) not null,
    email_verified boolean not null default false,
    email_verified_at timestamptz,
    last_login_at timestamptz,
    failed_login_attempts integer not null default 0,
    locked_until timestamptz,
    must_change_password boolean not null default false,
    avatar_url varchar(500),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0,
    constraint uk_user_account_email unique (email),
    constraint chk_user_account_status check (status in ('PENDING_VERIFICATION','ACTIVE','LOCKED','DISABLED','DELETED'))
);

create index idx_user_account_workspace_id on user_account(workspace_id);
create index idx_user_account_status on user_account(status);
create index idx_user_account_email_verified on user_account(email_verified);

create table user_profile (
    user_id bigint primary key references user_account(id) on delete cascade,
    job_title varchar(100),
    time_zone varchar(60),
    locale varchar(20),
    email_notifications_enabled boolean not null default true,
    in_app_notifications_enabled boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0
);

create table role_assignment (
    id bigserial primary key,
    user_id bigint not null references user_account(id) on delete cascade,
    workspace_id bigint not null references workspace(id) on delete cascade,
    project_id bigint,
    role_type varchar(50) not null,
    scope_type varchar(20) not null,
    active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0,
    constraint chk_role_assignment_role_type check (
        role_type in ('FOUNDER','PM','PRODUCT_MANAGER','ENGINEERING_LEAD','DEVELOPER','QA','RELEASE_MANAGER','ADMIN')
    ),
    constraint chk_role_assignment_scope_type check (
        scope_type in ('WORKSPACE','PROJECT')
    ),
    constraint chk_role_assignment_project_scope check (
        (scope_type = 'WORKSPACE' and project_id is null) or
        (scope_type = 'PROJECT' and project_id is not null)
    ),
    constraint uk_role_assignment_scope unique (user_id, role_type, scope_type, workspace_id, project_id)
);

create index idx_role_assignment_user_id on role_assignment(user_id);
create index idx_role_assignment_workspace_id on role_assignment(workspace_id);
create index idx_role_assignment_project_id on role_assignment(project_id);

create table email_verification_token (
    id bigserial primary key,
    user_id bigint not null references user_account(id) on delete cascade,
    token_hash varchar(255) not null,
    sent_to_email varchar(320) not null,
    expires_at timestamptz not null,
    used_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0,
    constraint uk_email_verification_token_hash unique (token_hash)
);

create index idx_email_verification_token_user_id on email_verification_token(user_id);
create index idx_email_verification_token_expires_at on email_verification_token(expires_at);

create table password_reset_token (
    id bigserial primary key,
    user_id bigint not null references user_account(id) on delete cascade,
    token_hash varchar(255) not null,
    expires_at timestamptz not null,
    used_at timestamptz,
    requested_from_ip varchar(64),
    requested_user_agent varchar(500),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0,
    constraint uk_password_reset_token_hash unique (token_hash)
);

create index idx_password_reset_token_user_id on password_reset_token(user_id);
create index idx_password_reset_token_expires_at on password_reset_token(expires_at);

create table external_identity (
    id bigserial primary key,
    user_id bigint not null references user_account(id) on delete cascade,
    provider varchar(30) not null,
    provider_subject varchar(255) not null,
    provider_email varchar(320),
    provider_email_verified boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0,
    constraint chk_external_identity_provider check (provider in ('GOOGLE')),
    constraint uk_external_identity_provider_subject unique (provider, provider_subject)
);

create index idx_external_identity_user_id on external_identity(user_id);

create table user_session (
    id bigserial primary key,
    user_id bigint not null references user_account(id) on delete cascade,
    refresh_token_hash varchar(255) not null,
    device_name varchar(120),
    ip_address varchar(64),
    user_agent varchar(500),
    status varchar(20) not null,
    expires_at timestamptz not null,
    revoked_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0,
    constraint chk_user_session_status check (status in ('ACTIVE','REVOKED','EXPIRED')),
    constraint uk_user_session_refresh_token_hash unique (refresh_token_hash)
);

create index idx_user_session_user_id on user_session(user_id);
create index idx_user_session_status on user_session(status);
create index idx_user_session_expires_at on user_session(expires_at);
```

## Recommended API surface

The product document already proposes core auth endpoints such as login, refresh, and current-user lookup, and the auth flow needs a few more endpoints to fully support signup and recovery.[cite:1]

| Use case | Endpoint | Request DTO | Response DTO |
|---|---|---|---|
| Signup | `POST /api/v1/auth/signup` | `SignupRequest` | `GenericMessageResponse` |
| Login | `POST /api/v1/auth/login` | `LoginRequest` | `AuthResponse` [cite:1] |
| Refresh | `POST /api/v1/auth/refresh` | refresh token cookie/body | `AuthResponse` [cite:1] |
| Verify email | `POST /api/v1/auth/verify-email` | `VerifyEmailRequest` | `GenericMessageResponse` |
| Resend verification | `POST /api/v1/auth/resend-verification` | `ResendVerificationRequest` | `GenericMessageResponse` |
| Forgot password | `POST /api/v1/auth/forgot-password` | `ForgotPasswordRequest` | `GenericMessageResponse` |
| Reset password | `POST /api/v1/auth/reset-password` | `ResetPasswordRequest` | `GenericMessageResponse` |
| My profile | `GET /api/v1/users/me` | — | `UserProfileResponse` [cite:1] |
| Update profile | `PUT /api/v1/users/me` | `UpdateProfileRequest` | `UserProfileResponse` |
| Change password | `POST /api/v1/users/me/change-password` | `ChangePasswordRequest` | `GenericMessageResponse` |
| Google sign-in | `POST /api/v1/auth/google` | `GoogleLoginRequest` | `AuthResponse` |
| Logout current session | `POST /api/v1/auth/logout` | — | `GenericMessageResponse` |
| Logout all sessions | `POST /api/v1/auth/logout-all` | — | `GenericMessageResponse` |

## Service-layer behavior

The service layer should own security-sensitive rules rather than relying only on controller annotations, which matches the product recommendation for API-level and service-level enforcement.[cite:1]

### Signup service behavior

- Normalize email.
- Validate password confirmation.
- Encode password with BCrypt or Argon2 as recommended by the product design.[cite:1]
- Create `UserAccount` with `PENDING_VERIFICATION` and `emailVerified = false`.[cite:1]
- Create default `UserProfile` row optionally.
- Generate verification token record.
- Send email after transaction-safe persistence.[cite:1]

### Login service behavior

- Look up user by normalized email.
- Verify account status and lock state.
- Check password hash.
- Optionally block login until email is verified.
- Update `lastLoginAt` on success.
- Create or rotate `UserSession` when refresh-token-based authentication is used.[cite:1]

### Forgot password behavior

- Always return a generic success message.
- Create reset token only if user exists and is eligible.
- Invalidate older active password reset tokens for that user.
- Send reset email asynchronously or after commit.[cite:1]

### Reset password behavior

- Validate token hash lookup.
- Reject if expired or used.
- Encode new password.
- Mark token used.
- Revoke existing active sessions if policy requires it.[cite:1]

## Security implementation notes

Several details make the difference between a demo auth module and a production-ready one.[cite:1]

- Store hashes for verification and reset tokens, not raw tokens.
- Keep password hashes nullable only if social-only accounts are supported.
- Add rate limiting on login, resend verification, and forgot password operations because the product already anticipates anti-abuse controls.[cite:1]
- Audit email change, password change, login success, login failure threshold, session revocation, and role changes because the broader platform treats traceability as essential.[cite:1]
- Restrict admin and role-management operations to workspace boundaries.[cite:1]

## Suggested Flyway migration split

The product document strongly recommends small, coherent Flyway migrations with clear intent and forward-only discipline, so the auth schema should also be broken into focused files instead of one oversized script.[cite:1]

```text
V1__create_workspace_and_user_account.sql
V2__create_user_profile_and_role_assignment.sql
V3__create_email_verification_and_password_reset_tokens.sql
V4__create_external_identity_and_user_session.sql
V5__seed_default_roles_reference_if_needed.sql
```

## Practical recommendations

The best starting point for this Spring Boot flow is a lean but extensible identity model: `UserAccount`, `Workspace`, `RoleAssignment`, `UserProfile`, verification and reset token tables, external identity linkage, and session tracking.[cite:1] That design fits the product’s RBAC, workspace-aware permissions, auditability requirements, JWT-based authentication direction, and email-driven account lifecycle without overcomplicating the first implementation.[cite:1]
