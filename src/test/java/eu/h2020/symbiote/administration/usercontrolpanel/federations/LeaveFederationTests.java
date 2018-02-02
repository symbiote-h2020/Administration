package eu.h2020.symbiote.administration.usercontrolpanel.federations;

import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.administration.usercontrolpanel.UserControlPanelBaseTestClass;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.communication.payloads.FederationRule;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Test class for use in testing MVC and form validation.
 */
public class LeaveFederationTests extends UserControlPanelBaseTestClass {

    @Test
    public void doesNotOwnPlatform() throws Exception {
        // The user does not own the platform which tries to leave the federation
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        mockMvc.perform(post("/administration/user/cpanel/leave_federation")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("federationId", federationRuleId)
                .param("platformId", "dummy"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("You do not own the platform with id dummy"));
    }

    @Test
    public void aamTimeout() throws Exception {
        // AAM responds with null
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doReturn(null).when(mockRabbitManager).sendReadFederationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/leave_federation")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("federationId", federationRuleId)
                .param("platformId", platformId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("AAM unreachable during ListFederationRequest"));
    }

    @Test
    public void aamCommunicationException() throws Exception {
        // AAM throws CommunicationException
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doThrow(new CommunicationException("error")).when(mockRabbitManager).sendReadFederationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/leave_federation")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("federationId", federationRuleId)
                .param("platformId", platformId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("AAM threw communication exception during ListFederationRequest: error"));
    }

    @Test
    public void platformNotInFederation() throws Exception {
        // The specified platform is not a member of the federation
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doReturn(sampleFederationRuleManagementResponse()).when(mockRabbitManager).sendReadFederationRequest(any());

        String newPlatformId = platformId + "4";

        mockMvc.perform(post("/administration/user/cpanel/leave_federation")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("federationId", federationRuleId)
                .param("platformId", newPlatformId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Platform " + newPlatformId + " is not a member of federation federation_rule_id"));
    }

    @Test
    public void platformIsOnlyMemberFederation() throws Exception {
        // The specified platform is the only member of the federation
        Map<String, FederationRule> response = new HashMap<>();
        Set<String> platformIds = new HashSet<>();
        platformIds.add(platformId);
        response.put(federationRuleId, new FederationRule(federationRuleId, platformIds));

        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doReturn(response).when(mockRabbitManager).sendReadFederationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/leave_federation")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("federationId", federationRuleId)
                .param("platformId", platformId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Platform " + platformId +
                        " is the only a member of federation " + federationRuleId + ". Please, delete the federation"));
    }

    @Test
    public void notAllPlatformIdsPresent() throws Exception {
        // Not all the platform ids are present in AAM response
        Map<String, FederationRule> response = new HashMap<>();
        response.put(federationRuleId, sampleFederationRule());

        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doReturn(sampleFederationRuleManagementResponse()).when(mockRabbitManager).sendReadFederationRequest(any());
        doReturn(response).when(mockRabbitManager).sendUpdateFederationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/leave_federation")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("federationId", federationRuleId)
                .param("platformId", platformId + '3'))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Not all the platforms ids present in AAM response"));
    }

    @Test
    public void responseContainsMoreThanOneFederationRule() throws Exception {
        // AAM response contains more than 1 federation rule
        Map<String, FederationRule> responseWith2Rules = new HashMap<>();
        responseWith2Rules.put("1", sampleFederationRuleManagementResponse().get(federationRuleId));
        responseWith2Rules.put("2", sampleFederationRuleManagementResponse().get(federationRuleId));

        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doReturn(sampleFederationRuleManagementResponse()).when(mockRabbitManager).sendReadFederationRequest(any());
        doReturn(responseWith2Rules).when(mockRabbitManager).sendUpdateFederationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/leave_federation")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("federationId", federationRuleId)
                .param("platformId", platformId + '3'))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Contains 2 Federation rules"));

    }

    @Test
    public void success() throws Exception {
        Map<String, FederationRule> response = new HashMap<>();
        Set<String> platformIds = new HashSet<>();
        platformIds.add(platformId);
        platformIds.add(platformId + '2');
        response.put(federationRuleId, new FederationRule(federationRuleId, platformIds));

        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doReturn(sampleFederationRuleManagementResponse()).when(mockRabbitManager).sendReadFederationRequest(any());
        doReturn(response).when(mockRabbitManager).sendUpdateFederationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/leave_federation")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("federationId", federationRuleId)
                .param("platformId", platformId + '3'))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.federationRule.platformIds.length()").value(2))
                .andExpect(jsonPath("$.federationRule.federationId").value(federationRuleId));
    }
}