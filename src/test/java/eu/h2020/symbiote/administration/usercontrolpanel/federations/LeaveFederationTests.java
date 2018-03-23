package eu.h2020.symbiote.administration.usercontrolpanel.federations;

import eu.h2020.symbiote.administration.usercontrolpanel.UserControlPanelBaseTestClass;
import eu.h2020.symbiote.model.mim.Federation;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;

import static eu.h2020.symbiote.administration.services.FederationNotificationService.FEDERATION_MANAGER_URL;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
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
                .param("platformId", platformId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("The federation does not exist"));
    }

    @Test
    public void platformIsOnlyMemberFederation() throws Exception {
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());

        Federation federation = sampleFederationRequest();
        federation.getMembers().remove(2);
        federation.getMembers().remove(1);
        federationRepository.save(federation);


        mockMvc.perform(post("/administration/user/cpanel/leave_federation")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("federationId", federationId)
                .param("platformId", platformId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Platform " + platformId +
                        " is the only a member of federation " + federationId + ". Please, delete the federation"));
    }

    @Test
    public void success() throws Exception {
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());

        Federation federation = sampleSavedFederation();
        federationRepository.save(federation);

        String platformId1 = federation.getMembers().get(0).getPlatformId();
        String platformId2 = federation.getMembers().get(1).getPlatformId();
        String platformId3 = federation.getMembers().get(2).getPlatformId();
        String platform1Url = federation.getMembers().get(0).getInterworkingServiceURL();
        String platform2Url = federation.getMembers().get(1).getInterworkingServiceURL();
        String platform3Url =federation.getMembers().get(2).getInterworkingServiceURL();

        MockRestServiceServer mockServer =
                MockRestServiceServer.bindTo(restTemplate).build();
        mockServer.expect(requestTo(platform1Url + FEDERATION_MANAGER_URL)).andExpect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.jsonPath("$.id").value(federationId))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members", hasSize(2)))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members[*].platformId", contains(platformId, platformId2)))
                .andRespond(withSuccess());
        mockServer.expect(requestTo(platform2Url + FEDERATION_MANAGER_URL)).andExpect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.jsonPath("$.id").value(federationId))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members", hasSize(2)))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members[*].platformId", contains(platformId, platformId2)))
                .andRespond(withSuccess());
        mockServer.expect(requestTo(platform3Url + FEDERATION_MANAGER_URL)).andExpect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.jsonPath("$.id").value(federationId))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members", hasSize(2)))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members[*].platformId", contains(platformId, platformId2)))
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

        // Reset the original request factory of restTemplate to unbind it from the mockServer
        restTemplate.setRequestFactory(originalRequestFactory);
    }
}