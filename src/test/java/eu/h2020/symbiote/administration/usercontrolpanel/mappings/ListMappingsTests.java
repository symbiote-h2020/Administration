package eu.h2020.symbiote.administration.usercontrolpanel.mappings;

import eu.h2020.symbiote.administration.exceptions.rabbit.CommunicationException;
import eu.h2020.symbiote.administration.usercontrolpanel.UserControlPanelBaseTestClass;
import eu.h2020.symbiote.core.internal.MappingListResponse;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Test class for use in testing MVC and form validation.
 */
public class ListMappingsTests extends UserControlPanelBaseTestClass {

    @Test
    public void listAllMappings() throws Exception {
        doReturn(sampleMappingListResponseSuccess()).when(rabbitManager).sendGetAllMappingsRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/list_all_mappings")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$.[0].id").value(sampleOntologyMapping().getId()));
    }

    @Test
    public void getMappingsFailure() throws Exception {
        // Failed response
        MappingListResponse response = sampleMappingListResponseFail();
        doReturn(response).when(rabbitManager).sendGetAllMappingsRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/list_all_mappings")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("An error occurred: " + response.getMessage()));
    }

    @Test
    public void getMappingsTimeout() throws Exception {
        // Registry returns null
        doReturn(null).when(rabbitManager).sendGetAllMappingsRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/list_all_mappings")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorMessage").value("An error occurred: Registry unreachable!"));
    }

    @Test
    public void getMappingsCommunicationException() throws Exception {
        // Registry throws CommunicationException
        doThrow(new CommunicationException("error")).when(rabbitManager).sendGetAllMappingsRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/list_all_mappings")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorMessage").value("An error occurred: Registry threw communication exception: error"));
    }
}