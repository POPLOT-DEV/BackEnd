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
    private static final String KEY_ID_PURPOSE = "poplot:qr-ticket-key-id";
    private static final int MINIMUM_SECRET_BYTES = 32;

    private final SecretKeySpec secretKey;
    private final String keyId;

    public QrTokenProvider(@Value("${qr-ticket.secret}") String secret) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalArgumentException("QR ticket secret must be at least 32 bytes.");
        }
        this.secretKey = new SecretKeySpec(secretBytes, HMAC_ALGORITHM);
        this.keyId = HexFormat.of().formatHex(sign(KEY_ID_PURPOSE));
    }

    public String createToken(UUID reservationPublicId) {
        byte[] signature = sign(TOKEN_PURPOSE + reservationPublicId);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    }

    public String hash(String token) {
        return digest(token.getBytes(StandardCharsets.UTF_8));
    }

    public String getKeyId() {
        return keyId;
    }

    public boolean matches(String token, String tokenHash) {
        if (tokenHash == null) {
            return false;
        }
        return MessageDigest.isEqual(
            hash(token).getBytes(StandardCharsets.US_ASCII),
            tokenHash.getBytes(StandardCharsets.US_ASCII)
        );
    }

    private byte[] sign(String message) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(secretKey);
            return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to sign QR ticket value.", exception);
        }
    }

    private String digest(byte[] value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to hash QR token value.", exception);
        }
    }
}
