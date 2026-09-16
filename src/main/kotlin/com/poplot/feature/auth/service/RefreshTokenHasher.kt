package com.poplot.feature.auth.service

import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.HexFormat

@Component
class RefreshTokenHasher {
    fun hash(refreshToken: String): String {
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
            .digest(refreshToken.toByteArray(StandardCharsets.UTF_8))
        return HexFormat.of().formatHex(digest)
    }

    private companion object {
        const val HASH_ALGORITHM = "SHA-256"
    }
}
