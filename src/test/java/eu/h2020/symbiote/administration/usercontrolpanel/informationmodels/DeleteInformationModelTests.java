package eu.h2020.symbiote.administration.usercontrolpanel.informationmodels;

import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.administration.usercontrolpanel.UserControlPanelBaseTestClass;
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
public class DeleteInformationModelTests extends UserControlPanelBaseTestClass {

    @Test
    public void couldNotRetrieveInfoModels() throws Exception {
        // Could not get Information models from Registry
        doReturn(null).when(mockRabbitManager).sendListInfoModelsRequest();

        mockMvc.perform(post("/administration/user/cpanel/delete_information_model")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("infoModelIdToDelete", informationModelId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Could not retrieve the information models from registry"));
    }

    @Test
    public void doesNotModel() throws Exception {
        // The user does not own the information model which tried to delete
        doReturn(sampleInformationModelListResponseSuccess()).when(mockRabbitManager).sendListInfoModelsRequest();

        mockMvc.perform(post("/administration/user/cpanel/delete_information_model")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("infoModelIdToDelete", "dummyid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("You do not own the Information Model that you tried to delete"));
    }

    @Test
    public void success() throws Exception {
        // Delete information model successfully
        doReturn(sampleInformationModelListResponseSuccess()).when(mockRabbitManager).sendListInfoModelsRequest();
        doReturn(sampleInformationModelResponseSuccess()).when(mockRabbitManager).sendDeleteInfoModelRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_information_model")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("infoModelIdToDelete", informationModelId))
                .andExpect(status().isOk());
    }

    @Test
    public void registryError() throws Exception {
        // Registry returns error
        doReturn(sampleInformationModelListResponseSuccess()).when(mockRabbitManager).sendListInfoModelsRequest();
        doReturn(sampleInformationModelResponseFail()).when(mockRabbitManager).sendDeleteInfoModelRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_information_model")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("infoModelIdToDelete", informationModelId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(sampleInformationModelResponseFail().getMessage()));
    }

    @Test
    public void registryTimeout() throws Exception {
        // Registry returns null
        doReturn(sampleInformationModelListResponseSuccess()).when(mockRabbitManager).sendListInfoModelsRequest();
        doReturn(null).when(mockRabbitManager).sendDeleteInfoModelRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_information_model")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("infoModelIdToDelete", informationModelId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Registry unreachable!"));
    }

    @Test
    public void registryCommunicationException() throws Exception {
        // Registry throws CommunicationException
        doReturn(sampleInformationModelListResponseSuccess()).when(mockRabbitManager).sendListInfoModelsRequest();
        doThrow(new CommunicationException("error")).when(mockRabbitManager).sendDeleteInfoModelRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_information_model")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("infoModelIdToDelete", informationModelId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Registry threw communication exception: error"));
    }
}