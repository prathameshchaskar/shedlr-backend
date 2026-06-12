package com.shedlr.authservice.workspace.repository;

import com.shedlr.authservice.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repository for Workspace entity.
 */
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    
    /**
     * Find a workspace by its unique code.
     * Used for workspace-based lookup during signup or login.
     */
    Optional<Workspace> findByCode(String code);

    /** Check if a workspace code is already taken. */
    boolean existsByCode(String code);
}
