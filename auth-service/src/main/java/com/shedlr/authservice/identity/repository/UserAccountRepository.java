package com.shedlr.authservice.identity.repository;

import com.shedlr.authservice.identity.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * UserAccountRepository handles persistence for core user data.
 */
@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    /**
     * Find a user by their email. 
     * Essential for login and password recovery.
     */
    Optional<UserAccount> findByEmail(String email);

    /**
     * Efficiently check if an email is already registered.
     */
    boolean existsByEmail(String email);
}
