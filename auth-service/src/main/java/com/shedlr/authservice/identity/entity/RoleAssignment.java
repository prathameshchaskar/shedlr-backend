package com.shedlr.authservice.identity.entity;

import com.shedlr.authservice.common.audit.AuditableEntity;
import com.shedlr.authservice.identity.enumtype.RoleScopeType;
import com.shedlr.authservice.identity.enumtype.RoleType;
import com.shedlr.authservice.workspace.entity.Workspace;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * RoleAssignment bridges users to roles within a specific scope (Workspace or Project).
 * This supports multi-tenancy where a user can have different roles in different contexts.
 */
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
@Getter
@Setter
public class RoleAssignment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user who is assigned the role. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    /** The workspace boundary for this role assignment. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    /** 
     * Optional project reference. 
     * Null if the role is at the workspace level (scope_type = WORKSPACE).
     */
    @Column(name = "project_id")
    private Long projectId;

    /** The specific role assigned. */
    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false, length = 50)
    private RoleType roleType;

    /** Whether the role is for the whole workspace or just a project. */
    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 20)
    private RoleScopeType scopeType;

    /** Flag to temporarily deactivate a role without deleting the record. */
    @Column(name = "active", nullable = false)
    private boolean active;

}
