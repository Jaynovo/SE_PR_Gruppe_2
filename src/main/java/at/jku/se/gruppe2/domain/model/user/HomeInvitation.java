package at.jku.se.gruppe2.domain.model.user;

import java.time.LocalDateTime;

public class HomeInvitation {

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
    public HomeInvitation() {
        this.status = Status.PENDING;
        this.invitedAt = LocalDateTime.now();
    }

    public HomeInvitation(int homeId, int inviterUserId, String inviteeEmail) {
        this();
        this.homeId = homeId;
        this.inviterUserId = inviterUserId;
        this.inviteeEmail = inviteeEmail;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getHomeId() {
        return homeId;
    }

    public void setHomeId(int homeId) {
        this.homeId = homeId;
    }

    public int getInviterUserId() {
        return inviterUserId;
    }

    public void setInviterUserId(int inviterUserId) {
        this.inviterUserId = inviterUserId;
    }

    public String getInviteeEmail() {
        return inviteeEmail;
    }

    public void setInviteeEmail(String inviteeEmail) {
        this.inviteeEmail = inviteeEmail;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getInvitedAt() {
        return invitedAt;
    }

    public void setInvitedAt(LocalDateTime invitedAt) {
        this.invitedAt = invitedAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    public String getHomeName() {
        return homeName;
    }

    public void setHomeName(String homeName) {
        this.homeName = homeName;
    }

    public String getInviterName() {
        return inviterName;
    }

    public void setInviterName(String inviterName) {
        this.inviterName = inviterName;
    }

    public UserRole getInvitedRole() {
        return invitedRole;
    }

    public void setInvitedRole(UserRole invitedRole) {
        this.invitedRole = invitedRole;
    }
}