package eu.h2020.symbiote.administration.usercontrolpanel.mappings;

import eu.h2020.symbiote.administration.Mappings;
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
public class RegisterMappingsTests extends UserControlPanelBaseTestClass {

    private MockMultipartFile invalidFile = new MockMultipartFile("definition", "mock.ff",
            "map", Mappings.INVALID_MAPPING.getBytes());

    private MockMultipartFile validFile = new MockMultipartFile("definition", "mock.ttl",
            "map", Mappings.VALID_MAPPING.getBytes());

    @Test
    public void validationErrors() throws Exception {
        // Validate mapping details

        mockMvc.perform(fileUpload("/administration/user/cpanel/register_info_model_mapping")
                .file(invalidFile)
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("name", "a")
                .param("sourceModelId", informationModelId)
                .param("destinationModelId", informationModelId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.info_model_mapping_reg_error_name").value("The name should have from 2 to 30 characters"))
                .andExpect(jsonPath("$.info_model_mapping_reg_error_destination").value("The source id is the same with the destination id"))
                .andExpect(jsonPath("$.info_model_mapping_reg_error_definition").exists())
                .andExpect(jsonPath("$.error").value("Invalid Arguments"));
    }

    @Test
    public void success() throws Exception {
        // Successful information model registration
        doReturn(sampleMappingResponseSuccess()).when(rabbitManager).sendRegisterMappingRequest(any());

        mockMvc.perform(fileUpload("/administration/user/cpanel/register_info_model_mapping")
                .file(validFile)
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("name", ontologyMappingName)
                .param("sourceModelId", informationModelId)
                .param("destinationModelId", informationModelId2))
                .andExpect(status().isCreated());
    }

    @Test
    public void registryError() throws Exception {
        // Registry returns error
        doReturn(sampleMappingResponseFail()).when(rabbitManager).sendRegisterMappingRequest(any());

        mockMvc.perform(fileUpload("/administration/user/cpanel/register_info_model_mapping")
                .file(validFile)
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("name", ontologyMappingName)
                .param("sourceModelId", informationModelId)
                .param("destinationModelId", informationModelId2))
                .andExpect(jsonPath("$.error").value(sampleInformationModelResponseFail().getMessage()));
    }

    @Test
    public void registryTimeout() throws Exception {
        // Registry returns null
        doReturn(null).when(rabbitManager).sendRegisterMappingRequest(any());

        mockMvc.perform(fileUpload("/administration/user/cpanel/register_info_model_mapping")
                .file(validFile)
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("name", ontologyMappingName)
                .param("sourceModelId", informationModelId)
                .param("destinationModelId", informationModelId2))
                .andExpect(jsonPath("$.error").value("Registry unreachable!"));
    }

    @Test
    public void registryCommunicationException() throws Exception {
        // Registry returns error
        doThrow(new CommunicationException("error")).when(rabbitManager).sendRegisterMappingRequest(any());

        mockMvc.perform(fileUpload("/administration/user/cpanel/register_info_model_mapping")
                .file(validFile)
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("name", ontologyMappingName)
                .param("sourceModelId", informationModelId)
                .param("destinationModelId", informationModelId2))
                .andExpect(jsonPath("$.error").value("Registry threw communication exception: error"));

    }
}