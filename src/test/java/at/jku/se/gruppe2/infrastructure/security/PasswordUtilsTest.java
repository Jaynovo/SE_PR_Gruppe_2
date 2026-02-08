package at.jku.se.gruppe2.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PasswordUtils}.
 *
 * <p>These tests verify deterministic hashing and correct verification behavior.</p>
 */
class PasswordUtilsTest {

    @Test
    void hashPassword_isDeterministic() {
        String h1 = PasswordUtils.hashPassword("secret");
        String h2 = PasswordUtils.hashPassword("secret");
        assertEquals(h1, h2);
        assertNotNull(h1);
        assertFalse(h1.isBlank());
    }

    @Test
    void hashPassword_changesWhenPasswordChanges() {
        String h1 = PasswordUtils.hashPassword("secret");
        String h2 = PasswordUtils.hashPassword("secret2");
        assertNotEquals(h1, h2);
    }

    @Test
    void verifyPassword_returnsTrueForMatch() {
        String hash = PasswordUtils.hashPassword("secret");
        assertTrue(PasswordUtils.verifyPassword("secret", hash));
    }

    @Test
    void verifyPassword_returnsFalseForMismatch() {
        String hash = PasswordUtils.hashPassword("secret");
        assertFalse(PasswordUtils.verifyPassword("wrong", hash));
    }
}