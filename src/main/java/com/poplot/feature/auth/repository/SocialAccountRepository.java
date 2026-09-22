package com.poplot.feature.auth.repository;

import com.poplot.feature.auth.entity.SocialAccount;
import com.poplot.feature.auth.enums.SocialProvider;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
   @EntityGraph(
      attributePaths = {"user"}
   )
   Optional<SocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);
}
