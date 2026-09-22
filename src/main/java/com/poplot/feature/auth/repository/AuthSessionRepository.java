package com.poplot.feature.auth.repository;

import com.poplot.feature.auth.entity.AuthSession;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {
   Optional<AuthSession> findByRefreshTokenHash(String refreshTokenHash);

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("\n        select authSession\n        from AuthSession authSession\n        join fetch authSession.user\n        where authSession.refreshTokenHash = :refreshTokenHash\n        ")
   Optional<AuthSession> findByRefreshTokenHashForUpdate(@Param("refreshTokenHash") String refreshTokenHash);

   @Modifying(
      flushAutomatically = true,
      clearAutomatically = true
   )
   @Query("\n        update AuthSession authSession\n        set authSession.revokedAt = :revokedAt\n        where authSession.familyId = :familyId\n          and authSession.revokedAt is null\n        ")
   int revokeActiveSessionsByFamilyId(@Param("familyId") UUID familyId, @Param("revokedAt") Instant revokedAt);
}
