package com.shedlr.authservice.identity.repository;

import com.shedlr.authservice.identity.entity.RoleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * RoleAssignmentRepository manages user permissions across workspaces and projects.
 */
public interface RoleAssignmentRepository extends JpaRepository<RoleAssignment, Long> {

    /**
     * Find all active roles for a user.
     * Used for building the security context and JWT claims.
     */
    List<RoleAssignment> findByUserIdAndActiveTrue(Long userId);

    /** Find roles for a user within a specific workspace. */
    List<RoleAssignment> findByUserIdAndWorkspaceIdAndActiveTrue(Long userId, Long workspaceId);
}
