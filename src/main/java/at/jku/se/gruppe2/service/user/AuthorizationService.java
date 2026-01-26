package at.jku.se.gruppe2.service.user;

import at.jku.se.gruppe2.model.user.*;
import at.jku.se.gruppe2.persistence.*;
import at.jku.se.gruppe2.utils.Session;

import java.util.*;

/**
 * Service to handle authorization and permission checks for users in homes.
 * It determines what actions users can perform based on their roles.
 */
public class AuthorizationService {
    private final UserHomeRepository userHomeRepository;

    public AuthorizationService() {
        this.userHomeRepository = new UserHomeRepository();
    }

    public AuthorizationService(UserHomeRepository userHomeRepository) {
        this.userHomeRepository = userHomeRepository;
    }

    /**
     * Check if current user can perform action requiring specific role
     */
    public boolean canPerformAction(int homeId, UserRole requiredRole) {
        if (Session.getCurrentUser() == null) {
            return false;
        }
        return userHomeRepository.hasPermission(
                Session.getCurrentUser().getId(),
                homeId,
                requiredRole
        );
    }

    /**
     * Get current user's role in a home
     */
    public Optional<UserRole> getCurrentUserRole(int homeId) {
        if (Session.getCurrentUser() == null) {
            return Optional.empty();
        }
        return userHomeRepository.getUserRoleInHome(
                Session.getCurrentUser().getId(),
                homeId
        );
    }

    /**
     * Get current user's role in their current home (from Session)
     */
    public Optional<UserRole> getCurrentUserRoleInCurrentHome() {
        if (Session.getCurrentUser() == null || Session.getCurrentUser().getHome() == null) {
            return Optional.empty();
        }
        return getCurrentUserRole(Session.getCurrentUser().getHome().getId());
    }

    // OWNER-ONLY PERMISSIONS
    /**
     * Only owners can invite users to the home
     */
    public boolean canInviteUsers(int homeId) {
        return canPerformAction(homeId, UserRole.OWNER);
    }

    /**
     * Only owners can manage (add/remove/change roles of) users
     */
    public boolean canManageUsers(int homeId) {
        return canPerformAction(homeId, UserRole.OWNER);
    }

    /**
     * Only owners can edit home details (address, floors, label, etc.)
     */
    public boolean canEditHomeDetails(int homeId) {
        return canPerformAction(homeId, UserRole.OWNER);
    }

    /**
     * Only owners can delete the home
     */
    public boolean canDeleteHome(int homeId) {
        return canPerformAction(homeId, UserRole.OWNER);
    }

    /**
     * Only owners can add new rooms
     */
    public boolean canAddRooms(int homeId) {
        return canPerformAction(homeId, UserRole.OWNER);
    }

    /**
     * Only owners can delete rooms
     */
    public boolean canDeleteRooms(int homeId) {
        return canPerformAction(homeId, UserRole.OWNER);
    }


    // RESIDENT+ PERMISSIONS
    /**
     * Residents and owners can rename rooms
     */
    public boolean canRenameRooms(int homeId) {
        return canPerformAction(homeId, UserRole.RESIDENT);
    }

    /**
     * Residents and owners can edit room details (dimensions, floor, etc.)
     */
    public boolean canEditRoomDetails(int homeId) {
        return canPerformAction(homeId, UserRole.RESIDENT);
    }

    /**
     * Residents and owners can add devices to rooms
     */
    public boolean canAddDevices(int homeId) {
        return canPerformAction(homeId, UserRole.RESIDENT);
    }

    /**
     * Residents and owners can remove devices from rooms
     */
    public boolean canRemoveDevices(int homeId) {
        return canPerformAction(homeId, UserRole.RESIDENT);
    }

    /**
     * Residents and owners can configure devices (rename, change settings)
     */
    public boolean canConfigureDevices(int homeId) {
        return canPerformAction(homeId, UserRole.RESIDENT);
    }

    /**
     * Residents and owners can create and manage automation rules
     */
    public boolean canManageRules(int homeId) {
        return canPerformAction(homeId, UserRole.RESIDENT);
    }

    /**
     * Residents and owners can configure actuator settings (e.g., alarm codes, schedules)
     */
    public boolean canConfigureActuators(int homeId) {
        return canPerformAction(homeId, UserRole.RESIDENT);
    }

    /**
     * Residents and owners can view sensor data
     */
    public boolean canViewSensorData(int homeId) {
        return canPerformAction(homeId, UserRole.RESIDENT);
    }

    // GUEST+ PERMISSIONS
    /**
     * All users (including guests) can control basic actuators (lights, blinds, etc.)
     */
    public boolean canControlActuators(int homeId) {
        return canPerformAction(homeId, UserRole.GUEST);
    }

    /**
     * All users can view room information
     */
    public boolean canViewRooms(int homeId) {
        return canPerformAction(homeId, UserRole.GUEST);
    }

    /**
     * All users can view home dashboard
     */
    public boolean canViewDashboard(int homeId) {
        return canPerformAction(homeId, UserRole.GUEST);
    }


    // SPECIAL PERMISSION CHECKS
    /**
     * Checks if user can change role of another user
     * Users cannot change their own role or promote anyone to a higher role than themselves
     */
    public boolean canChangeUserRole(int homeId, int targetUserId, UserRole newRole) {
        if (Session.getCurrentUser() == null) {
            return false;
        }

        int currentUserId = Session.getCurrentUser().getId();

        // Can't change your own role
        if (currentUserId == targetUserId) {
            return false;
        }

        // Must be owner to change roles
        if (!canManageUsers(homeId)) {
            return false;
        }

        Optional<UserRole> currentUserRole = getCurrentUserRole(homeId);
        if (currentUserRole.isEmpty()) {
            return false;
        }

        // Can't promote someone to a higher role than yourself
        return currentUserRole.get().hasPermission(newRole);
    }

    /**
     * Checks if user can remove another user from the home
     * Can't remove yourself, must be owner, and can't remove the last owner
     */
    public boolean canRemoveUser(int homeId, int targetUserId) {
        if (Session.getCurrentUser() == null) {
            return false;
        }

        int currentUserId = Session.getCurrentUser().getId();

        // Can't remove yourself
        if (currentUserId == targetUserId) {
            return false;
        }

        // Must be owner to remove users
        if (!canManageUsers(homeId)) {
            return false;
        }

        // Check if target is the last owner
        Optional<UserRole> targetRole = userHomeRepository.getUserRoleInHome(targetUserId, homeId);
        if (targetRole.isPresent() && targetRole.get() == UserRole.OWNER) {
            int ownerCount = userHomeRepository.countUsersWithRole(homeId, UserRole.OWNER);
            if (ownerCount <= 1) {
                return false; // Can't remove the last owner
            }
        }

        return true;
    }


    // EXCEPTION-THROWING METHODS (for enforcing permissions)
    /**
     * Throw exception if user doesn't have permission
     */
    public void requirePermission(int homeId, UserRole requiredRole, String action) {
        if (!canPerformAction(homeId, requiredRole)) {
            UserRole currentRole = getCurrentUserRole(homeId).orElse(null);
            throw new SecurityException(
                    String.format("Insufficient permissions to %s. Required: %s, Current: %s",
                            action,
                            requiredRole.getDisplayName(),
                            currentRole != null ? currentRole.getDisplayName() : "None")
            );
        }
    }

    /**
     * Require owner permission or throw exception
     */
    public void requireOwner(int homeId, String action) {
        requirePermission(homeId, UserRole.OWNER, action);
    }

    /**
     * Require resident or higher permission or throw exception
     */
    public void requireResident(int homeId, String action) {
        requirePermission(homeId, UserRole.RESIDENT, action);
    }

    /**
     * Require guest or higher permission
     */
    public void requireMembership(int homeId, String action) {
        requirePermission(homeId, UserRole.GUEST, action);
    }
}