package eu.h2020.symbiote.administration.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.PersistenceConstructor;

import java.util.Date;
import java.util.Objects;

public class FederationInvitation {

    private final String invitedPlatformId;
    private final InvitationStatus status;
    private final Date invitationDate;
    private final Date handledDate;

    @PersistenceConstructor
    @JsonCreator
    public FederationInvitation(@JsonProperty("invitedPlatformId") String invitedPlatformId,
                                @JsonProperty("status") InvitationStatus status,
                                @JsonProperty("invitationDate") Date invitationDate,
                                @JsonProperty("handledDate") Date handledDate) {
        this.invitedPlatformId = invitedPlatformId;
        this.status = status;
        this.invitationDate = invitationDate;
        this.handledDate = handledDate;
    }

    public FederationInvitation(String invitedPlatformId, InvitationStatus status, Date invitationDate) {
        this(invitedPlatformId, status, invitationDate, null);
    }

    public String getInvitedPlatformId() { return invitedPlatformId; }
    public InvitationStatus getStatus() { return status; }
    public Date getInvitationDate() { return invitationDate; }
    public Date getHandledDate() { return handledDate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FederationInvitation that = (FederationInvitation) o;
        return Objects.equals(getInvitedPlatformId(), that.getInvitedPlatformId());
    }

    @Override
    public int hashCode() {

        return Objects.hash(getInvitedPlatformId());
    }

    @Override
    public String toString() {
        return "FederationInvitation{" +
                "invitedPlatformId='" + invitedPlatformId + '\'' +
                ", status=" + status +
                ", invitationDate=" + invitationDate +
                ", handledDate=" + handledDate +
                '}';
    }

    public enum InvitationStatus {
        PENDING, ACCEPTED, REJECTED;
    }
}
