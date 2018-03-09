package eu.h2020.symbiote.administration.admincontrolpanel;

import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.junit.Test;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Test class for use in testing MVC and form validation.
 */
public class AdminControlPanelTests extends AdminControlPanelBaseTestClass {

    @Test
    public void getControlPanelDenied() throws Exception {

        mockMvc.perform(get("/administration/admin/cpanel"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/administration/admin/login"));
    }

    @Test
    public void getControlPanelSuccess() throws Exception {

        mockMvc.perform(get("/administration/admin/cpanel")
                .with(authentication(sampleAdminAuth(UserRole.SERVICE_OWNER))) )
            .andExpect(status().isOk());

    }
}