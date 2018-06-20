package eu.h2020.symbiote.administration.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.Set;

public class InvitationRequest {

    @NotNull
    private final String federationId;

    @NotNull
    private final Set<String> invitedPlatforms;

    @JsonCreator
    public InvitationRequest(@JsonProperty("federationId") String federationId,
                             @JsonProperty("invitedPlatforms") Set<String> invitedPlatforms) {
        this.federationId = federationId;
        this.invitedPlatforms = invitedPlatforms;
    }

    public String getFederationId() { return federationId; }
    public Set<String> getInvitedPlatforms() { return invitedPlatforms; }

    @Override
    public String toString() {
        return "InvitationRequest{" +
                "federationId='" + federationId + '\'' +
                ", invitedPlatforms=" + invitedPlatforms +
                '}';
    }
}
