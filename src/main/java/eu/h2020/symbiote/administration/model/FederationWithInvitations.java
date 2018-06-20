package eu.h2020.symbiote.administration.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.model.mim.Federation;
import eu.h2020.symbiote.model.mim.FederationMember;
import eu.h2020.symbiote.model.mim.InformationModel;
import eu.h2020.symbiote.model.mim.QoSConstraint;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

@Document(collection = "federation")
public class FederationWithInvitations extends Federation {
    private HashMap<String, FederationInvitation> openInvitations;

    @JsonCreator
    public FederationWithInvitations(@JsonProperty("id") String id,
                                     @JsonProperty("name") String name,
                                     @JsonProperty("public") Boolean isPublic,
                                     @JsonProperty("informationModel") InformationModel informationModel,
                                     @JsonProperty("slaConstraints") List<QoSConstraint> slaConstraints,
                                     @JsonProperty("members") List<FederationMember> members,
                                     @JsonProperty("openInvitations") HashMap<String, FederationInvitation> openInvitations) {
        setId(id);
        setName(name);
        setPublic(isPublic);
        setInformationModel(informationModel);
        setSlaConstraints(slaConstraints);
        setMembers(members);
        this.openInvitations = openInvitations != null ? openInvitations : new HashMap<>();
    }

    public HashMap<String, FederationInvitation> getOpenInvitations() { return openInvitations; }

    public void openInvitations(Set<FederationInvitation> invitations) {
        for (FederationInvitation invitation : invitations)
            openInvitations.put(invitation.getInvitedPlatformId(), invitation);
    }

    public void openInvitation(FederationInvitation invitation) {
        openInvitations.put(invitation.getInvitedPlatformId(), invitation);
    }

    public void closeInvitation(FederationInvitation invitation) {
        openInvitations.remove(invitation.getInvitedPlatformId());
    }
}
