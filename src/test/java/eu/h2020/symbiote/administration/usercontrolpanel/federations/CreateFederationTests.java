package eu.h2020.symbiote.administration.usercontrolpanel.federations;

import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.administration.usercontrolpanel.UserControlPanelBaseTestClass;
import eu.h2020.symbiote.model.mim.*;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static eu.h2020.symbiote.administration.services.FederationNotificationService.FEDERATION_MANAGER_URL;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for use in testing MVC and form validation.
 */
public class CreateFederationTests extends UserControlPanelBaseTestClass {

    @Test
    public void success() throws Exception {
        String platform1Url = platformUrl + "/" + platformId;
        String platformId2 = platformId + "2";
        String platform2Url = platformUrl + "/" + platformId2;
        String platformId3 = platformId + "3";
        String platform3Url = platformUrl + "/" + platformId3;

        doReturn(samplePlatformResponseSuccess(platformId)).when(rabbitManager)
                .sendGetPlatformDetailsMessage(eq(platformId));
        doReturn(samplePlatformResponseSuccess(platformId2)).when(rabbitManager)
                .sendGetPlatformDetailsMessage(eq(platformId2));
        doReturn(samplePlatformResponseSuccess(platformId3)).when(rabbitManager)
                .sendGetPlatformDetailsMessage(eq(platformId3));
        doReturn(sampleInformationModelListResponseSuccess()).when(rabbitManager)
                .sendListInfoModelsRequest();
        doReturn(new HttpHeaders())
                .when(authorizationService).getHttpHeadersWithSecurityRequest();
        doReturn(true)
                .when(authorizationService).validateServiceResponse(any(), any(), any());

        MockRestServiceServer mockServer =
                MockRestServiceServer.bindTo(restTemplate).build();
        mockServer.expect(requestTo(platform1Url + FEDERATION_MANAGER_URL)).andExpect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.jsonPath("$.id").value(federationId))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members", hasSize(3)))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members[*].platformId",
                        contains(platformId, platformId2, platformId3)))
                .andRespond(withSuccess());
        mockServer.expect(requestTo(platform2Url + FEDERATION_MANAGER_URL)).andExpect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.jsonPath("$.id").value(federationId))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members", hasSize(3)))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members[*].platformId",
                        contains(platformId, platformId2, platformId3)))                .andRespond(withSuccess());
        mockServer.expect(requestTo(platform3Url + FEDERATION_MANAGER_URL)).andExpect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.jsonPath("$.id").value(federationId))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members", hasSize(3)))
                .andExpect(MockRestRequestMatchers.jsonPath("$.members[*].platformId",
                        contains(platformId, platformId2, platformId3)))
                .andRespond(withSuccess());

        mockMvc.perform(post("/administration/user/cpanel/create_federation")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleFederationRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Federation Registration was successful!"))
                .andExpect(jsonPath("$.federation.id").value(federationId));

        mockServer.verify();

        // Reset the original request factory of restTemplate to unbind it from the mockServer
        restTemplate.setRequestFactory(originalRequestFactory);

        List<Federation> federations = federationRepository.findAll();
        assertEquals(1, federations.size());
        assertEquals(federationId, federations.get(0).getId());
        assertEquals(3, federations.get(0).getMembers().size());
        assertEquals(platform1Url, federations.get(0).getMembers().get(0).getInterworkingServiceURL());
        assertEquals(platform2Url, federations.get(0).getMembers().get(1).getInterworkingServiceURL());
        assertEquals(platform3Url, federations.get(0).getMembers().get(2).getInterworkingServiceURL());
    }

    @Test
    public void federationExists() throws Exception {

        // Save the federation
        Federation federation = sampleFederationRequest();
        federationRepository.save(federation);

        // Change the federation name and send creation request
        federation.setName("newName");

        mockMvc.perform(post("/administration/user/cpanel/create_federation")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(federation)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("The federation with id '" + federationId +
                        "' already exists!"));
    }

    @Test
    public void invalidCreateFederationRequest() throws Exception {

        // Invalid FederationRequest
        Federation federation = new Federation();
        federation.setId("invalid.id");
        federation.setName("na");


        InformationModel informationModel = new InformationModel();
        informationModel.setId("invalid.id");
        federation.setInformationModel(informationModel);

        QoSConstraint qosConstraint1 = new QoSConstraint();
        qosConstraint1.setMetric(QoSMetric.availability);
        qosConstraint1.setComparator(Comparator.equal);
        QoSConstraint qosConstraint2 = new QoSConstraint();
        qosConstraint2.setThreshold(1.2);

        List<QoSConstraint> qosConstraints = new ArrayList<>(Arrays.asList(qosConstraint1, qosConstraint2));
        federation.setSlaConstraints(qosConstraints);

        FederationMember member1 = new FederationMember("valid", platformUrl);
        FederationMember member2 = new FederationMember("invalid.", platformUrl + "2");
        List<FederationMember> members = new ArrayList<>(Arrays.asList(member1, member2));
        federation.setMembers(members);


        mockMvc.perform(post("/administration/user/cpanel/create_federation")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(federation)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Arguments"))
                .andExpect(jsonPath("$.error_id")
                        .value("must match \"^(\\Z|[\\w-]{4,})$\""))
                .andExpect(jsonPath("$.error_name")
                        .value("Length must be between 3 and 30 characters"))
                .andExpect(jsonPath("$.error_informationModel_id")
                        .value("must match \"^[\\w-]{4,}$\""))
                .andExpect(jsonPath("$.error_slaConstraints_metric[1]")
                        .value("may not be null"))
                .andExpect(jsonPath("$.error_slaConstraints_comparator[1]")
                        .value("may not be null"))
                .andExpect(jsonPath("$.error_slaConstraints_threshold[0]")
                        .value("may not be null"))
                .andExpect(jsonPath("$.error_members_platformId[1]")
                        .value("must match \"^[\\w-]{4,}$\""));

        assertEquals(0, federationRepository.findAll().size());
    }

    @Test
    public void memberDoesNotExist() throws Exception {
        String platformId2 = platformId + "2";
        String platformId3 = platformId + "3";

        doReturn(samplePlatformResponseSuccess(platformId)).when(rabbitManager)
                .sendGetPlatformDetailsMessage(eq(platformId));
        doReturn(samplePlatformResponseSuccess(platformId2)).when(rabbitManager)
                .sendGetPlatformDetailsMessage(eq(platformId2));
        doReturn(samplePlatformResponseFail()).when(rabbitManager)
                .sendGetPlatformDetailsMessage(eq(platformId3));

        mockMvc.perform(post("/administration/user/cpanel/create_federation")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleFederationRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("The platform with id " + platformId3 + " was not found"));

        List<Federation> federations = federationRepository.findAll();
        assertEquals(0, federations.size());
    }

    @Test
    public void getPlatformDetailsRegistryUnreachable() throws Exception {

        doReturn(null).when(rabbitManager)
                .sendGetPlatformDetailsMessage(any());

        mockMvc.perform(post("/administration/user/cpanel/create_federation")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleFederationRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error")
                        .value("Registry unreachable!"));

        List<Federation> federations = federationRepository.findAll();
        assertEquals(0, federations.size());
    }

    @Test
    public void getPlatformDetailsCommunicationException() throws Exception {

        doThrow(new CommunicationException("error")).when(rabbitManager)
                .sendGetPlatformDetailsMessage(any());

        mockMvc.perform(post("/administration/user/cpanel/create_federation")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleFederationRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error")
                        .value("Registry threw CommunicationException: error"));

        List<Federation> federations = federationRepository.findAll();
        assertEquals(0, federations.size());
    }

    @Test
    public void informationModelDoesNotExist() throws Exception {
        String platformId2 = platformId + "2";
        String platformId3 = platformId + "3";
        String dummyInfoModelId = "dummy";

        doReturn(samplePlatformResponseSuccess(platformId)).when(rabbitManager)
                .sendGetPlatformDetailsMessage(eq(platformId));
        doReturn(samplePlatformResponseSuccess(platformId2)).when(rabbitManager)
                .sendGetPlatformDetailsMessage(eq(platformId2));
        doReturn(samplePlatformResponseSuccess(platformId3)).when(rabbitManager)
                .sendGetPlatformDetailsMessage(eq(platformId3));
        doReturn(sampleInformationModelListResponseSuccess()).when(rabbitManager)
                .sendListInfoModelsRequest();

        Federation federationRequest = sampleFederationRequest();
        federationRequest.getInformationModel().setId(dummyInfoModelId);

        mockMvc.perform(post("/administration/user/cpanel/create_federation")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(federationRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("The information model with id " + dummyInfoModelId + " was not found"));

        List<Federation> federations = federationRepository.findAll();
        assertEquals(0, federations.size());
    }

    @Test
    public void informationModelRequestRegistryUnreachable() throws Exception {
        String platformId2 = platformId + "2";
        String platformId3 = platformId + "3";

        doReturn(samplePlatformResponseSuccess(platformId)).when(rabbitManager)
                .sendGetPlatformDetailsMessage(eq(platformId));
        doReturn(samplePlatformResponseSuccess(platformId2)).when(rabbitManager)
                .sendGetPlatformDetailsMessage(eq(platformId2));
        doReturn(samplePlatformResponseSuccess(platformId3)).when(rabbitManager)
                .sendGetPlatformDetailsMessage(eq(platformId3));
        doReturn(null).when(rabbitManager)
                .sendListInfoModelsRequest();


        mockMvc.perform(post("/administration/user/cpanel/create_federation")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleFederationRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error")
                        .value("Could not retrieve the information models from registry"));

        List<Federation> federations = federationRepository.findAll();
        assertEquals(0, federations.size());
    }

    @Test
    public void cannotContactFederationManagers() throws Exception {
        String platform1Url = platformUrl + "/" + platformId;
        String platformId2 = platformId + "2";
        String platform2Url = platformUrl + "/" + platformId2;
        String platformId3 = platformId + "3";
        String platform3Url = platformUrl + "/" + platformId3;

        doReturn(samplePlatformResponseSuccess(platformId)).when(rabbitManager)
                .sendGetPlatformDetailsMessage(eq(platformId));
        doReturn(samplePlatformResponseSuccess(platformId2)).when(rabbitManager)
                .sendGetPlatformDetailsMessage(eq(platformId2));
        doReturn(samplePlatformResponseSuccess(platformId3)).when(rabbitManager)
                .sendGetPlatformDetailsMessage(eq(platformId3));
        doReturn(sampleInformationModelListResponseSuccess()).when(rabbitManager)
                .sendListInfoModelsRequest();
        doReturn(new HttpHeaders())
                .when(authorizationService).getHttpHeadersWithSecurityRequest();
        doReturn(true)
                .when(authorizationService).validateServiceResponse(any(), any(), any());

        mockMvc.perform(post("/administration/user/cpanel/create_federation")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleFederationRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Federation Registration was successful!"))
                .andExpect(jsonPath("$.federation.id").value(federationId));

        List<Federation> federations = federationRepository.findAll();
        assertEquals(1, federations.size());
        assertEquals(federationId, federations.get(0).getId());
        assertEquals(3, federations.get(0).getMembers().size());
        assertEquals(platform1Url, federations.get(0).getMembers().get(0).getInterworkingServiceURL());
        assertEquals(platform2Url, federations.get(0).getMembers().get(1).getInterworkingServiceURL());
        assertEquals(platform3Url, federations.get(0).getMembers().get(2).getInterworkingServiceURL());
    }
}