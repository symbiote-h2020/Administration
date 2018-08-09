package eu.h2020.symbiote.administration.usercontrolpanel.federations;

import eu.h2020.symbiote.administration.model.FederationWithInvitations;
import eu.h2020.symbiote.administration.usercontrolpanel.UserControlPanelBaseTestClass;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static eu.h2020.symbiote.administration.services.federation.FederationNotificationService.FEDERATION_MANAGER_URL;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Test class for use in testing MVC and form validation.
 */
public class LeaveFederationTests extends UserControlPanelBaseTestClass {

    @Test
    public void doesNotOwnPlatform() throws Exception {
        // The user does not own the platform which tries to leave the federation
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/leave_federation")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("federationId", federationId)
                .param("platformId", "dummy"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("You do not own the platform with id dummy"));
    }


    @Test
    public void federationDoesNotExist() throws Exception {
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/leave_federation")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("federationId", federationId)
                .param("platformId", platform1Id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("The federation does not exist"));
    }

    @Test
    public void platformIsOnlyMemberFederation() throws Exception {
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());

        FederationWithInvitations federation = sampleSavedFederationWithSinglePlatform();
        federationRepository.save(federation);

        String platformId = federation.getMembers().get(0).getPlatformId();
        String platform1Url = federation.getMembers().get(0).getInterworkingServiceURL();

        MockRestServiceServer mockServer =
                MockRestServiceServer.bindTo(restTemplate).build();
        mockServer.expect(requestTo(platform1Url + FEDERATION_MANAGER_URL + "/" + federation.getId()))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(MockRestRequestMatchers.jsonPath("$.id").value(federation.getId()))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members", hasSize(1)))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members[*].platformId",
                        contains(platformId)))
                .andRespond(withSuccess());

        mockMvc.perform(post("/administration/user/cpanel/leave_federation")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("federationId", federation.getId())
                .param("platformId", platformId))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$.deleted").value(true));

        mockServer.verify();

        // Verify that AAM received the message
        while(dummyAAMListener.federationMessagesDeleted() == 0)
            TimeUnit.MILLISECONDS.sleep(100);

        assertEquals(0, dummyAAMListener.federationMessagesCreated());
        assertEquals(0, dummyAAMListener.federationMessagesUpdated());
        assertEquals(1, dummyAAMListener.federationMessagesDeleted());

        // Reset the original request factory of restTemplate to unbind it from the mockServer
        restTemplate.setRequestFactory(originalRequestFactory);
    }

    @Test
    public void success() throws Exception {
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());

        FederationWithInvitations federation = sampleSavedFederation();
        federationRepository.save(federation);
        Date initialDate = federation.getLastModified();

        String platformId1 = federation.getMembers().get(0).getPlatformId();
        String platformId2 = federation.getMembers().get(1).getPlatformId();
        String platformId3 = federation.getMembers().get(2).getPlatformId();
        String platform1Url = federation.getMembers().get(0).getInterworkingServiceURL();
        String platform2Url = federation.getMembers().get(1).getInterworkingServiceURL();
        String platform3Url =federation.getMembers().get(2).getInterworkingServiceURL();

        MockRestServiceServer mockServer =
                MockRestServiceServer.bindTo(restTemplate).build();
        mockServer.expect(requestTo(platform1Url + FEDERATION_MANAGER_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.jsonPath("$.id").value(federationId))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members", hasSize(2)))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members[*].platformId", contains(platform1Id, platformId2)))
                .andRespond(withSuccess());
        mockServer.expect(requestTo(platform2Url + FEDERATION_MANAGER_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.jsonPath("$.id").value(federationId))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members", hasSize(2)))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members[*].platformId", contains(platform1Id, platformId2)))
                .andRespond(withSuccess());
        mockServer.expect(requestTo(platform3Url + FEDERATION_MANAGER_URL + "/" + federation.getId()))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(MockRestRequestMatchers.jsonPath("$.id").value(federationId))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members", hasSize(2)))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members[*].platformId", contains(platform1Id, platformId2)))
                .andRespond(withSuccess());

        mockMvc.perform(post("/administration/user/cpanel/leave_federation")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("federationId", federationId)
                .param("platformId", platformId3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + federationId + ".members.length()").value(2))
                .andExpect(jsonPath("$." + federationId + ".members[*].platformId", contains(platformId1, platformId2)))
                .andExpect(jsonPath("$." + federationId + ".id").value(federationId));

        mockServer.verify();

        List<FederationWithInvitations> federations = federationRepository.findAll();
        assertEquals(1, federations.size());
        assertEquals(federationId, federations.get(0).getId());
        assertNotEquals(initialDate, federations.get(0).getLastModified());

        // Reset the original request factory of restTemplate to unbind it from the mockServer
        restTemplate.setRequestFactory(originalRequestFactory);

        // Verify that AAM received the message
        while(dummyAAMListener.federationMessagesUpdated() == 0)
            TimeUnit.MILLISECONDS.sleep(100);

        assertEquals(0, dummyAAMListener.federationMessagesCreated());
        assertEquals(1, dummyAAMListener.federationMessagesUpdated());
        assertEquals(0, dummyAAMListener.federationMessagesDeleted());
    }
}