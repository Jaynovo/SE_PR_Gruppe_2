package at.jku.se.gruppe2.domain.model.user;

public enum UserRole {
    OWNER("Owner", 3),
    RESIDENT("Resident", 2),
    GUEST("Guest", 1);

    private final String displayName;
    private final int permissionLevel;

    UserRole(String displayName, int permissionLevel) {
        this.displayName = displayName;
        this.permissionLevel = permissionLevel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getPermissionLevel() {
        return permissionLevel;
    }

    public boolean hasPermission(UserRole requiredRole) {
        return this.permissionLevel >= requiredRole.permissionLevel;
    }

    @Override
    public String toString() {
        return displayName;
    }
}