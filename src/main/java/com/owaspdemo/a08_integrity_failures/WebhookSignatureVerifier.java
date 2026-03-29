package com.owaspdemo.a08_integrity_failures;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public class WebhookSignatureVerifier {

    private final byte[] secret;

    public WebhookSignatureVerifier(String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String computeSignature(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC", e);
        }
    }

    public boolean verify(String payload, String signature) {
        String expected = computeSignature(payload);
        // GOOD: Constant-time comparison to prevent timing attacks
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }
}
