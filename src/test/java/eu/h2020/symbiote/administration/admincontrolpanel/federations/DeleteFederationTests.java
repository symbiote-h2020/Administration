package eu.h2020.symbiote.administration.admincontrolpanel.federations;

import eu.h2020.symbiote.administration.admincontrolpanel.AdminControlPanelBaseTestClass;
import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.model.mim.Federation;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Test class for use in testing MVC and form validation.
 */
public class DeleteFederationTests extends AdminControlPanelBaseTestClass {

    @Test
    public void success() throws Exception {
        // Save the federation
        Federation federation = sampleFederationRequest();
        federationRepository.save(federation);

        mockMvc.perform(post("/administration/admin/cpanel/delete_federation")
                .with(authentication(sampleAdminAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("federationIdToDelete", federationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + federationId + ".members.length()").value(3))
                .andExpect(jsonPath("$." + federationId + ".id").value(federationId));
    }

    @Test
    public void federationNotFound() throws Exception {

        mockMvc.perform(post("/administration/admin/cpanel/delete_federation")
                .with(authentication(sampleAdminAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("federationIdToDelete", federationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("The federation was not found"));
    }
}