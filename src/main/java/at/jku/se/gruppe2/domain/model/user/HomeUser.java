package at.jku.se.gruppe2.domain.model.user;

import java.time.LocalDateTime;

/**
 * Represents a user's membership in a home with their role
 */
public class HomeUser {
    private int userId;
    private int homeId;
    private UserRole role;
    private LocalDateTime joinedAt;

    // User details (for display purposes)
    private String firstName;
    private String lastName;
    private String email;
    private String avatarPath;

    // Constructors
    /**
     * Creates a HomeUser with default role {@link UserRole#GUEST}.
     */
    public HomeUser() {
        this.role = UserRole.GUEST; // Default role
    }

    /**
     * Creates a HomeUser with specified role.
     *
     * @param userId user identifier
     * @param homeId home identifier
     * @param role   assigned role
     */
    public HomeUser(int userId, int homeId, UserRole role) {
        this.userId = userId;
        this.homeId = homeId;
        this.role = role;
    }

    // Getters and Setters
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getHomeId() {
        return homeId;
    }

    public void setHomeId(int homeId) {
        this.homeId = homeId;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }

    // Convenience methods
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isOwner() {
        return role == UserRole.OWNER;
    }

    public boolean isResidentOrHigher() {
        return role.hasPermission(UserRole.RESIDENT);
    }

    public boolean isGuest() {
        return role == UserRole.GUEST;
    }

    public boolean hasPermission(UserRole requiredRole) {
        return role.hasPermission(requiredRole);
    }

    @Override
    public String toString() {
        return "HomeUser{" +
                "userId=" + userId +
                ", homeId=" + homeId +
                ", role=" + role +
                ", name='" + getFullName() + '\'' +
                '}';
    }
}