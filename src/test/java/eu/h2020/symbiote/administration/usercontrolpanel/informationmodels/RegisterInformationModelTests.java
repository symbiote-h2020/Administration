package eu.h2020.symbiote.administration.usercontrolpanel.informationmodels;

import eu.h2020.symbiote.administration.exceptions.rabbit.CommunicationException;
import eu.h2020.symbiote.administration.usercontrolpanel.UserControlPanelBaseTestClass;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Test class for use in testing MVC and form validation.
 */
public class RegisterInformationModelTests extends UserControlPanelBaseTestClass {

    private MockMultipartFile invalidFile = new MockMultipartFile("info-model-rdf", "mock.ff",
            "text/plain", "dummy content".getBytes());

    private MockMultipartFile validFile = new MockMultipartFile("info-model-rdf", "mock.ttl",
            "text/plain", informationModelRdf.getBytes());

    private MockMultipartFile validFile2 = new MockMultipartFile("info-model-rdf", "mock.ttl.ttl",
            "text/plain", informationModelRdf.getBytes());

    @Test
    public void validationErrors() throws Exception {
        // Validate information model details

        mockMvc.perform(fileUpload("/administration/user/cpanel/register_information_model")
                .file(invalidFile)
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("info-model-name", "a")
                .param("info-model-uri", "url"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.info_model_reg_error_name").value("The name should have from 2 to 30 characters"))
                .andExpect(jsonPath("$.info_model_reg_error_uri").value("The uri is invalid"))
                .andExpect(jsonPath("$.info_model_reg_error_rdf").value("This format is not supported"))
                .andExpect(jsonPath("$.error").value("Invalid Arguments"));
    }

    @Test
    public void success() throws Exception {
        // Successful information model registration
        doReturn(sampleInformationModelResponseSuccess()).when(rabbitManager).sendRegisterInfoModelRequest(any());

        mockMvc.perform(fileUpload("/administration/user/cpanel/register_information_model")
                .file(validFile)
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("info-model-name", informationModelName)
                .param("info-model-uri", informationModelUri))
                .andExpect(status().isCreated());
    }

    @Test
    public void success2() throws Exception {
        // Successful information model registration
        doReturn(sampleInformationModelResponseSuccess()).when(rabbitManager).sendRegisterInfoModelRequest(any());

        mockMvc.perform(fileUpload("/administration/user/cpanel/register_information_model")
                .file(validFile2)
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("info-model-name", informationModelName)
                .param("info-model-uri", informationModelUri))
                .andExpect(status().isCreated());
    }

    @Test
    public void registryError() throws Exception {
        // Registry returns error
        doReturn(sampleInformationModelResponseFail()).when(rabbitManager).sendRegisterInfoModelRequest(any());

        mockMvc.perform(fileUpload("/administration/user/cpanel/register_information_model")
                .file(validFile)
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("info-model-name", informationModelName)
                .param("info-model-uri", informationModelUri))
                .andExpect(jsonPath("$.error").value(sampleInformationModelResponseFail().getMessage()));
    }

    @Test
    public void registryTimeout() throws Exception {
        // Registry returns null
        doReturn(null).when(rabbitManager).sendRegisterInfoModelRequest(any());

        mockMvc.perform(fileUpload("/administration/user/cpanel/register_information_model")
                .file(validFile)
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("info-model-name", informationModelName)
                .param("info-model-uri", informationModelUri))
                .andExpect(jsonPath("$.error").value("Registry unreachable!"));
    }

    @Test
    public void registryCommunicationException() throws Exception {
        // Registry returns error
        doThrow(new CommunicationException("error")).when(rabbitManager).sendRegisterInfoModelRequest(any());

        mockMvc.perform(fileUpload("/administration/user/cpanel/register_information_model")
                .file(validFile)
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("info-model-name", informationModelName)
                .param("info-model-uri", informationModelUri))
                .andExpect(jsonPath("$.error").value("Registry threw communication exception: error"));

    }
}