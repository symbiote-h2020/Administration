package eu.h2020.symbiote.administration.usercontrolpanel.platform;

import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.administration.usercontrolpanel.UserControlPanelBaseTestClass;
import eu.h2020.symbiote.security.commons.Certificate;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.communication.payloads.OwnedPlatformDetails;
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
        Set<OwnedPlatformDetails> ownedPlatformDetails = new HashSet<>();
        ownedPlatformDetails.add(new OwnedPlatformDetails(platformId, platformUrl, platformName, new Certificate(), componentCertificates));

        doReturn(ownedPlatformDetails).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doReturn(samplePlatformResponseSuccess()).when(mockRabbitManager)
                .sendGetPlatformDetailsMessage(platformId);

        mockMvc.perform(post("/administration/user/cpanel/list_user_platforms")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All the owned platform details were successfully received"))
                .andExpect(jsonPath("$.availablePlatforms.length()").value(1))
                .andExpect(jsonPath("$.availablePlatforms[0].id").value(platformId));
    }

    @Test
    public void getPartialPlatformInformation() throws Exception {
        // Get all partial platform information from Registry
        Set<OwnedPlatformDetails> ownedPlatformDetailsSet = new HashSet<>();
        ownedPlatformDetailsSet.add(new OwnedPlatformDetails(platformId, platformUrl, platformName, new Certificate(), new HashMap<>()));
        ownedPlatformDetailsSet.add(new OwnedPlatformDetails(platformId + "2", platformUrl, platformName + "2",
                new Certificate(), new HashMap<>()));

        doReturn(ownedPlatformDetailsSet).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doReturn(samplePlatformResponseSuccess()).when(mockRabbitManager)
                .sendGetPlatformDetailsMessage(platformId);
        doReturn(samplePlatformResponseFail()).when(mockRabbitManager)
                .sendGetPlatformDetailsMessage(eq(platformId + "2"));


        mockMvc.perform(post("/administration/user/cpanel/list_user_platforms")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
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
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doReturn(null).when(mockRabbitManager)
                .sendGetPlatformDetailsMessage(any());
        mockMvc.perform(post("/administration/user/cpanel/list_user_platforms")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("Registry unreachable!"));
    }

    @Test
    public void registryCommunicationException() throws Exception {
        // Registry threw communication exception
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doThrow(new CommunicationException("error")).when(mockRabbitManager)
                .sendGetPlatformDetailsMessage(any());
        mockMvc.perform(post("/administration/user/cpanel/list_user_platforms")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("Registry threw CommunicationException: error"));
    }

    @Test
    public void aamTimeout() throws Exception {
        // AAM responds with null
        doReturn(null).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        mockMvc.perform(post("/administration/user/cpanel/list_user_platforms")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("AAM responded with null"));
    }

    @Test
    public void aamCommunicationException() throws Exception {
        // AAM threw communication exception
        doThrow(new CommunicationException("error")).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        mockMvc.perform(post("/administration/user/cpanel/list_user_platforms")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("AAM threw CommunicationException: error"));
    }
}