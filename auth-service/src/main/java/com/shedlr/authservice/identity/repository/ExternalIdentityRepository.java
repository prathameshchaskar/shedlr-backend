package com.shedlr.authservice.identity.repository;

import com.shedlr.authservice.identity.entity.ExternalIdentity;
import com.shedlr.authservice.identity.enumtype.ExternalProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

/**
 * Repository for managing links to external identity providers.
 */
public interface ExternalIdentityRepository extends JpaRepository<ExternalIdentity, Long> {

    /**
     * Find a user by their provider-specific unique ID.
     * Essential for OAuth2 login flows.
     */
    Optional<ExternalIdentity> findByProviderAndProviderSubject(ExternalProvider provider, String providerSubject);

    /** List all linked providers for a specific local user. */
    List<ExternalIdentity> findByUserId(Long userId);
}
