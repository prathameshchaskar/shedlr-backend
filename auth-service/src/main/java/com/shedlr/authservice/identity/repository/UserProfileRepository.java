package com.shedlr.authservice.identity.repository;

import com.shedlr.authservice.identity.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * UserProfileRepository handles persistence operations for the UserProfile entity.
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
}
