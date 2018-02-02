package eu.h2020.symbiote.administration.usercontrolpanel.federations;

import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.administration.model.CreateFederationRequest;
import eu.h2020.symbiote.administration.model.PlatformId;
import eu.h2020.symbiote.administration.usercontrolpanel.UserControlPanelBaseTestClass;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.communication.payloads.FederationRule;
import org.junit.Test;
import org.springframework.http.MediaType;

import java.util.ArrayList;
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
public class CreateFederationTests extends UserControlPanelBaseTestClass {

    @Test
    public void success() throws Exception {
        // Create Federation successfully
        doReturn(sampleFederationRuleManagementResponse()).when(mockRabbitManager).sendCreateFederationRequest(any());


        mockMvc.perform(post("/administration/user/cpanel/create_federation")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleCreateFederationRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Federation Registration was successful!"));
    }

    @Test
    public void notAllThePlatformsInAAMResponse() throws Exception {
        CreateFederationRequest request = sampleCreateFederationRequest();
        // Not both platforms ids are present in AAM response
        request.getPlatforms().remove(new PlatformId(platformId + '2'));
        request.getPlatforms().add(new PlatformId(platformId + "3"));

        doReturn(sampleFederationRuleManagementResponse()).when(mockRabbitManager).sendCreateFederationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/create_federation")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Not all the platforms ids present in AAM response"));
    }

    @Test
    public void responseContainsMoreThanOneFederationRule() throws Exception {
        // AAM response contains more than 1 federation rule
        Map<String, FederationRule> responseWith2Rules = new HashMap<>();
        responseWith2Rules.put("1", sampleFederationRuleManagementResponse().get(federationRuleId));
        responseWith2Rules.put("2", sampleFederationRuleManagementResponse().get(federationRuleId));

        doReturn(responseWith2Rules).when(mockRabbitManager).sendCreateFederationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/create_federation")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleCreateFederationRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Contains more than 1 Federation rule"));
    }

    @Test
    public void aamTimeout() throws Exception {
        // AAM responds with null
        doReturn(null).when(mockRabbitManager).sendCreateFederationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/create_federation")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleCreateFederationRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("AAM unreachable"));
    }

    @Test
    public void aamCommunicationException() throws Exception {
        // AAM throws CommunicationException
        doThrow(new CommunicationException("error")).when(mockRabbitManager).sendCreateFederationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/create_federation")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleCreateFederationRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("AAM threw communication exception: error"));
    }

    @Test
    public void invalidCreateFederationRequest() throws Exception {
        // Invalid CreateFederationRequest
        ArrayList<PlatformId> platformIds = new ArrayList<>();
        platformIds.add(new PlatformId("a"));
        platformIds.add(new PlatformId("b"));
        CreateFederationRequest request = new CreateFederationRequest("a", platformIds);

        mockMvc.perform(post("/administration/user/cpanel/create_federation")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Arguments"))
                .andExpect(jsonPath("$.error_id")
                        .value("must match \"^[\\w-]{4,}$\""))
                .andExpect(jsonPath("$.error_platforms_id[0]")
                        .value("must match \"^[\\w-]{4,}$\""))
                .andExpect(jsonPath("$.error_platforms_id[1]")
                        .value("must match \"^[\\w-]{4,}$\""));
    }
}