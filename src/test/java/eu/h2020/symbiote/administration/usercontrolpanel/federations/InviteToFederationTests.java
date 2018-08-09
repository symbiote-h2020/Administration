package eu.h2020.symbiote.administration.usercontrolpanel.federations;

import eu.h2020.symbiote.administration.exceptions.rabbit.CommunicationException;
import eu.h2020.symbiote.administration.model.FederationInvitation;
import eu.h2020.symbiote.administration.model.FederationWithInvitations;
import eu.h2020.symbiote.administration.model.InvitationRequest;
import eu.h2020.symbiote.administration.usercontrolpanel.UserControlPanelBaseTestClass;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static eu.h2020.symbiote.administration.services.federation.FederationNotificationService.FEDERATION_MANAGER_URL;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Test class for use in testing MVC and form validation.
 */
public class InviteToFederationTests extends UserControlPanelBaseTestClass {

    @Test
    public void federationDoesNotExist() throws Exception {
        InvitationRequest invitationRequest = new InvitationRequest(federationId, new HashSet<>());

        mockMvc.perform(post("/administration/user/cpanel/federation_invite")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(invitationRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("The federation does not exist"));
    }

    @Test
    public void aamThrowsException() throws Exception {
        doThrow(new CommunicationException("error")).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());

        FederationWithInvitations federation = sampleSavedFederation();
        federation.getMembers().remove(2);
        federation.getMembers().remove(0);
        federation.getMembers().get(0).setPlatformId("dummy");
        federationRepository.save(federation);

        InvitationRequest invitationRequest = new InvitationRequest(federationId, new HashSet<>());

        mockMvc.perform(post("/administration/user/cpanel/federation_invite")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(invitationRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("AAM threw CommunicationException: error"));
    }

    @Test
    public void doesNotOwnFederatedPlatform() throws Exception {
        // The user does not own a platform in the federation in order to invite other members
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());

        FederationWithInvitations federation = sampleSavedFederation();
        federation.getMembers().remove(2);
        federation.getMembers().remove(0);
        federation.getMembers().get(0).setPlatformId("dummy");
        federationRepository.save(federation);

        InvitationRequest invitationRequest = new InvitationRequest(federationId, new HashSet<>());

        mockMvc.perform(post("/administration/user/cpanel/federation_invite")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(invitationRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("You do not own any of the federation members in order to invite other platforms"));
    }

    @Test
    public void success() throws Exception {
        // In this test, we invite 1 platform which is owned by the user i.e. platform2Id and one that it is not owned
        // i.e. dummyPlatform. The platform2Id should be added immediately to the Federation members, whereas the
        // other one should just be invited. We invite platform2Id twice, to make sure that it is stored only once as
        // a federation member

        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());

        FederationWithInvitations federation = sampleSavedFederationWithSinglePlatform();
        String platformId = federation.getMembers().get(0).getPlatformId();
        String platformUrl = federation.getMembers().get(0).getInterworkingServiceURL();
        federationRepository.save(federation);

        String dummyPlatform = "dummyPlatform";
        InvitationRequest invitationRequest = new InvitationRequest(
                federation.getId(),
                new HashSet<>(Arrays.asList(platform2Id, platform2Id, dummyPlatform))
                );

        MockRestServiceServer mockServer =
                MockRestServiceServer.bindTo(restTemplate).build();
        mockServer.expect(requestTo(platformUrl + FEDERATION_MANAGER_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.jsonPath("$.id").value(federation.getId()))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members", hasSize(2)))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members[*].platformId",
                        contains(platformId, platform2Id)))
                .andRespond(withSuccess());
        mockServer.expect(requestTo(platform2Url + FEDERATION_MANAGER_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.jsonPath("$.id").value(federation.getId()))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members", hasSize(2)))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members[*].platformId",
                        contains(platformId, platform2Id)))
                .andRespond(withSuccess());

        mockMvc.perform(post("/administration/user/cpanel/federation_invite")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(invitationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + federation.getId() + ".openInvitations.*", hasSize(1)))
                .andExpect(jsonPath("$." + federation.getId() + ".openInvitations." + dummyPlatform).exists())
                .andExpect(jsonPath("$." + federation.getId() + ".members", hasSize(2)))
                .andExpect(jsonPath("$." + federation.getId() + ".members[*].platformId",
                        containsInAnyOrder(platform1Id, platform2Id)))
                .andExpect(jsonPath("$." + federation.getId() + ".members[*].interworkingServiceURL",
                        containsInAnyOrder(platform1Url, platform2Url)));

        mockServer.verify();

        // Check what is store in the database
        List<FederationWithInvitations> federations = federationRepository.findAll();
        assertEquals(1, federations.size());
        assertEquals(2, federations.get(0).getMembers().size());
        assertEquals(platformId, federations.get(0).getMembers().get(0).getPlatformId());
        assertEquals(platform2Id, federations.get(0).getMembers().get(1).getPlatformId());
        assertEquals(platform2Url, federations.get(0).getMembers().get(1).getInterworkingServiceURL());
        assertEquals(1, federations.get(0).getOpenInvitations().size());
        assertTrue(federations.get(0).getOpenInvitations().values().stream()
                .map(FederationInvitation::getInvitedPlatformId).collect(Collectors.toSet())
                .contains(dummyPlatform));

        // Reset the original request factory of restTemplate to unbind it from the mockServer
        restTemplate.setRequestFactory(originalRequestFactory);
    }
}