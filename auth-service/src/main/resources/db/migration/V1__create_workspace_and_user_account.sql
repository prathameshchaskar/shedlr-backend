-- V1__create_workspace_and_user_account.sql
-- Purpose: Setup the core tenant and identity tables.

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
