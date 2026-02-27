package com.budgetpilot.security;

import com.budgetpilot.util.ValidationUtils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordHasher {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int SALT_LENGTH = 16;
    private static final int KEY_LENGTH = 256;
    private static final String PREFIX = "pbkdf2";

    private final SecureRandom secureRandom = new SecureRandom();

    public String hash(char[] password) {
        ValidationUtils.requireNonNull(password, "password");
        if (password.length < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        byte[] derived = derive(password, salt, ITERATIONS, KEY_LENGTH);
        return PREFIX
                + "$" + ITERATIONS
                + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(derived);
    }

    public boolean verify(char[] password, String hash) {
        if (password == null || hash == null || hash.isBlank()) {
            return false;
        }
        String[] parts = hash.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(password, salt, iterations, expected.length * 8);
            return constantTimeEquals(expected, actual);
        } catch (Exception ex) {
            return false;
        }
    }

    private byte[] derive(char[] password, byte[] salt, int iterations, int keyLength) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to hash password.", ex);
        }
    }

    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}
