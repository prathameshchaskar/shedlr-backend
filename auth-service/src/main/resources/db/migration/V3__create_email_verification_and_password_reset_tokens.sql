-- V3__create_email_verification_and_password_reset_tokens.sql
-- Purpose: Setup security token tables for verification and recovery.

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
