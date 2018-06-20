package eu.h2020.symbiote.administration.usercontrolpanel.federations;

import eu.h2020.symbiote.administration.admincontrolpanel.AdminControlPanelBaseTestClass;
import eu.h2020.symbiote.administration.model.FederationWithInvitations;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

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
        FederationWithInvitations federation1 = sampleSavedFederation();
        FederationWithInvitations federation2 = sampleSavedFederation();
        federation2.setId(federationId + "2");
        List<FederationWithInvitations> federationList = new ArrayList<>();
        federationList.add(federation1);
        federationList.add(federation2);
        federationRepository.save(federationList);

        mockMvc.perform(post("/administration/user/cpanel/list_federations")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + federationId + ".members.length()").value(3))
                .andExpect(jsonPath("$." + federationId + "2.members.length()").value(3))
                .andExpect(jsonPath("$." + federationId + ".id").value(federationId))
                .andExpect(jsonPath("$." + federationId + "2.id").value(federationId + "2"));
    }


}