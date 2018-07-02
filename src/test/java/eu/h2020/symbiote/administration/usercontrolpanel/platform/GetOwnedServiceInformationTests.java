package eu.h2020.symbiote.administration.usercontrolpanel.platform;

import eu.h2020.symbiote.administration.exceptions.rabbit.CommunicationException;
import eu.h2020.symbiote.administration.usercontrolpanel.UserControlPanelBaseTestClass;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.junit.Test;
import org.mockito.AdditionalMatchers;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
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
            return samplePlatformRegistryResponseSuccess(platformId);
        }).when(rabbitManager)
                .sendGetPlatformDetailsMessage(any());

        doAnswer(invocation -> {
            String sspId = (String) invocation.getArguments()[0];
            return sampleSspRegistryResponseSuccess(sspId);
        }).when(rabbitManager)
                .sendGetSSPDetailsMessage(any());

        mockMvc.perform(post("/administration/user/cpanel/list_user_services")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All the owned service details were successfully received"))
                .andExpect(jsonPath("$.availablePlatforms[*].id",
                        containsInAnyOrder(platform1Id, platform2Id, platform3Id, platform4Id)))
                .andExpect(jsonPath("$.unavailablePlatforms", hasSize(0)))
                .andExpect(jsonPath("$.availableSSPs[*].id",
                        containsInAnyOrder(ssp1Id, ssp2Id)))
                .andExpect(jsonPath("$.unavailableSSPs", hasSize(0)));
    }

    @Test
    public void getPartialPlatformInformation() throws Exception {
        // Get all partial platform information from Registry

        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());
        doReturn(samplePlatformRegistryResponseSuccess()).when(rabbitManager)
                .sendGetPlatformDetailsMessage(eq(platform1Id));
        doReturn(samplePlatformResponseFail()).when(rabbitManager)
                .sendGetPlatformDetailsMessage(AdditionalMatchers.not(eq(platform1Id)));
        doReturn(sampleSspRegistryResponseSuccess()).when(rabbitManager)
                .sendGetSSPDetailsMessage(eq(ssp1Id));
        doReturn(sampleSspRegistryResponseFail()).when(rabbitManager)
                .sendGetSSPDetailsMessage(AdditionalMatchers.not(eq(ssp1Id)));

        mockMvc.perform(post("/administration/user/cpanel/list_user_services")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isPartialContent())
                .andExpect(jsonPath("$.message")
                        .value("Could NOT all the service information"))
                .andExpect(jsonPath("$.availablePlatforms.length()").value(1))
                .andExpect(jsonPath("$.availablePlatforms[0].id").value(platform1Id))
                .andExpect(jsonPath("$.unavailablePlatforms.length()").value(3))
                .andExpect(jsonPath("$.unavailablePlatforms",
                        containsInAnyOrder(platform2Name, platform3Name, platform4Name)))
                .andExpect(jsonPath("$.availableSSPs.length()").value(1))
                .andExpect(jsonPath("$.availableSSPs[0].id").value(ssp1Id))
                .andExpect(jsonPath("$.unavailableSSPs.length()").value(1))
                .andExpect(jsonPath("$.unavailableSSPs").value(ssp2Name));
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