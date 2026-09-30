package com.poplot.feature.reservation.token;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class QrTokenProvider {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String TOKEN_PURPOSE = "poplot:qr-ticket:";

    private final SecretKeySpec secretKey;

    public QrTokenProvider(@Value("${qr-ticket.secret}") String secret) {
        this.secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
    }

    public String createToken(UUID reservationPublicId) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(secretKey);
            byte[] signature = mac.doFinal((TOKEN_PURPOSE + reservationPublicId).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to create QR token.", exception);
        }
    }

    public String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to hash QR token.", exception);
        }
    }
}
