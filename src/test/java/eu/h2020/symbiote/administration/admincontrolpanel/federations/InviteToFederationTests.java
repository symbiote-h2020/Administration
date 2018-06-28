package eu.h2020.symbiote.administration.admincontrolpanel.federations;

import eu.h2020.symbiote.administration.admincontrolpanel.AdminControlPanelBaseTestClass;
import eu.h2020.symbiote.administration.model.FederationInvitation;
import eu.h2020.symbiote.administration.model.FederationWithInvitations;
import eu.h2020.symbiote.administration.model.InvitationRequest;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.junit.Test;
import org.springframework.http.MediaType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Test class for use in testing MVC and form validation.
 */
public class InviteToFederationTests extends AdminControlPanelBaseTestClass {

    @Test
    public void success() throws Exception {

        FederationWithInvitations federation = sampleSavedFederationWithSinglePlatform();
        String platformId = federation.getMembers().get(0).getPlatformId();
        federationRepository.save(federation);

        InvitationRequest invitationRequest = new InvitationRequest(
                federation.getId(),
                new HashSet<>(Arrays.asList(platform2Id, platform3Id))
        );

        mockMvc.perform(post("/administration/admin/cpanel/federation_invite")
                .with(authentication(sampleAdminAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(invitationRequest)))
                .andExpect(status().isOk());

        // Check what is store in the database
        List<FederationWithInvitations> federations = federationRepository.findAll();
        assertEquals(1, federations.size());
        assertEquals(1, federations.get(0).getMembers().size());
        assertEquals(platformId, federations.get(0).getMembers().get(0).getPlatformId());
        assertEquals(2, federations.get(0).getOpenInvitations().size());
        assertTrue(federations.get(0).getOpenInvitations().values().stream()
                .map(FederationInvitation::getInvitedPlatformId).collect(Collectors.toSet())
                .containsAll(Arrays.asList(platform2Id, platform3Id)));
    }

    @Test
    public void federationDoesNotExist() throws Exception {
        InvitationRequest invitationRequest = new InvitationRequest(federationId, new HashSet<>());

        mockMvc.perform(post("/administration/admin/cpanel/federation_invite")
                .with(authentication(sampleAdminAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(invitationRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("The federation does not exist"));
    }
}