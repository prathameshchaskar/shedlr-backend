package com.shedlr.authservice.workspace.entity;

import com.shedlr.authservice.common.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Workspace represents a top-level tenant boundary.
 * All users, projects, and resources belong to a workspace.
 */
@Entity
@Table(name = "workspace")
@Getter
@Setter
public class Workspace extends AuditableEntity {

    /** Primary key for the workspace. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable name of the workspace. */
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /** Unique short code used for identification in URLs or logic (e.g., 'acme-corp'). */
    @Column(name = "code", nullable = false, length = 50, unique = true)
    private String code;

    /** Optional description of the workspace's purpose. */
    @Column(name = "description", length = 500)
    private String description;

    /** 
     * Status of the workspace (e.g., 'ACTIVE', 'SUSPENDED'). 
     * Stored as a string for flexibility in the initial design.
     */
    @Column(name = "status", nullable = false, length = 30)
    private String status;

}
