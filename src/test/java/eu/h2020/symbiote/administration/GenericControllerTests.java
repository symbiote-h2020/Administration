package eu.h2020.symbiote.administration;

import eu.h2020.symbiote.administration.helpers.AuthorizationServiceHelper;
import eu.h2020.symbiote.administration.model.FederationWithInvitations;
import eu.h2020.symbiote.security.commons.SecurityConstants;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.Filter;
import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Test class for use in testing MVC and form validation.
 */
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class GenericControllerTests extends AdministrationBaseTestClass {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private Filter springSecurityFilterChain;

    private MockMvc mockMvc;

    @Before
    public void setup() {

        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(this.wac)
                .addFilters(springSecurityFilterChain)
                .build();

        MockitoAnnotations.initMocks(this);
        federationRepository.deleteAll();
    }

    @Test
    public void joinedFederationsGenerateServiceResponseError() throws Exception {
        String error = "error";
        when(authorizationService.generateServiceResponse()).thenReturn(
                new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR));

        mockMvc.perform(post("/administration/generic/joinedFederations")
                .param("platformId", platform1Id))
                .andExpect(status().isInternalServerError())
                .andExpect(header().doesNotExist(SecurityConstants.SECURITY_RESPONSE_HEADER))
                .andExpect(content().string(error));

        verify(authorizationService, times(1)).generateServiceResponse();
    }

    @Test
    public void joinedFederationsUnAuthorized() throws Exception {
        String error = "The stored resource access policy was not satisfied";

        when(authorizationService.generateServiceResponse()).thenReturn(new ResponseEntity<>(serviceResponse, HttpStatus.OK));
        when(authorizationService.checkJoinedFederationsRequest(any(), any(), eq(serviceResponse)))
                .thenReturn(AuthorizationServiceHelper.addSecurityService(
                        error, new HttpHeaders(), HttpStatus.UNAUTHORIZED, serviceResponse));

        mockMvc.perform(post("/administration/generic/joinedFederations")
                .param("platformId", platform1Id))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(SecurityConstants.SECURITY_RESPONSE_HEADER, serviceResponse));

        verify(authorizationService, times(1)).generateServiceResponse();
        verify(authorizationService, times(1)).checkJoinedFederationsRequest(any(), any(), eq(serviceResponse));

    }

    @Test
    public void joined1FederationSuccess() throws Exception {

        storeFederations();

        when(authorizationService.generateServiceResponse()).thenReturn(new ResponseEntity<>(serviceResponse, HttpStatus.OK));
        when(authorizationService.checkJoinedFederationsRequest(eq(platform2Id), any(), eq(serviceResponse)))
                .thenReturn(new ResponseEntity(HttpStatus.OK));

        mockMvc.perform(post("/administration/generic/joinedFederations")
                .param("platformId", platform2Id))
                .andExpect(status().isOk())
                .andExpect(header().string(SecurityConstants.SECURITY_RESPONSE_HEADER, serviceResponse))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[*].id", contains(federationId)));

        verify(authorizationService, times(1)).generateServiceResponse();
        verify(authorizationService, times(1)).checkJoinedFederationsRequest(eq(platform2Id), any(), eq(serviceResponse));
    }

    @Test
    public void joined2FederationsSuccess() throws Exception {
        storeFederations();

        when(authorizationService.generateServiceResponse()).thenReturn(new ResponseEntity<>(serviceResponse, HttpStatus.OK));
        when(authorizationService.checkJoinedFederationsRequest(eq(platform1Id), any(), eq(serviceResponse)))
                .thenReturn(new ResponseEntity(HttpStatus.OK));

        mockMvc.perform(post("/administration/generic/joinedFederations")
                .param("platformId", platform1Id))
                .andExpect(status().isOk())
                .andExpect(header().string(SecurityConstants.SECURITY_RESPONSE_HEADER, serviceResponse))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(federationIdSinglePlatformId, federationId)));

        verify(authorizationService, times(1)).generateServiceResponse();
        verify(authorizationService, times(1)).checkJoinedFederationsRequest(eq(platform1Id), any(), eq(serviceResponse));
    }

    @Test
    public void joined0FederationsSuccess() throws Exception {
        storeFederations();
        String dummyPlatformId = "dummyPlatformId";

        when(authorizationService.generateServiceResponse()).thenReturn(new ResponseEntity<>(serviceResponse, HttpStatus.OK));
        when(authorizationService.checkJoinedFederationsRequest(eq(dummyPlatformId), any(), eq(serviceResponse)))
                .thenReturn(new ResponseEntity(HttpStatus.OK));

        mockMvc.perform(post("/administration/generic/joinedFederations")
                .param("platformId", dummyPlatformId))
                .andExpect(status().isOk())
                .andExpect(header().string(SecurityConstants.SECURITY_RESPONSE_HEADER, serviceResponse))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(authorizationService, times(1)).generateServiceResponse();
        verify(authorizationService, times(1)).checkJoinedFederationsRequest(eq(dummyPlatformId), any(), eq(serviceResponse));
    }

    private void storeFederations() {
        FederationWithInvitations federationWithInvitations1 = sampleSavedFederationWithSinglePlatform();
        FederationWithInvitations federationWithInvitations2 = sampleSavedFederation();

        federationRepository.save(new ArrayList<>(Arrays.asList(federationWithInvitations1, federationWithInvitations2)));
    }
}