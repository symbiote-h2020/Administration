package eu.h2020.symbiote.administration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.core.cci.InformationModelResponse;
import eu.h2020.symbiote.core.cci.PlatformRegistryResponse;
import eu.h2020.symbiote.core.internal.InformationModelListResponse;
import eu.h2020.symbiote.core.internal.ResourceListResponse;
import eu.h2020.symbiote.model.mim.Platform;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import eu.h2020.symbiote.security.commons.enums.OperationType;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.communication.payloads.*;
import org.junit.Test;
import org.mockito.Spy;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


/**
 * Test class for use in testing Rabbit Manager methods.
 */
public class RabbitManagerTests extends AdministrationBaseTestClass {

    @Spy
    private RabbitManager rabbitManager;

    // ==== Registry Communication ====

    @Test
    public void sendRegistryPlatformMessage() throws Exception {

        boolean communicationCaught = false;

        // Successful Message
        doReturn(serialize(samplePlatformResponseSuccess()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        PlatformRegistryResponse response = rabbitManager.sendRegistryPlatformMessage("exchangeName",
                "routingKey", samplePlatform());

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals(200, response.getStatus());

        // Return null
        doReturn(null)
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        response = rabbitManager.sendRegistryPlatformMessage("exchangeName",
                "routingKey", samplePlatform());

        assertNull(response);

        // Throw Exception while deserialializing the response
        ObjectMapper om = spy(new ObjectMapper());
        rabbitManager.setMapper(om);


        // Do not call readValue as it might fail
        doThrow(new IOException()).when(om).readValue(any(String.class), eq(PlatformRegistryResponse.class));
        doReturn(serialize(samplePlatformResponseSuccess()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));
        try {
            rabbitManager.sendRegistryPlatformMessage("exchangeName",
                    "routingKey", samplePlatform());
        } catch (CommunicationException e) {
            communicationCaught = true;
        }

        assertEquals(true, communicationCaught);

        // Throw JsonProcessingException while serialializing the request
        // Call writeValueAsString
        when(om.writeValueAsString(any(String.class))).thenThrow(new JsonProcessingException("") {});
        response = rabbitManager.sendRegistryPlatformMessage("exchangeName",
                "routingKey", samplePlatform());

        assertNull(response);

    }

    @Test
    public void sendPlatformCreationRequestSuccess() throws Exception {

        doReturn(serialize(samplePlatformResponseSuccess()))
            .when(rabbitManager)
            .sendRpcMessage(any(), any(), any(), eq("application/json"));

        PlatformRegistryResponse response = rabbitManager.sendPlatformCreationRequest(samplePlatform());

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals(200, response.getStatus());

    }

    @Test
    public void sendPlatformCreationRequestFail() throws Exception {

        doReturn(serialize(samplePlatformResponseFail()))
            .when(rabbitManager)
            .sendRpcMessage(any(), any(), any(), eq("application/json"));

        PlatformRegistryResponse response = rabbitManager.sendPlatformCreationRequest(samplePlatform());

        assertNotNull(response);
        assertNull(response.getBody());
        assertEquals(400, response.getStatus());
    }

    @Test
    public void sendPlatformModificationRequest() throws Exception {
        
        Platform newPlatform = samplePlatform();
        newPlatform.setDescription(Collections.singletonList("Changed description"));
        PlatformRegistryResponse newResponse = samplePlatformResponseSuccess();
        newResponse.setBody(newPlatform);
        String serializedResponse = serialize(newResponse);

        doReturn(serializedResponse)
            .when(rabbitManager)
            .sendRpcMessage(any(), any(), any(), eq("application/json"));

        PlatformRegistryResponse response = rabbitManager.sendPlatformModificationRequest(newPlatform);

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals(platformId, response.getBody().getId());
        assertEquals("Changed description", response.getBody().getDescription().get(0));
        assertEquals(200, response.getStatus());
    }

    @Test
    public void sendPlatformRemovalRequest() throws Exception {

        doReturn(serialize(samplePlatformResponseSuccess()))
            .when(rabbitManager)
            .sendRpcMessage(any(), any(), any(), eq("application/json"));

        PlatformRegistryResponse response = rabbitManager.sendPlatformRemovalRequest(sampleEmptyPlatform());

        assertNotNull(response);
        assertEquals(200, response.getStatus());
    }


    @Test
    public void sendGetPlatformDetailsMessage() throws Exception {
        boolean communicationCaught = false;

        // Successful Message
        doReturn(serialize(samplePlatformResponseSuccess()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("text/plain"));

        PlatformRegistryResponse response = rabbitManager.sendGetPlatformDetailsMessage("platformId");

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals(200, response.getStatus());

        // Return null
        doReturn(null)
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("text/plain"));

        response = rabbitManager.sendGetPlatformDetailsMessage("platformId");

        assertNull(response);

        // Throw Exception while deserialializing the response
        ObjectMapper om = spy(new ObjectMapper());
        rabbitManager.setMapper(om);


        // Do not call readValue as it might fail
        doThrow(new IOException()).when(om).readValue(any(String.class), eq(PlatformRegistryResponse.class));
        doReturn(serialize(samplePlatformResponseSuccess()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("text/plain"));
        try {
            rabbitManager.sendGetPlatformDetailsMessage("platformId");
        } catch (CommunicationException e) {
            communicationCaught = true;
        }

        assertEquals(true, communicationCaught);

    }


    @Test
    public void sendListInfoModelsRequest() throws Exception {
        boolean communicationCaught = false;

        // Successful Message
        doReturn(serialize(sampleInformationModelListResponseSuccess()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("text/plain"));

        InformationModelListResponse response = rabbitManager.sendListInfoModelsRequest();

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().get(0).getId());
        assertEquals(200, response.getStatus());
        assertEquals(informationModelId, response.getBody().get(0).getId());

        // Return null
        doReturn(null)
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("text/plain"));

        response = rabbitManager.sendListInfoModelsRequest();

        assertNull(response);

        // Throw Exception while deserialializing the response
        ObjectMapper om = spy(new ObjectMapper());
        rabbitManager.setMapper(om);


        // Do not call readValue as it might fail
        doThrow(new IOException()).when(om).readValue(any(String.class), eq(InformationModelListResponse.class));
        doReturn(serialize(sampleErrorResponse()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("text/plain"));
        try {
            rabbitManager.sendListInfoModelsRequest();
        } catch (CommunicationException e) {
            communicationCaught = true;
        }

        assertEquals(true, communicationCaught);

        // Throw IOException while deserializing the ErrorResponseContainer
        doThrow(new IOException()).when(om).readValue(any(String.class), eq(ErrorResponseContainer.class));
        response = rabbitManager.sendListInfoModelsRequest();

        assertNull(response);
    }


    @Test
    public void sendInfoModelRequest() throws Exception {
        boolean communicationCaught = false;

        // Successful Message
        doReturn(serialize(sampleInformationModelResponseSuccess()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        InformationModelResponse response = rabbitManager.sendInfoModelRequest("", sampleInformationModelRequest());

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals(200, response.getStatus());
        assertEquals(informationModelId, response.getBody().getId());

        // Return null
        doReturn(null)
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        response = rabbitManager.sendInfoModelRequest("", sampleInformationModelRequest());

        assertNull(response);

        // Throw Exception while deserialializing the response
        ObjectMapper om = spy(new ObjectMapper());
        rabbitManager.setMapper(om);


        // Do not call readValue as it might fail
        doThrow(new IOException()).when(om).readValue(any(String.class), eq(InformationModelResponse.class));
        doReturn(serialize(sampleErrorResponse()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));
        try {
            rabbitManager.sendInfoModelRequest("", sampleInformationModelRequest());
        } catch (CommunicationException e) {
            communicationCaught = true;
        }

        assertEquals(true, communicationCaught);

        // Throw IOException while deserializing the ErrorResponseContainer
        doThrow(new IOException()).when(om).readValue(any(String.class), eq(ErrorResponseContainer.class));
        response = rabbitManager.sendInfoModelRequest("", sampleInformationModelRequest());

        assertNull(response);
    }


    @Test
    public void sendRegisterInfoModelRequest() throws Exception {

        doReturn(serialize(sampleInformationModelResponseSuccess()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        InformationModelResponse response = rabbitManager.sendRegisterInfoModelRequest(sampleInformationModelRequest());

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals(200, response.getStatus());
        assertEquals(informationModelId, response.getBody().getId());

    }

    @Test
    public void sendDeleteInfoModelRequest() throws Exception {

        doReturn(serialize(sampleInformationModelResponseSuccess()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        InformationModelResponse response = rabbitManager.sendDeleteInfoModelRequest(sampleInformationModelRequest());

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals(200, response.getStatus());
        assertEquals(informationModelName, response.getBody().getName());

    }


    @Test
    public void sendRegistryResourcesRequest() throws Exception {
        boolean communicationCaught = false;

        // Successful Message
        doReturn(serialize(sampleResourceListResponseSuccess()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        ResourceListResponse response = rabbitManager.sendRegistryResourcesRequest(sampleCoreResourceRegistryRequest());

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().get(0).getId());
        assertEquals(200, response.getStatus());
        assertEquals(resourcelId, response.getBody().get(0).getId());

        // Return null
        doReturn(null)
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        response = rabbitManager.sendRegistryResourcesRequest(sampleCoreResourceRegistryRequest());

        assertNull(response);

        // Throw Exception while deserialializing the response
        ObjectMapper om = spy(new ObjectMapper());
        rabbitManager.setMapper(om);


        // Do not call readValue as it might fail
        doThrow(new IOException()).when(om).readValue(any(String.class), eq(ResourceListResponse.class));
        doReturn(serialize(sampleErrorResponse()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));
        try {
            rabbitManager.sendRegistryResourcesRequest(sampleCoreResourceRegistryRequest());
        } catch (CommunicationException e) {
            communicationCaught = true;
        }

        assertEquals(true, communicationCaught);

        // Throw JsonProcessingException while serialializing the request
        // Call writeValueAsString
        when(om.writeValueAsString(any(String.class))).thenThrow(new JsonProcessingException("") {});
        response = rabbitManager.sendRegistryResourcesRequest(sampleCoreResourceRegistryRequest());

        assertNull(response);
    }


    // ==== AAM Communication ====

    @Test
    public void sendUserManagementRequest() throws Exception {

        boolean communicationCaught = false;

        // Successful Message
        doReturn(serialize(ManagementStatus.OK))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        ManagementStatus response = rabbitManager.sendUserManagementRequest(sampleUserManagementRequest(UserRole.PLATFORM_OWNER));

        assertNotNull(response);
        assertEquals(ManagementStatus.OK, response);

        // Return null
        doReturn(null)
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        try {
            rabbitManager.sendUserManagementRequest(sampleUserManagementRequest(UserRole.PLATFORM_OWNER));
        } catch (CommunicationException e) {
            communicationCaught = true;
        }

        assertEquals(true, communicationCaught);

        // Throw Exception while deserialializing the response
        communicationCaught = false;
        ObjectMapper om = spy(new ObjectMapper());
        rabbitManager.setMapper(om);


        // Do not call readValue as it might fail
        doThrow(new IOException()).when(om).readValue(any(String.class), eq(ManagementStatus.class));
        doReturn(serialize(sampleErrorResponse()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));
        try {
            rabbitManager.sendUserManagementRequest(sampleUserManagementRequest(UserRole.PLATFORM_OWNER));
        } catch (CommunicationException e) {
            communicationCaught = true;
        }

        assertEquals(true, communicationCaught);

        // Throw JsonProcessingException while serialializing the request
        // Call writeValueAsString
        when(om.writeValueAsString(any(String.class))).thenThrow(new JsonProcessingException("") {});
        response = rabbitManager.sendUserManagementRequest(sampleUserManagementRequest(UserRole.PLATFORM_OWNER));

        assertNull(response);
    }


    @Test
    public void sendManagePlatformRequest() throws Exception {

        boolean communicationCaught = false;

        // Successful Message
        doReturn(serialize(samplePlatformManagementResponse(ManagementStatus.OK)))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        PlatformManagementResponse response = rabbitManager.sendManagePlatformRequest(samplePlatformManagementRequest(OperationType.CREATE));

        assertNotNull(response);
        assertEquals(ManagementStatus.OK, response.getRegistrationStatus());

        // Return null
        doReturn(null)
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        response = rabbitManager.sendManagePlatformRequest(samplePlatformManagementRequest(OperationType.CREATE));

        assertNull(response);


        // Throw Exception while deserialializing the response
        ObjectMapper om = spy(new ObjectMapper());
        rabbitManager.setMapper(om);


        // Do not call readValue as it might fail
        doThrow(new IOException()).when(om).readValue(any(String.class), eq(PlatformManagementResponse.class));
        doReturn(serialize(sampleErrorResponse()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));
        try {
            rabbitManager.sendManagePlatformRequest(samplePlatformManagementRequest(OperationType.CREATE));
        } catch (CommunicationException e) {
            communicationCaught = true;
        }

        assertEquals(true, communicationCaught);

        // Throw JsonProcessingException while serialializing the request
        // Call writeValueAsString
        when(om.writeValueAsString(any(String.class))).thenThrow(new JsonProcessingException("") {});
        response = rabbitManager.sendManagePlatformRequest(samplePlatformManagementRequest(OperationType.CREATE));

        assertNull(response);
    }


    @Test
    public void sendLoginRequest() throws Exception {

        boolean communicationCaught = false;

        // Successful Message
        doReturn(serialize(sampleUserDetailsResponse(HttpStatus.OK)))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        UserDetailsResponse response = rabbitManager.sendLoginRequest(sampleCredentials());

        assertNotNull(response);
        assertEquals(UserRole.PLATFORM_OWNER, response.getUserDetails().getRole());

        // Return null
        doReturn(null)
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        response = rabbitManager.sendLoginRequest(sampleCredentials());
        assertNull(response);


        // Throw Exception while deserialializing the response
        ObjectMapper om = spy(new ObjectMapper());
        rabbitManager.setMapper(om);


        // Do not call readValue as it might fail
        doThrow(new IOException()).when(om).readValue(any(String.class), eq(UserDetailsResponse.class));
        doReturn(serialize(sampleErrorResponse()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));
        try {
            rabbitManager.sendLoginRequest(sampleCredentials());
        } catch (CommunicationException e) {
            communicationCaught = true;
        }

        assertEquals(true, communicationCaught);

        // Throw JsonProcessingException while serialializing the request
        // Call writeValueAsString
        when(om.writeValueAsString(any(String.class))).thenThrow(new JsonProcessingException("") {});
        response = rabbitManager.sendLoginRequest(sampleCredentials());
        assertNull(response);
    }


    @Test
    public void sendOwnedPlatformDetailsRequest() throws Exception {

        boolean communicationCaught = false;

        // Successful Message
        doReturn(serialize(sampleOwnedPlatformDetails()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        Set<OwnedPlatformDetails> response = rabbitManager.sendOwnedPlatformDetailsRequest(sampleUserManagementRequest(UserRole.PLATFORM_OWNER));

        assertNotNull(response);
        assertEquals(platformId, response.iterator().next().getPlatformInstanceId());

        // Return null
        doReturn(null)
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        response = rabbitManager.sendOwnedPlatformDetailsRequest(sampleUserManagementRequest(UserRole.PLATFORM_OWNER));
        assertNull(response);


        // Throw Exception while deserialializing the response
        ObjectMapper om = spy(new ObjectMapper());
        rabbitManager.setMapper(om);


        // Do not call readValue as it might fail
        doThrow(new IOException()).when(om).readValue(any(String.class), eq(UserDetailsResponse.class));
        doReturn(serialize(sampleErrorResponse()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));
        try {
            rabbitManager.sendOwnedPlatformDetailsRequest(sampleUserManagementRequest(UserRole.PLATFORM_OWNER));
        }
            catch (CommunicationException e) {
            communicationCaught = true;
        }

        assertEquals(true, communicationCaught);

        // Throw JsonProcessingException while serialializing the request
        // Call writeValueAsString
        when(om.writeValueAsString(any(String.class))).thenThrow(new JsonProcessingException("") {});
        response = rabbitManager.sendOwnedPlatformDetailsRequest(sampleUserManagementRequest(UserRole.PLATFORM_OWNER));
        assertNull(response);
    }


    @Test
    public void sendFederationRuleManagementRequest() throws Exception {

        boolean communicationCaught = false;

        // Successful Message
        doReturn(serialize(sampleFederationRuleManagementResponse()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        Map<String, FederationRule> response = rabbitManager.sendFederationRuleManagementRequest(
                sampleFederationRuleManagementRequest(FederationRuleManagementRequest.OperationType.CREATE));

        assertNotNull(response);
        assertNotNull(response.get(federationRuleId));
        assertEquals(federationRuleId, response.get(federationRuleId).getFederationId());
        assertEquals(true, response.get(federationRuleId).getPlatformIds().contains(platformId));
        assertEquals(true, response.get(federationRuleId).getPlatformIds().contains(platformId + '2'));

        // Return null
        doReturn(null)
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        response = rabbitManager.sendFederationRuleManagementRequest(
                sampleFederationRuleManagementRequest(FederationRuleManagementRequest.OperationType.CREATE));
        assertNull(response);


        // Throw Exception while deserialializing the response
        ObjectMapper om = spy(new ObjectMapper());
        rabbitManager.setMapper(om);


        // Do not call readValue as it might fail
        doThrow(new IOException()).when(om).readValue(any(String.class), eq(Map.class));
        doReturn(serialize(sampleErrorResponse()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));
        try {
            rabbitManager.sendFederationRuleManagementRequest(
                    sampleFederationRuleManagementRequest(FederationRuleManagementRequest.OperationType.CREATE));
        }
        catch (CommunicationException e) {
            communicationCaught = true;
        }

        assertEquals(true, communicationCaught);

        // Throw JsonProcessingException while serialializing the request
        // Call writeValueAsString
        when(om.writeValueAsString(any(String.class))).thenThrow(new JsonProcessingException("") {});
        response = rabbitManager.sendFederationRuleManagementRequest(
                sampleFederationRuleManagementRequest(FederationRuleManagementRequest.OperationType.CREATE));
        assertNull(response);
    }
}