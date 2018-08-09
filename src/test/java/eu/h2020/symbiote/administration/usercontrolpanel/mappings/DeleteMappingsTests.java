package eu.h2020.symbiote.administration.usercontrolpanel.mappings;

import eu.h2020.symbiote.administration.exceptions.rabbit.CommunicationException;
import eu.h2020.symbiote.administration.usercontrolpanel.UserControlPanelBaseTestClass;
import eu.h2020.symbiote.core.cci.InfoModelMappingResponse;
import eu.h2020.symbiote.core.internal.MappingListResponse;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.junit.Ignore;
import org.junit.Test;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Test class for use in testing MVC and form validation.
 */
public class DeleteMappingsTests extends UserControlPanelBaseTestClass {

    @Test
    public void couldNotRetrieveInfoMappings() throws Exception {
        // Could not get Information mappings from Registry
        doReturn(null).when(rabbitManager).sendGetSingleMappingsRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_info_model_mapping")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("mappingIdToDelete", ontologyMappingId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorMessage").value("An error occurred: Registry unreachable!"));
    }

    @Test
    public void couldNotFindInfoMappings() throws Exception {
        // Could not get Information mappings from Registry
        MappingListResponse response = sampleMappingListResponseFail();
        doReturn(sampleMappingListResponseFail()).when(rabbitManager).sendGetSingleMappingsRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_info_model_mapping")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("mappingIdToDelete", ontologyMappingId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("An error occurred: " + response.getMessage()));
    }
    
    @Ignore
    @Test
    public void doesNotOwnMapping() throws Exception {
        // The user does not own the information mapping which tried to delete
        doReturn(sampleMappingListResponseSuccess()).when(rabbitManager).sendGetSingleMappingsRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_info_model_mapping")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("mappingIdToDelete", "dummyId"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("You do not own the Information Mapping that you tried to delete"));
    }

    @Test
    public void success() throws Exception {
        // Delete information mapping successfully
        doReturn(sampleMappingListResponseSuccess()).when(rabbitManager).sendGetSingleMappingsRequest(any());
        doReturn(sampleMappingResponseSuccess()).when(rabbitManager).sendDeleteMappingRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_info_model_mapping")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("mappingIdToDelete", ontologyMappingId))
                .andExpect(status().isOk());
    }

    @Test
    public void registryError() throws Exception {
        // Registry returns error
        InfoModelMappingResponse response = sampleMappingResponseFail();
        doReturn(sampleMappingListResponseSuccess()).when(rabbitManager).sendGetSingleMappingsRequest(any());
        doReturn(response).when(rabbitManager).sendDeleteMappingRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_info_model_mapping")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("mappingIdToDelete", ontologyMappingId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("An error occurred: " + response.getMessage()));
    }

    @Test
    public void registryTimeout() throws Exception {
        // Registry returns null
        doReturn(sampleMappingListResponseSuccess()).when(rabbitManager).sendGetSingleMappingsRequest(any());
        doReturn(null).when(rabbitManager).sendDeleteMappingRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_info_model_mapping")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("mappingIdToDelete", ontologyMappingId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorMessage").value("An error occurred: Registry unreachable!"));
    }

    @Test
    public void registryCommunicationException() throws Exception {
        // Registry throws CommunicationException
        doReturn(sampleMappingListResponseSuccess()).when(rabbitManager).sendGetSingleMappingsRequest(any());
        doThrow(new CommunicationException("error")).when(rabbitManager).sendDeleteMappingRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_info_model_mapping")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("mappingIdToDelete", ontologyMappingId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorMessage").value("An error occurred: Registry threw communication exception: error"));
    }
}