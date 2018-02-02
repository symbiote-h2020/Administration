package eu.h2020.symbiote.administration.usercontrolpanel.federations;

import eu.h2020.symbiote.administration.admincontrolpanel.AdminControlPanelBaseTestClass;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.communication.payloads.FederationRule;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
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
        // We do not check if the user owns the federation
        Map<String, FederationRule> response = new HashMap<>();
        Set<String> platformIds = new HashSet<>();
        platformIds.add(platformId);
        platformIds.add(platformId + '2');
        response.put(federationRuleId, new FederationRule(federationRuleId, platformIds));

        doReturn(sampleFederationRuleManagementResponse()).when(mockRabbitManager).sendReadFederationRequest(any());
        doReturn(response).when(mockRabbitManager).sendUpdateFederationRequest(any());

        mockMvc.perform(post("/administration/admin/cpanel/leave_federation")
                .with(authentication(sampleAdminAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("federationId", federationRuleId)
                .param("platformId", platformId + '3'))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.federationRule.platformIds.length()").value(2))
                .andExpect(jsonPath("$.federationRule.federationId").value(federationRuleId));
    }
}