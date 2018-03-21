package eu.h2020.symbiote.administration.usercontrolpanel.platform;

import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.administration.usercontrolpanel.UserControlPanelBaseTestClass;
import eu.h2020.symbiote.security.commons.Certificate;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.communication.payloads.OwnedService;
import org.junit.Test;
import org.mockito.AdditionalMatchers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Test class for use in testing MVC and form validation.
 */
public class GetOwnedServiceInformationTests extends UserControlPanelBaseTestClass {

    @Test
    public void getAllServiceInformation() throws Exception {

        // Get all platform information from Registry
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());

        doAnswer(invocation -> {
            String platformId = (String) invocation.getArguments()[0];
            return samplePlatformResponseSuccess(platformId);
        }).when(rabbitManager)
                .sendGetPlatformDetailsMessage(any());

        mockMvc.perform(post("/administration/user/cpanel/list_user_services")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All the owned service details were successfully received"))
                .andExpect(jsonPath("$.availablePlatforms[*].id",
                        containsInAnyOrder(platformId, platformId + "2", platformId + "3", platformId + "4")))
                .andExpect(jsonPath("$.unavailablePlatforms", hasSize(0)))
                .andExpect(jsonPath("$.availableSSPs[*].id",
                        containsInAnyOrder(sspId, sspId + "2")))
                .andExpect(jsonPath("$.unavailableSSPs", hasSize(0)));
    }

    @Test
    public void getPartialPlatformInformation() throws Exception {
        // Get all partial platform information from Registry

        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());
        doReturn(samplePlatformResponseSuccess()).when(rabbitManager)
                .sendGetPlatformDetailsMessage(eq(platformId));
        doReturn(samplePlatformResponseFail()).when(rabbitManager)
                .sendGetPlatformDetailsMessage(AdditionalMatchers.not(eq(platformId)));


        mockMvc.perform(post("/administration/user/cpanel/list_user_services")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isPartialContent())
                .andExpect(jsonPath("$.message")
                        .value("Could NOT all the service information"))
                .andExpect(jsonPath("$.availablePlatforms.length()").value(1))
                .andExpect(jsonPath("$.availablePlatforms[0].id").value(platformId))
                .andExpect(jsonPath("$.availableSSPs[*].id",
                        containsInAnyOrder(sspId, sspId + "2")))
                .andExpect(jsonPath("$.unavailableSSPs", hasSize(0)));
    }

    @Test
    public void aamTimeout() throws Exception {
        // AAM responds with null
        doReturn(null).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());
        mockMvc.perform(post("/administration/user/cpanel/list_user_services")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("AAM responded with null"));
    }

    @Test
    public void aamCommunicationException() throws Exception {
        // AAM threw communication exception
        doThrow(new CommunicationException("error")).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());
        mockMvc.perform(post("/administration/user/cpanel/list_user_services")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("AAM threw CommunicationException: error"));
    }
}