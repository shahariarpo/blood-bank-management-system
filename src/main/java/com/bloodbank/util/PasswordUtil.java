package com.bloodbank.util;

import org.mindrot.jbcrypt.BCrypt;

/** Password hashing and verification using BCrypt with a generated salt. */
public final class PasswordUtil {

    private static final int SALT_ROUNDS = 12;

    private PasswordUtil() {
        // Utility class - no instances.
    }

    /** Returns a salted BCrypt hash, never the plain password. */
    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(SALT_ROUNDS));
    }

    /** Compares a plain password with a stored hash. Invalid hashes return false. */
    public static boolean matches(String plainPassword, String storedHash) {
        if (storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, storedHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
