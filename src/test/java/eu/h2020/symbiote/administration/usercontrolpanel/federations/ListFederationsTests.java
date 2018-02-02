package eu.h2020.symbiote.administration.usercontrolpanel.federations;

import eu.h2020.symbiote.administration.admincontrolpanel.AdminControlPanelBaseTestClass;
import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.junit.Test;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Test class for use in testing MVC and form validation.
 */
public class ListFederationsTests extends AdminControlPanelBaseTestClass {

    @Test
    public void success() throws Exception {
        // Successfully listing Federations
        doReturn(sampleFederationRuleManagementResponse()).when(mockRabbitManager).sendReadFederationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/list_federations")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + federationRuleId + ".platformIds.length()").value(3))
                .andExpect(jsonPath("$." + federationRuleId + ".federationId").value(federationRuleId));
    }

    @Test
    public void aamTimeout() throws Exception {
        // AAM responds with null
        doReturn(null).when(mockRabbitManager).sendReadFederationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/list_federations")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("AAM unreachable during ListFederationRequest"));
    }

    @Test
    public void aamCommunicationException() throws Exception {
        // AAM throws CommunicationException
        doThrow(new CommunicationException("error")).when(mockRabbitManager).sendReadFederationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/list_federations")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error")
                        .value("AAM threw communication exception during ListFederationRequest: error"));
    }

}