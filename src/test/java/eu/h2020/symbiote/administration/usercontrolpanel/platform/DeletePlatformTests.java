package eu.h2020.symbiote.administration.usercontrolpanel.platform;

import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.administration.usercontrolpanel.UserControlPanelBaseTestClass;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.junit.Test;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Test class for use in testing MVC and form validation.
 */
public class DeletePlatformTests extends UserControlPanelBaseTestClass {

    @Test
    public void success() throws Exception {
        // Delete Platform Successfully
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doReturn(samplePlatformResponseSuccess()).when(mockRabbitManager)
                .sendPlatformRemovalRequest(any());
        doReturn(samplePlatformManagementResponse(ManagementStatus.OK)).when(mockRabbitManager)
                .sendManagePlatformRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", platformId))
                .andExpect(status().isOk());
    }

    @Test
    public void doesNotOwnPlatform() throws Exception {
        // The user does not own the platform which tries to delete
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        mockMvc.perform(post("/administration/user/cpanel/delete_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", "dummy"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("You do not own the platform with id dummy"));
    }

    @Test
    public void aamTimeout() throws Exception {
        // AAM return null
        doReturn(null).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", platformId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("AAM unreachable"));
    }

    @Test
    public void aamCommunicationException() throws Exception {
        // AAM throws CommunicationException
        doThrow(new CommunicationException("error")).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", platformId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("AAM threw communication exception: error"));
    }

    @Test
    public void registryError() throws Exception {
        // Registry returns error
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doReturn(samplePlatformResponseFail()).when(mockRabbitManager)
                .sendPlatformRemovalRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", platformId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(samplePlatformResponseFail().getMessage()));
    }

    @Test
    public void registryTimeout() throws Exception {
        // Registry returns null
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doReturn(null).when(mockRabbitManager)
                .sendPlatformRemovalRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", platformId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Registry unreachable!"));
    }

    @Test
    public void registryCommunicationException() throws Exception {
        // Registry throws CommunicationException
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doThrow(new CommunicationException("error")).when(mockRabbitManager)
                .sendPlatformRemovalRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", platformId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Registry threw communication exception: error"));
    }

    @Test
    public void aamError() throws Exception {
        // AAM returns error
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doReturn(samplePlatformResponseSuccess()).when(mockRabbitManager)
                .sendPlatformRemovalRequest(any());
        doReturn(samplePlatformManagementResponse(ManagementStatus.ERROR)).when(mockRabbitManager)
                .sendManagePlatformRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", platformId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("AAM says that the Platform does not exist!"));
    }

    @Test
    public void aamTimeout2() throws Exception {
        // AAM returns null
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doReturn(samplePlatformResponseSuccess()).when(mockRabbitManager)
                .sendPlatformRemovalRequest(any());
        doReturn(null).when(mockRabbitManager)
                .sendManagePlatformRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", platformId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("AAM unreachable!"));
    }

    @Test
    public void aamCommunicationException2() throws Exception {
        // Registry throws CommunicationException
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doReturn(samplePlatformResponseSuccess()).when(mockRabbitManager)
                .sendPlatformRemovalRequest(any());
        doThrow(new CommunicationException("error")).when(mockRabbitManager)
                .sendManagePlatformRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", platformId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("AAM threw communication exception: error"));
    }

}