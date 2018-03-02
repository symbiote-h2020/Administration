package eu.h2020.symbiote.administration.admincontrolpanel.federations;

import eu.h2020.symbiote.administration.admincontrolpanel.AdminControlPanelBaseTestClass;
import eu.h2020.symbiote.model.mim.Federation;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.junit.Test;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Test class for use in testing MVC and form validation.
 */
public class LeaveFederationAdminTests extends AdminControlPanelBaseTestClass {

    @Test
    public void success() throws Exception {
        Federation federation = sampleFederationRequest();
        federationRepository.save(federation);

        mockMvc.perform(post("/administration/admin/cpanel/leave_federation")
                .with(authentication(sampleAdminAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("federationId", federationId)
                .param("platformId", platformId + '3'))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + federationId + ".members.length()").value(2))
                .andExpect(jsonPath("$." + federationId + ".id").value(federationId));
    }
}