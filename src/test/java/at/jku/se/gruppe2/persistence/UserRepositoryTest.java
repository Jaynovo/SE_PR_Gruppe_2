package at.jku.se.gruppe2.persistence;

import at.jku.se.gruppe2.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class UserRepositoryTest {
    private static HomeRepository homeRepository;
    private static UserRepository userRepository;

    @BeforeAll
    static void init() {
        homeRepository = new HomeRepository();
        userRepository = new UserRepository(homeRepository);
    }

    @BeforeEach
    void clearDatabase() throws SQLException {
        try (
            Connection conn = Database.getConnection())
            {
            conn.setAutoCommit(false);

            try (PreparedStatement statement1 = conn.prepareStatement("DELETE FROM address_information;")) {
                statement1.executeUpdate();
            }
            try (PreparedStatement statement2 = conn.prepareStatement("DELETE FROM home;")) {
                statement2.executeUpdate();
            }
            try (PreparedStatement statement3 = conn.prepareStatement("DELETE FROM user_information;")) {
                statement3.executeUpdate();
            }
            conn.commit();
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void createUserAndLoadByEmail() {
        Home home = null;
        String e_mail = "Max.Mustermann@example.com";

        User user = new User("Max", "Mustermann", e_mail, "swordfish", home);

        userRepository.createUserInDatabase(user);

        Optional<User> userOptional = userRepository.findUserByEmail(e_mail);
        assertTrue(userOptional.isPresent(), "User should be found via email (and has been created beforehand)");

        User loadedUser = userOptional.get();
        assertEquals(e_mail, loadedUser.getEmail());
        assertEquals("Max", loadedUser.getFirstName());
        assertEquals("Mustermann", loadedUser.getLastName());

        assertNull(loadedUser.getHome(), "This home should be Null");
    }

    @Test
    void feelGoodTest() {
        assert(true);
    }
}
