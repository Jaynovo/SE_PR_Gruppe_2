package at.jku.se.gruppe2.infrastructure.security;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;


/**
 * Utility class for hashing and verifying passwords.
 *
 * <p>This class provides a minimal password handling mechanism based on
 * the SHA-256 cryptographic hash function.</p>
 *
 * <p><b>Security note:</b> This implementation does <em>not</em> use salting
 * or key stretching (e.g. PBKDF2, bcrypt, scrypt, Argon2). It is sufficient
 * for academic/demo purposes, but <strong>not recommended</strong> for
 * production systems.</p>
 */
public class PasswordUtils {

    /**
     * Hashes a raw password using SHA-256 and returns the result as a hexadecimal string.
     *
     * @param password the raw (plain-text) password to hash
     * @return SHA-256 hash encoded as a lowercase hexadecimal string
     * @throws RuntimeException if the SHA-256 algorithm is not available
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedPassword = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedPassword) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean verifyPassword(String rawPassword, String storedHash) {
        return hashPassword(rawPassword).equals(storedHash);
    }
}