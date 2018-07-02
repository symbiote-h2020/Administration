package eu.h2020.symbiote.administration.usercontrolpanel.ssp;

import eu.h2020.symbiote.administration.exceptions.rabbit.CommunicationException;
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
public class DeleteSSPTests extends UserControlPanelBaseTestClass {

    @Test
    public void success() throws Exception {
        // Delete Platform Successfully
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());
        doReturn(sampleSspRegistryResponseSuccess()).when(rabbitManager)
                .sendSmartSpaceRemovalRequest(any());
        doReturn(sampleSmartSpaceManagementResponse(ManagementStatus.OK)).when(rabbitManager)
                .sendManageSSPRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_ssp")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("sspIdToDelete", ssp1Id))
                .andExpect(status().isOk());
    }

    @Test
    public void doesNotOwnPlatform() throws Exception {
        // The user does not own the platform which tries to delete
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_ssp")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("sspIdToDelete", "dummy"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("You do not own the smart space with id dummy"));
    }

    @Test
    public void aamTimeout() throws Exception {
        // AAM return null
        doReturn(null).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_ssp")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("sspIdToDelete", ssp1Id))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("AAM unreachable"));
    }

    @Test
    public void aamCommunicationException() throws Exception {
        // AAM throws CommunicationException
        doThrow(new CommunicationException("error")).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_ssp")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("sspIdToDelete", ssp1Id))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("AAM threw Communication Exception: error"));
    }

    @Test
    public void aamError() throws Exception {
        // AAM returns error
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());
        doReturn(sampleSspRegistryResponseSuccess()).when(rabbitManager)
                .sendSmartSpaceRemovalRequest(any());
        doReturn(sampleSmartSpaceManagementResponse(ManagementStatus.ERROR)).when(rabbitManager)
                .sendManageSSPRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_ssp")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("sspIdToDelete", ssp1Id))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("AAM says that the SSP does not exist!"));
    }

    @Test
    public void aamTimeout2() throws Exception {
        // AAM returns null
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());
        doReturn(sampleSspRegistryResponseSuccess()).when(rabbitManager)
                .sendSmartSpaceRemovalRequest(any());
        doReturn(null).when(rabbitManager)
                .sendManageSSPRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_ssp")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("sspIdToDelete", ssp1Id))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("AAM unreachable!"));
    }

    @Test
    public void aamCommunicationException2() throws Exception {
        // Registry throws CommunicationException
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());
        doReturn(sampleSspRegistryResponseSuccess()).when(rabbitManager)
                .sendSmartSpaceRemovalRequest(any());
        doThrow(new CommunicationException("error")).when(rabbitManager)
                .sendManageSSPRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_ssp")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("sspIdToDelete", ssp1Id))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("AAM threw CommunicationException: error"));
    }

}