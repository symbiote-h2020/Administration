package eu.h2020.symbiote.administration.admincontrolpanel.federations;

import eu.h2020.symbiote.administration.admincontrolpanel.AdminControlPanelBaseTestClass;
import eu.h2020.symbiote.administration.model.FederationWithInvitations;
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
public class DeleteFederationTests extends AdminControlPanelBaseTestClass {

    @Test
    public void success() throws Exception {

        FederationWithInvitations federation = sampleSavedFederation();
        federationRepository.save(federation);

        String platformId2 = federation.getMembers().get(1).getPlatformId();
        String platformId3 = federation.getMembers().get(2).getPlatformId();
        String platform1Url = federation.getMembers().get(0).getInterworkingServiceURL();
        String platform2Url = federation.getMembers().get(1).getInterworkingServiceURL();
        String platform3Url =federation.getMembers().get(2).getInterworkingServiceURL();

        MockRestServiceServer mockServer =
                MockRestServiceServer.bindTo(restTemplate).build();
        mockServer.expect(requestTo(platform1Url + FEDERATION_MANAGER_URL + "/" + federationId))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(MockRestRequestMatchers.jsonPath("$.id").value(federationId))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members", hasSize(3)))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members[*].platformId",
                        contains(platform1Id, platformId2, platformId3)))
                .andRespond(withSuccess());
        mockServer.expect(requestTo(platform2Url + FEDERATION_MANAGER_URL + "/" + federationId))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(MockRestRequestMatchers.jsonPath("$.id").value(federationId))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members", hasSize(3)))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members[*].platformId",
                        contains(platform1Id, platformId2, platformId3)))
                .andRespond(withSuccess());
        mockServer.expect(requestTo(platform3Url + FEDERATION_MANAGER_URL + "/" + federationId))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(MockRestRequestMatchers.jsonPath("$.id").value(federationId))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members", hasSize(3)))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members[*].platformId",
                        contains(platform1Id, platformId2, platformId3)))
                .andRespond(withSuccess());


        mockMvc.perform(post("/administration/admin/cpanel/delete_federation")
                .with(authentication(sampleAdminAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("federationIdToDelete", federationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + federationId + ".members.length()").value(3))
                .andExpect(jsonPath("$." + federationId + ".id").value(federationId));

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
    public void federationNotFound() throws Exception {

        mockMvc.perform(post("/administration/admin/cpanel/delete_federation")
                .with(authentication(sampleAdminAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("federationIdToDelete", federationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("The federation does not exist"));
    }
}