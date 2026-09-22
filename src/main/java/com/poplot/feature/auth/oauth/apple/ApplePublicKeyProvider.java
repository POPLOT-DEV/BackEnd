package com.poplot.feature.auth.oauth.apple;

import java.security.PublicKey;

@FunctionalInterface
public interface ApplePublicKeyProvider {
    PublicKey getPublicKey(String keyId);
}
