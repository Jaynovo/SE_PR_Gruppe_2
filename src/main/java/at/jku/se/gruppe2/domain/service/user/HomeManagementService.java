package at.jku.se.gruppe2.domain.service.user;

import at.jku.se.gruppe2.domain.model.user.*;
import at.jku.se.gruppe2.infrastructure.persistence.repository.*;
import at.jku.se.gruppe2.infrastructure.security.Session;

import java.util.*;

/**
 * Service for managing home members and their roles
 */
public class HomeManagementService {
    private final UserHomeRepository userHomeRepository;
    private final AuthorizationService authorizationService;
    private final UserRepository userRepository;



    public HomeManagementService() {
        this.userHomeRepository = new UserHomeRepository();
        this.authorizationService = new AuthorizationService();
        this.userRepository = new UserRepository();
    }

    public HomeManagementService(UserHomeRepository userHomeRepository,
                                 AuthorizationService authorizationService) {
        this.userHomeRepository = userHomeRepository;
        this.authorizationService = authorizationService;
        this.userRepository = new UserRepository();
    }

    /**
     * Get all users in a home
     */
    public List<HomeUser> getHomeMembers(int homeId) {
        // Anyone who is a member can view other members
        authorizationService.requireMembership(homeId, "view home members");
        return userHomeRepository.getUsersInHome(homeId);
    }

    /**
     * Get all users in the current user's home
     */
    public List<HomeUser> getCurrentHomeMembers() {
        if (Session.getCurrentUser() == null || Session.getCurrentUser().getHome() == null) {
            throw new IllegalStateException("No home selected");
        }
        return getHomeMembers(Session.getCurrentUser().getHome().getId());
    }

    /**
     * Update a user's role in a home (owner only)
     */
    public boolean updateUserRole(int homeId, int userId, UserRole newRole) {
        // Check permissions
        if (!authorizationService.canChangeUserRole(homeId, userId, newRole)) {
            throw new SecurityException("You don't have permission to change this user's role");
        }

        return userHomeRepository.updateUserRole(userId, homeId, newRole) > 0;
    }

    /**
     * Remove a user from a home (owner only)
     */
    public boolean removeUserFromHome(int homeId, int userId) {
        if (!authorizationService.canRemoveUser(homeId, userId)) {
            throw new SecurityException("You don't have permission to remove this user");
        }

        boolean removed = userHomeRepository.removeUserFromHome(userId, homeId) > 0;

        if (removed) {
            // Clear the stale home_info FK so the user doesn't appear to still
            // belong to a home they've been removed from.
            User removedUser = new User();
            removedUser.setId(userId);
            userRepository.updateHome(removedUser, null);
        }

        return removed;
    }

    /**
     * Get the current user's role in a specific home
     */
    public Optional<UserRole> getUserRole(int homeId) {
        if (Session.getCurrentUser() == null) {
            return Optional.empty();
        }
        return userHomeRepository.getUserRoleInHome(Session.getCurrentUser().getId(), homeId);
    }

    /**
     * Get all owners of a home
     */
    public List<HomeUser> getHomeOwners(int homeId) {
        return userHomeRepository.getHomeOwners(homeId);
    }

    /**
     * Check if a home has at least one owner
     */
    public boolean homeHasOwner(int homeId) {
        return userHomeRepository.homeHasOwner(homeId);
    }

    /**
     * Count how many users have a specific role in a home
     */
    public int countUsersWithRole(int homeId, UserRole role) {
        return userHomeRepository.countUsersWithRole(homeId, role);
    }
}