package com.poplot.feature.auth.service;

import com.poplot.feature.auth.entity.AuthSession;
import com.poplot.feature.auth.repository.AuthSessionRepository;
import com.poplot.feature.user.entity.User;
import com.poplot.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthTokenService {
    private final JwtProvider jwtProvider;
    private final RefreshTokenHasher refreshTokenHasher;
    private final AuthSessionRepository authSessionRepository;

    @Transactional
    public IssuedAuthTokens issue(User user) {
        return issueForFamily(user, UUID.randomUUID());
    }

    @Transactional
    public IssuedAuthTokens issueForFamily(User user, UUID familyId) {
        if (user.getId() == null) {
            throw new IllegalStateException("Cannot issue tokens for a transient user.");
        }
        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        authSessionRepository.save(new AuthSession(user, refreshTokenHasher.hash(refreshToken), jwtProvider.getExpiration(refreshToken), familyId));
        return new IssuedAuthTokens(accessToken, refreshToken, jwtProvider.getAccessTokenExpirationSeconds());
    }
}
