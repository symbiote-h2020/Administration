package eu.h2020.symbiote.administration.usercontrolpanel.federations;

import eu.h2020.symbiote.administration.exceptions.rabbit.CommunicationException;
import eu.h2020.symbiote.administration.model.FederationInvitation;
import eu.h2020.symbiote.administration.model.FederationWithInvitations;
import eu.h2020.symbiote.administration.usercontrolpanel.UserControlPanelBaseTestClass;
import eu.h2020.symbiote.model.mim.FederationMember;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static eu.h2020.symbiote.administration.services.federation.FederationNotificationService.FEDERATION_MANAGER_URL;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
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
public class HandleFederationInvitationsTests extends UserControlPanelBaseTestClass {

    @Test
    public void federationDoesNotExist() throws Exception {
        mockMvc.perform(post("/administration/user/cpanel/federation/handleInvitation")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("federationId", federationId)
                .param("platformId", platform2Id)
                .param("accepted", "true"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("The federation does not exist"));
    }

    @Test
    public void aamThrowsException() throws Exception {
        doThrow(new CommunicationException("error")).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());

        FederationWithInvitations federation = sampleSavedFederation();
        federationRepository.save(federation);

        mockMvc.perform(post("/administration/user/cpanel/federation/handleInvitation")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("federationId", federationId)
                .param("platformId", platform2Id)
                .param("accepted", "true"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("AAM threw Communication Exception: error"));
    }

    @Test
    public void doesNotOwnPlatform() throws Exception {
        // The user does not own the platform in order to accept the invitation
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());

        String invitedMemberId = "invitedMemberId";
        FederationWithInvitations federation = sampleSavedFederation();
        federation.openInvitation(new FederationInvitation(
                invitedMemberId,
                FederationInvitation.InvitationStatus.PENDING,
                new Date()));
        federationRepository.save(federation);

        mockMvc.perform(post("/administration/user/cpanel/federation/handleInvitation")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("federationId", federationId)
                .param("platformId", invitedMemberId)
                .param("accepted", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("You do not own the platform with id invitedMemberId"));
    }

    @Test
    public void rejectSuccess() throws Exception {
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());

        String invitedMemberId = platform2Id;
        FederationWithInvitations federation = sampleSavedFederationWithSinglePlatform();
        federation.openInvitation(new FederationInvitation(
                invitedMemberId,
                FederationInvitation.InvitationStatus.PENDING,
                new Date()));
        federationRepository.save(federation);

        List<FederationWithInvitations> federations = federationRepository.findAll();
        assertEquals(1, federations.size());
        assertEquals(1, federations.get(0).getMembers().size());
        assertEquals(1, federations.get(0).getOpenInvitations().size());

        mockMvc.perform(post("/administration/user/cpanel/federation/handleInvitation")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("federationId", federation.getId())
                .param("platformId", invitedMemberId)
                .param("accepted", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + federation.getId() + ".openInvitations").isEmpty())
                .andExpect(jsonPath("$." + federation.getId() + ".members", hasSize(1)))
                .andExpect(jsonPath("$." + federation.getId() + ".members[*].platformId", contains(platform1Id)));

        // Test what is stored in the database
        federations = federationRepository.findAll();
        assertEquals(1, federations.size());
        assertEquals(0, federations.get(0).getOpenInvitations().size());
    }

    @Test
    public void acceptSuccess() throws Exception {
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());

        String invitedMemberId = platform4Id;
        FederationWithInvitations federation = sampleSavedFederation();
        federation.openInvitation(new FederationInvitation(
                invitedMemberId,
                FederationInvitation.InvitationStatus.PENDING,
                new Date()));
        federationRepository.save(federation);
        Date initialDate = federation.getLastModified();

        List<FederationWithInvitations> federations = federationRepository.findAll();
        assertEquals(1, federations.size());
        assertEquals(3, federations.get(0).getMembers().size());
        assertTrue(federations.get(0).getMembers().stream()
                .map(FederationMember::getPlatformId)
                .collect(Collectors.toSet())
                .containsAll(new ArrayList<>(Arrays.asList(platform1Id, platform2Id, platform3Id))));
        assertEquals(1, federations.get(0).getOpenInvitations().size());

        MockRestServiceServer mockServer =
                MockRestServiceServer.bindTo(restTemplate).build();
        mockServer.expect(requestTo(platform1Url + FEDERATION_MANAGER_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.jsonPath("$.id").value(federationId))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members", hasSize(4)))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members[*].platformId",
                        containsInAnyOrder(platform1Id, platform2Id, platform3Id, platform4Id)))
                .andRespond(withSuccess());
        mockServer.expect(requestTo(platform2Url + FEDERATION_MANAGER_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.jsonPath("$.id").value(federationId))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members", hasSize(4)))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members[*].platformId",
                        containsInAnyOrder(platform1Id, platform2Id, platform3Id, platform4Id)))
                .andRespond(withSuccess());
        mockServer.expect(requestTo(platform3Url + FEDERATION_MANAGER_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.jsonPath("$.id").value(federationId))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members", hasSize(4)))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members[*].platformId",
                        containsInAnyOrder(platform1Id, platform2Id, platform3Id, platform4Id)))
                .andRespond(withSuccess());
        mockServer.expect(requestTo(platform4Url + FEDERATION_MANAGER_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.jsonPath("$.id").value(federationId))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members", hasSize(4)))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members[*].platformId",
                        containsInAnyOrder(platform1Id, platform2Id, platform3Id, platform4Id)))
                .andRespond(withSuccess());
        mockMvc.perform(post("/administration/user/cpanel/federation/handleInvitation")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("federationId", federationId)
                .param("platformId", invitedMemberId)
                .param("accepted", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + federationId + ".openInvitations").isEmpty())
                .andExpect(jsonPath("$." + federationId + ".members", hasSize(4)))
                .andExpect(jsonPath("$." + federationId + ".members[*].platformId",
                        containsInAnyOrder(platform1Id, platform2Id, platform3Id, invitedMemberId)));

        mockServer.verify();

        // Test what is stored in the database
        federations = federationRepository.findAll();
        assertEquals(1, federations.size());
        assertNotEquals(initialDate, federations.get(0).getLastModified());
        assertEquals(4, federations.get(0).getMembers().size());
        assertTrue(federations.get(0).getMembers().stream()
                .map(FederationMember::getPlatformId)
                .collect(Collectors.toSet())
                .containsAll(new ArrayList<>(Arrays.asList(platform1Id, platform2Id, platform3Id, invitedMemberId))));
        assertEquals(0, federations.get(0).getOpenInvitations().size());


        // Verify that AAM received the messages
        while(dummyAAMListener.federationMessagesUpdated() == 0)
            TimeUnit.MILLISECONDS.sleep(100);

        assertEquals(0, dummyAAMListener.federationMessagesCreated());
        assertEquals(1, dummyAAMListener.federationMessagesUpdated());
        assertEquals(0, dummyAAMListener.federationMessagesDeleted());

        // Reset the original request factory of restTemplate to unbind it from the mockServer
        restTemplate.setRequestFactory(originalRequestFactory);
    }
}