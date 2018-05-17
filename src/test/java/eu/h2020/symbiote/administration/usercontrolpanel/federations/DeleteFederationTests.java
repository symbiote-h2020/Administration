package eu.h2020.symbiote.administration.usercontrolpanel.federations;

import eu.h2020.symbiote.administration.model.FederationWithInvitations;
import eu.h2020.symbiote.administration.usercontrolpanel.UserControlPanelBaseTestClass;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;

import java.util.concurrent.TimeUnit;

import static eu.h2020.symbiote.administration.services.federation.FederationNotificationService.FEDERATION_MANAGER_URL;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
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
public class DeleteFederationTests extends UserControlPanelBaseTestClass {

    @Test
    public void doesNotOwnPlatform() throws Exception {
        // The user does not own the platform which tries to leave the federation
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());

        FederationWithInvitations federation = sampleSavedFederation();
        federation.getMembers().remove(2);
        federation.getMembers().remove(0);
        federation.getMembers().get(0).setPlatformId("dummy");
        federationRepository.save(federation);

        mockMvc.perform(post("/administration/user/cpanel/delete_federation")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("federationIdToDelete", federationId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("You do not own the single platform in the federation"));
    }


    @Test
    public void federationDoesNotExist() throws Exception {

        mockMvc.perform(post("/administration/user/cpanel/delete_federation")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("federationIdToDelete", federationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("The federation does not exist"));
    }

    @Test
    public void platformIsNotOnlyMemberFederation() throws Exception {
        FederationWithInvitations federation = sampleSavedFederation();
        federationRepository.save(federation);


        mockMvc.perform(post("/administration/user/cpanel/delete_federation")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("federationIdToDelete", federationId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("There are more than 1 platform in the federations"));
    }

    @Test
    public void success() throws Exception {
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());

        FederationWithInvitations federation = sampleSavedFederationWithSinglePlatform();
        String platformId = federation.getMembers().get(0).getPlatformId();
        String platform1Url = federation.getMembers().get(0).getInterworkingServiceURL();
        federationRepository.save(federation);

        MockRestServiceServer mockServer =
                MockRestServiceServer.bindTo(restTemplate).build();
        mockServer.expect(requestTo(platform1Url + FEDERATION_MANAGER_URL + "/" + federation.getId()))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(MockRestRequestMatchers.jsonPath("$.id").value(federation.getId()))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members", hasSize(1)))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members[*].platformId",
                        contains(platformId)))
                .andRespond(withSuccess());


        mockMvc.perform(post("/administration/user/cpanel/delete_federation")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("federationIdToDelete", federation.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + federation.getId() + ".members.length()").value(1))
                .andExpect(jsonPath("$." + federation.getId() + ".members[*].platformId", contains(platformId)))
                .andExpect(jsonPath("$." + federation.getId() + ".id").value(federation.getId()));

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
}