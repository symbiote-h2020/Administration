package eu.h2020.symbiote.administration.usercontrolpanel.informationmodels;

import eu.h2020.symbiote.administration.exceptions.rabbit.CommunicationException;
import eu.h2020.symbiote.administration.usercontrolpanel.UserControlPanelBaseTestClass;
import eu.h2020.symbiote.core.internal.InformationModelListResponse;
import eu.h2020.symbiote.model.mim.InformationModel;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.junit.Test;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Test class for use in testing MVC and form validation.
 */
public class ListInformationModelTests extends UserControlPanelBaseTestClass {

    @Test
    public void listAllInformationModels() throws Exception {
        doReturn(sampleInformationModelListResponseSuccess()).when(rabbitManager).sendListInfoModelsRequest();

        mockMvc.perform(post("/administration/user/cpanel/list_all_info_models")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$.[0].id").value(sampleInformationModel().getId()));
    }

    @Test
    public void listUserInformationModels() throws Exception {
        // Successfully listing user's information model
        InformationModel infoModel = sampleInformationModel();
        infoModel.setOwner("dummy");

        InformationModelListResponse informationModelListResponse = sampleInformationModelListResponseSuccess();
        informationModelListResponse.getBody().add(infoModel);

        doReturn(informationModelListResponse).when(rabbitManager).sendListInfoModelsRequest();

        mockMvc.perform(post("/administration/user/cpanel/list_user_info_models")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$.[0].id").value(sampleInformationModel().getId()));
    }

    @Test
    public void getInformationModelsFailure() throws Exception {
        // Failed response
        doReturn(sampleInformationModelListResponseFail()).when(rabbitManager).sendListInfoModelsRequest();

        mockMvc.perform(post("/administration/user/cpanel/list_user_info_models")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(sampleInformationModelListResponseFail().getMessage()));
    }

    @Test
    public void getInformationModelsTimeout() throws Exception {
        // Registry returns null
        doReturn(null).when(rabbitManager).sendListInfoModelsRequest();

        mockMvc.perform(post("/administration/user/cpanel/list_user_info_models")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Could not retrieve the information models from registry"));
    }

    @Test
    public void getInformationModelsCommunicationException() throws Exception {
        // Registry throws CommunicationException
        doThrow(new CommunicationException("error")).when(rabbitManager).sendListInfoModelsRequest();

        mockMvc.perform(post("/administration/user/cpanel/list_user_info_models")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Communication exception while retrieving the information models: error"));
    }
}