package at.jku.se.gruppe2.domain.model.user;

import java.time.LocalDateTime;

/**
 * Represents an invitation for a user to join a specific home.
 *
 * <p>An invitation contains information about the inviter, the invitee (via email),
 * the assigned role and its current {@link Status}. It also tracks timestamps for
 * when the invitation was sent and when it was responded to.</p>
 */
public class HomeInvitation {

    /**
     * states of an invitation.
     */
    public enum Status {
        PENDING, ACCEPTED, DECLINED, CANCELLED
    }

    private int id;
    private int homeId;
    private int inviterUserId;
    private String inviteeEmail;
    private Status status;
    private UserRole invitedRole = UserRole.GUEST;
    private LocalDateTime invitedAt;
    private LocalDateTime respondedAt;

    // For display purposes
    private String homeName;
    private String inviterName;


    // Constructors
    /**
     * Creates a new invitation with default status {@code PENDING}
     * and sets {@code invitedAt} to the current timestamp.
     */
    public HomeInvitation() {
        this.status = Status.PENDING;
        this.invitedAt = LocalDateTime.now();
    }

    /**
     * Creates a new invitation.
     *
     * @param homeId        identifier of the home
     * @param inviterUserId identifier of the inviting user
     * @param inviteeEmail  email of the invited user
     */
    public HomeInvitation(int homeId, int inviterUserId, String inviteeEmail) {
        this();
        this.homeId = homeId;
        this.inviterUserId = inviterUserId;
        this.inviteeEmail = inviteeEmail;
    }

    /** @return invitation identifier */
    public int getId() { return id; }

    /** @param id invitation identifier */
    public void setId(int id) { this.id = id; }

    /** @return home identifier */
    public int getHomeId() { return homeId; }

    /** @param homeId home identifier */
    public void setHomeId(int homeId) { this.homeId = homeId; }

    /** @return inviter user identifier */
    public int getInviterUserId() { return inviterUserId; }

    /** @param inviterUserId inviter user identifier */
    public void setInviterUserId(int inviterUserId) { this.inviterUserId = inviterUserId; }

    /** @return invitee email address */
    public String getInviteeEmail() { return inviteeEmail; }

    /** @param inviteeEmail invitee email address */
    public void setInviteeEmail(String inviteeEmail) { this.inviteeEmail = inviteeEmail; }

    /** @return current invitation status */
    public Status getStatus() { return status; }

    /** @param status new invitation status */
    public void setStatus(Status status) { this.status = status; }

    /** @return timestamp when invitation was sent */
    public LocalDateTime getInvitedAt() { return invitedAt; }

    /** @param invitedAt timestamp when invitation was sent */
    public void setInvitedAt(LocalDateTime invitedAt) { this.invitedAt = invitedAt; }

    /** @return timestamp when invitation was responded to */
    public LocalDateTime getRespondedAt() { return respondedAt; }

    /** @param respondedAt timestamp when invitation was responded to */
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }

    /** @return display name of the home */
    public String getHomeName() { return homeName; }

    /** @param homeName display name of the home */
    public void setHomeName(String homeName) { this.homeName = homeName; }

    /** @return display name of the inviter */
    public String getInviterName() { return inviterName; }

    /** @param inviterName display name of the inviter */
    public void setInviterName(String inviterName) { this.inviterName = inviterName; }

    /** @return role assigned upon acceptance */
    public UserRole getInvitedRole() { return invitedRole; }

    /** @param invitedRole role assigned upon acceptance */
    public void setInvitedRole(UserRole invitedRole) { this.invitedRole = invitedRole; }
}