-- V4__create_external_identity_and_user_session.sql
-- Purpose: Setup OAuth linkages and session management.

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
