package eu.h2020.symbiote.administration.usercontrolpanel.platform;

import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.administration.usercontrolpanel.UserControlPanelBaseTestClass;
import eu.h2020.symbiote.security.commons.Certificate;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.communication.payloads.OwnedService;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
public class GetPlatformInformationTests extends UserControlPanelBaseTestClass {

    @Test
    public void getAllPlatformInformation() throws Exception {
        // Get all platform information from Registry
        Map<String, Certificate> componentCertificates = new HashMap<>();
        Set<OwnedService> ownedPlatformDetails = new HashSet<>();
        ownedPlatformDetails.add(new OwnedService(platformId, platformName, OwnedService.ServiceType.PLATFORM,
                platformUrl, null, false, null , new Certificate(), componentCertificates));

        doReturn(ownedPlatformDetails).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());
        doReturn(samplePlatformResponseSuccess()).when(rabbitManager)
                .sendGetPlatformDetailsMessage(platformId);

        mockMvc.perform(post("/administration/user/cpanel/list_user_platforms")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All the owned platform details were successfully received"))
                .andExpect(jsonPath("$.availablePlatforms.length()").value(1))
                .andExpect(jsonPath("$.availablePlatforms[0].id").value(platformId));
    }

    @Test
    public void getPartialPlatformInformation() throws Exception {
        // Get all partial platform information from Registry
        Map<String, Certificate> componentCertificates = new HashMap<>();
        Set<OwnedService> ownedPlatformDetailsSet = new HashSet<>();
        ownedPlatformDetailsSet.add(new OwnedService(platformId, platformName, OwnedService.ServiceType.PLATFORM,
                platformUrl, null, false, null , new Certificate(), componentCertificates));
        ownedPlatformDetailsSet.add(new OwnedService(platformId + "2", platformName + "2", OwnedService.ServiceType.PLATFORM,
                platformUrl, null, false, null , new Certificate(), componentCertificates));

        doReturn(ownedPlatformDetailsSet).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());
        doReturn(samplePlatformResponseSuccess()).when(rabbitManager)
                .sendGetPlatformDetailsMessage(eq(platformId));
        doReturn(samplePlatformResponseFail()).when(rabbitManager)
                .sendGetPlatformDetailsMessage(eq(platformId + "2"));


        mockMvc.perform(post("/administration/user/cpanel/list_user_platforms")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isPartialContent())
                .andExpect(jsonPath("$.message")
                        .value("Could NOT retrieve information from Registry for the following platform that you own: "
                                + platformName + "2"))
                .andExpect(jsonPath("$.availablePlatforms.length()").value(1))
                .andExpect(jsonPath("$.availablePlatforms[0].id").value(platformId));
    }

    @Test
    public void registryUnreachable() throws Exception {
        // Registry unreachable
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());
        doReturn(null).when(rabbitManager)
                .sendGetPlatformDetailsMessage(any());
        mockMvc.perform(post("/administration/user/cpanel/list_user_platforms")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("Registry unreachable!"));
    }

    @Test
    public void registryCommunicationException() throws Exception {
        // Registry threw communication exception
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());
        doThrow(new CommunicationException("error")).when(rabbitManager)
                .sendGetPlatformDetailsMessage(any());
        mockMvc.perform(post("/administration/user/cpanel/list_user_platforms")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("Registry threw CommunicationException: error"));
    }

    @Test
    public void aamTimeout() throws Exception {
        // AAM responds with null
        doReturn(null).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());
        mockMvc.perform(post("/administration/user/cpanel/list_user_platforms")
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
        mockMvc.perform(post("/administration/user/cpanel/list_user_platforms")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("AAM threw CommunicationException: error"));
    }
}