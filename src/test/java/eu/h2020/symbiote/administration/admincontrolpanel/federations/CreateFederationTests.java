package eu.h2020.symbiote.administration.admincontrolpanel.federations;

import eu.h2020.symbiote.administration.admincontrolpanel.AdminControlPanelBaseTestClass;
import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.administration.model.CreateFederationRequest;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.communication.payloads.FederationRule;
import org.junit.Test;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Test class for use in testing MVC and form validation.
 */
public class CreateFederationTests extends AdminControlPanelBaseTestClass {

    @Test
    public void success() throws Exception {
        // Create Federation successfully
        doReturn(sampleFederationRuleManagementResponse()).when(mockRabbitManager).sendFederationRuleManagementRequest(any());


        mockMvc.perform(post("/administration/admin/cpanel/create_federation")
                .with(authentication(sampleAdminAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleCreateFederationRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Federation Registration was successful!"));
    }

    @Test
    public void notAllThePlatformsInAAMResponse() throws Exception {
        CreateFederationRequest request = sampleCreateFederationRequest();
        // Not both platforms ids are present in AAM response
        request.setPlatform2Id(platformId + "3");

        doReturn(sampleFederationRuleManagementResponse()).when(mockRabbitManager).sendFederationRuleManagementRequest(any());

        mockMvc.perform(post("/administration/admin/cpanel/create_federation")
                .with(authentication(sampleAdminAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Not both platforms ids present in AAM response"));
    }

    @Test
    public void responseContainsMoreThanOneFederationRule() throws Exception {
        // AAM response contains more than 1 federation rule
        Map<String, FederationRule> responseWith2Rules = new HashMap<>();
        responseWith2Rules.put("1", sampleFederationRuleManagementResponse().get(federationRuleId));
        responseWith2Rules.put("2", sampleFederationRuleManagementResponse().get(federationRuleId));

        doReturn(responseWith2Rules).when(mockRabbitManager).sendFederationRuleManagementRequest(any());

        mockMvc.perform(post("/administration/admin/cpanel/create_federation")
                .with(authentication(sampleAdminAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleCreateFederationRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Contains more than 1 Federation rule"));
    }

    @Test
    public void aamTimeout() throws Exception {
        // AAM responds with null
        doReturn(null).when(mockRabbitManager).sendFederationRuleManagementRequest(any());

        mockMvc.perform(post("/administration/admin/cpanel/create_federation")
                .with(authentication(sampleAdminAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleCreateFederationRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("AAM unreachable"));
    }

    @Test
    public void aamCommunicationException() throws Exception {
        // AAM throws CommunicationException
        doThrow(new CommunicationException("error")).when(mockRabbitManager).sendFederationRuleManagementRequest(any());

        mockMvc.perform(post("/administration/admin/cpanel/create_federation")
                .with(authentication(sampleAdminAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleCreateFederationRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("AAM threw communication exception: error"));
    }

    @Test
    public void invalidCreateFederationRequest() throws Exception {
        // Invalid CreateFederationRequest
        CreateFederationRequest request = new CreateFederationRequest();
        request.setId("a");
        request.setPlatform1Id("a");
        request.setPlatform2Id("b");

        mockMvc.perform(post("/administration/admin/cpanel/create_federation")
                .with(authentication(sampleAdminAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Arguments"))
                .andExpect(jsonPath("$.federation_reg_error_id")
                        .value("must match \"^([\\w-][\\w-][\\w-][\\w-]+)\""))
                .andExpect(jsonPath("$.federation_reg_error_platform1Id")
                        .value("must match \"^([\\w-][\\w-][\\w-][\\w-]+)\""))
                .andExpect(jsonPath("$.federation_reg_error_platform2Id")
                        .value("must match \"^([\\w-][\\w-][\\w-][\\w-]+)\""));
    }
}