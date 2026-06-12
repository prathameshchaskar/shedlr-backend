-- V2__create_user_profile_and_role_assignment.sql
-- Purpose: Setup profile metadata and RBAC tables.

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
