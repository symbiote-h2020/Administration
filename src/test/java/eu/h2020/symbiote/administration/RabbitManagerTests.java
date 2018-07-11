package eu.h2020.symbiote.administration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import eu.h2020.symbiote.administration.exceptions.rabbit.CommunicationException;
import eu.h2020.symbiote.core.cci.InformationModelResponse;
import eu.h2020.symbiote.core.cci.PlatformRegistryResponse;
import eu.h2020.symbiote.core.cci.SspRegistryResponse;
import eu.h2020.symbiote.core.internal.ClearDataRequest;
import eu.h2020.symbiote.core.internal.ClearDataResponse;
import eu.h2020.symbiote.core.internal.InformationModelListResponse;
import eu.h2020.symbiote.core.internal.ResourceListResponse;
import eu.h2020.symbiote.model.mim.Platform;
import eu.h2020.symbiote.model.mim.SmartSpace;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import eu.h2020.symbiote.security.commons.enums.OperationType;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.communication.payloads.*;
import org.junit.Test;
import org.mockito.Spy;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.Collections;
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
        doReturn(serialize(samplePlatformRegistryResponseSuccess()))
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

        // Throw Exception while deseriallizing the response
        ObjectMapper om = spy(new ObjectMapper());
        rabbitManager.setMapper(om);


        // Do not call readValue as it might fail
        doThrow(new IOException()).when(om).readValue(any(String.class), eq(PlatformRegistryResponse.class));
        doReturn(serialize(samplePlatformRegistryResponseSuccess()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));
        try {
            rabbitManager.sendRegistryPlatformMessage("exchangeName",
                    "routingKey", samplePlatform());
        } catch (CommunicationException e) {
            communicationCaught = true;
        }

        assertTrue(communicationCaught);

        // Throw JsonProcessingException while serializing the request
        // Call writeValueAsString
        when(om.writeValueAsString(any(String.class))).thenThrow(new JsonProcessingException("") {});
        response = rabbitManager.sendRegistryPlatformMessage("exchangeName",
                "routingKey", samplePlatform());

        assertNull(response);

    }

    @Test
    public void sendPlatformCreationRequestSuccess() throws Exception {

        doReturn(serialize(samplePlatformRegistryResponseSuccess()))
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
        PlatformRegistryResponse newResponse = samplePlatformRegistryResponseSuccess();
        newResponse.setBody(newPlatform);
        String serializedResponse = serialize(newResponse);

        doReturn(serializedResponse)
            .when(rabbitManager)
            .sendRpcMessage(any(), any(), any(), eq("application/json"));

        PlatformRegistryResponse response = rabbitManager.sendPlatformModificationRequest(newPlatform);

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals(platform1Id, response.getBody().getId());
        assertEquals("Changed description", response.getBody().getDescription().get(0));
        assertEquals(200, response.getStatus());
    }

    @Test
    public void sendPlatformRemovalRequest() throws Exception {

        doReturn(serialize(samplePlatformRegistryResponseSuccess()))
            .when(rabbitManager)
            .sendRpcMessage(any(), any(), any(), eq("application/json"));

        PlatformRegistryResponse response = rabbitManager.sendPlatformRemovalRequest(sampleEmptyPlatform());

        assertNotNull(response);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void sendRegistrySmartSpaceMessage() throws Exception {

        boolean communicationCaught = false;

        // Successful Message
        doReturn(serialize(sampleSspRegistryResponseSuccess()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        SspRegistryResponse response = rabbitManager.sendRegistrySmartSpaceMessage("exchangeName",
                "routingKey", sampleSmartSpace());

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals(200, response.getStatus());

        // Return null
        doReturn(null)
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        response = rabbitManager.sendRegistrySmartSpaceMessage("exchangeName",
                "routingKey", sampleSmartSpace());

        assertNull(response);

        // Throw Exception while deseriallizing the response
        ObjectMapper om = spy(new ObjectMapper());
        rabbitManager.setMapper(om);


        // Do not call readValue as it might fail
        doThrow(new IOException()).when(om).readValue(any(String.class), eq(SspRegistryResponse.class));
        doReturn(serialize(sampleSspRegistryResponseSuccess()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));
        try {
            rabbitManager.sendRegistrySmartSpaceMessage("exchangeName",
                    "routingKey", sampleSmartSpace());
        } catch (CommunicationException e) {
            communicationCaught = true;
        }

        assertTrue(communicationCaught);

        // Throw JsonProcessingException while serializing the request
        // Call writeValueAsString
        when(om.writeValueAsString(any(String.class))).thenThrow(new JsonProcessingException("") {});
        response = rabbitManager.sendRegistrySmartSpaceMessage("exchangeName",
                "routingKey", sampleSmartSpace());

        assertNull(response);

    }

    @Test
    public void sendSmartSpaceCreationRequestSuccess() throws Exception {

        doReturn(serialize(sampleSspRegistryResponseSuccess()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        SspRegistryResponse response = rabbitManager.sendSmartSpaceCreationRequest(sampleSmartSpace());

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void sendSmartSpaceCreationRequestFail() throws Exception {

        doReturn(serialize(sampleSspRegistryResponseFail()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        SspRegistryResponse response = rabbitManager.sendSmartSpaceCreationRequest(sampleSmartSpace());

        assertNotNull(response);
        assertNull(response.getBody());
        assertEquals(400, response.getStatus());
    }

    @Test
    public void sendSmartSpaceModificationRequest() throws Exception {

        SmartSpace newSmartSpace = sampleSmartSpace();
        newSmartSpace.setDescription(Collections.singletonList("Changed description"));
        SspRegistryResponse newResponse = sampleSspRegistryResponseSuccess();
        newResponse.setBody(newSmartSpace);
        String serializedResponse = serialize(newResponse);

        doReturn(serializedResponse)
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        SspRegistryResponse response = rabbitManager.sendSmartSpaceModificationRequest(newSmartSpace);

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals(ssp1Id, response.getBody().getId());
        assertEquals("Changed description", response.getBody().getDescription().get(0));
        assertEquals(200, response.getStatus());
    }

    @Test
    public void sendSmartSpaceRemovalRequest() throws Exception {

        doReturn(serialize(sampleSspRegistryResponseSuccess()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        SspRegistryResponse response = rabbitManager.sendSmartSpaceRemovalRequest(sampleSmartSpace());

        assertNotNull(response);
        assertEquals(200, response.getStatus());
    }


    @Test
    public void sendGetPlatformDetailsMessage() throws Exception {
        boolean communicationCaught = false;

        // Successful Message
        doReturn(serialize(samplePlatformRegistryResponseSuccess()))
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

        // Throw Exception while deserializing the response
        ObjectMapper om = spy(new ObjectMapper());
        rabbitManager.setMapper(om);


        // Do not call readValue as it might fail
        doThrow(new IOException()).when(om).readValue(any(String.class), eq(PlatformRegistryResponse.class));
        doReturn(serialize(samplePlatformRegistryResponseSuccess()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("text/plain"));
        try {
            rabbitManager.sendGetPlatformDetailsMessage("platformId");
        } catch (CommunicationException e) {
            communicationCaught = true;
        }

        assertTrue(communicationCaught);
    }

    @Test
    public void sendGetSSPDetailsMessage() throws Exception {
        boolean communicationCaught = false;

        // Successful Message
        doReturn(serialize(sampleSspRegistryResponseSuccess()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("text/plain"));

        SspRegistryResponse response = rabbitManager.sendGetSSPDetailsMessage(ssp1Id);

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals(200, response.getStatus());

        // Return null
        doReturn(null)
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("text/plain"));

        response = rabbitManager.sendGetSSPDetailsMessage(ssp1Id);

        assertNull(response);

        // Throw Exception while deserializing the response
        ObjectMapper om = spy(new ObjectMapper());
        rabbitManager.setMapper(om);


        // Do not call readValue as it might fail
        doThrow(new IOException()).when(om).readValue(any(String.class), eq(SspRegistryResponse.class));
        doReturn(serialize(samplePlatformRegistryResponseSuccess()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("text/plain"));
        try {
            rabbitManager.sendGetSSPDetailsMessage(ssp1Id);
        } catch (CommunicationException e) {
            communicationCaught = true;
        }

        assertTrue(communicationCaught);

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

        // Throw Exception while deserializing the response
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

        assertTrue(communicationCaught);

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

        // Throw Exception while deserializing the response
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

        assertTrue(communicationCaught);

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
        assertEquals(resourceId, response.getBody().get(0).getId());

        // Return null
        doReturn(null)
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        response = rabbitManager.sendRegistryResourcesRequest(sampleCoreResourceRegistryRequest());

        assertNull(response);

        // Throw Exception while deserializing the response
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

        assertTrue(communicationCaught);

        // Throw JsonProcessingException while serializing the request
        // Call writeValueAsString
        when(om.writeValueAsString(any(String.class))).thenThrow(new JsonProcessingException("") {});
        response = rabbitManager.sendRegistryResourcesRequest(sampleCoreResourceRegistryRequest());

        assertNull(response);
    }

    @Test
    public void sendClearDataRequest() throws Exception {
        boolean communicationCaught = false;

        // Successful Message
        doReturn(serialize(new ClearDataResponse(200, "Done", "done")))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        ClearDataResponse response = rabbitManager.sendClearDataRequest(new ClearDataRequest());

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertEquals(200, response.getStatus());

        // Return null
        doReturn(null)
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        response = rabbitManager.sendClearDataRequest(new ClearDataRequest());

        assertNull(response);

        // Throw Exception while deserializing the response
        ObjectMapper om = spy(new ObjectMapper());
        rabbitManager.setMapper(om);


        // Do not call readValue as it might fail
        doThrow(new IOException()).when(om).readValue(any(String.class), eq(ClearDataRequest.class));
        doReturn(serialize(sampleErrorResponse()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));
        try {
            rabbitManager.sendClearDataRequest(new ClearDataRequest());
        } catch (CommunicationException e) {
            communicationCaught = true;
        }

        assertTrue(communicationCaught);

        // Throw JsonProcessingException while serializing the request
        // Call writeValueAsString
        when(om.writeValueAsString(any(String.class))).thenThrow(new JsonProcessingException("") {});
        response = rabbitManager.sendClearDataRequest(new ClearDataRequest());

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

        ManagementStatus response = rabbitManager.sendUserManagementRequest(sampleUserManagementRequest(UserRole.SERVICE_OWNER));

        assertNotNull(response);
        assertEquals(ManagementStatus.OK, response);

        // Return null
        doReturn(null)
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        try {
            rabbitManager.sendUserManagementRequest(sampleUserManagementRequest(UserRole.SERVICE_OWNER));
        } catch (CommunicationException e) {
            communicationCaught = true;
        }

        assertTrue(communicationCaught);

        // Throw Exception while deserializing the response
        communicationCaught = false;
        ObjectMapper om = spy(new ObjectMapper());
        rabbitManager.setMapper(om);


        // Do not call readValue as it might fail
        doThrow(new IOException()).when(om).readValue(any(String.class), eq(ManagementStatus.class));
        doReturn(serialize(sampleErrorResponse()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));
        try {
            rabbitManager.sendUserManagementRequest(sampleUserManagementRequest(UserRole.SERVICE_OWNER));
        } catch (CommunicationException e) {
            communicationCaught = true;
        }

        assertTrue(communicationCaught);

        // Throw JsonProcessingException while serializing the request
        // Call writeValueAsString
        when(om.writeValueAsString(any(String.class))).thenThrow(new JsonProcessingException("") {});
        response = rabbitManager.sendUserManagementRequest(sampleUserManagementRequest(UserRole.SERVICE_OWNER));

        assertNull(response);
    }

    @Test
    public void sendRevocationRequest() throws Exception {

        boolean communicationCaught = false;

        // Successful Message
        doReturn(serialize(new RevocationResponse(true, HttpStatus.OK)))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        RevocationResponse response = rabbitManager.sendRevocationRequest(new RevocationRequest());

        assertNotNull(response);
        assertTrue(response.isRevoked());
        assertEquals(HttpStatus.OK, response.getStatus());

        // Return null
        doReturn(null)
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        try {
            rabbitManager.sendRevocationRequest(new RevocationRequest());
        } catch (CommunicationException e) {
            communicationCaught = true;
        }

        assertTrue(communicationCaught);

        // Throw Exception while deserializing the response
        communicationCaught = false;
        ObjectMapper om = spy(new ObjectMapper());
        rabbitManager.setMapper(om);


        // Do not call readValue as it might fail
        doThrow(new IOException()).when(om).readValue(any(String.class), eq(RevocationRequest.class));
        doReturn(serialize(sampleErrorResponse()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));
        try {
            rabbitManager.sendRevocationRequest(new RevocationRequest());
        } catch (CommunicationException e) {
            communicationCaught = true;
        }

        assertTrue(communicationCaught);

        // Throw JsonProcessingException while serializing the request
        // Call writeValueAsString
        when(om.writeValueAsString(any(String.class))).thenThrow(new JsonProcessingException("") {});
        response = rabbitManager.sendRevocationRequest(new RevocationRequest());

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


        // Throw Exception while deserializing the response
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

        assertTrue(communicationCaught);

        // Throw JsonProcessingException while serializing the request
        // Call writeValueAsString
        when(om.writeValueAsString(any(String.class))).thenThrow(new JsonProcessingException("") {});
        response = rabbitManager.sendManagePlatformRequest(samplePlatformManagementRequest(OperationType.CREATE));

        assertNull(response);
    }

    @Test
    public void sendManageSSPRequest() throws Exception {

        boolean communicationCaught = false;

        // Successful Message
        doReturn(serialize(sampleSmartSpaceManagementResponse(ManagementStatus.OK)))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        SmartSpaceManagementResponse response = rabbitManager.sendManageSSPRequest(sampleSmartSpaceManagementRequest(OperationType.CREATE));

        assertNotNull(response);
        assertEquals(ManagementStatus.OK, response.getManagementStatus());

        // Return null
        doReturn(null)
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        response = rabbitManager.sendManageSSPRequest(sampleSmartSpaceManagementRequest(OperationType.CREATE));

        assertNull(response);


        // Throw Exception while deserializing the response
        ObjectMapper om = spy(new ObjectMapper());
        rabbitManager.setMapper(om);


        // Do not call readValue as it might fail
        doThrow(new IOException()).when(om).readValue(any(String.class), eq(SmartSpaceManagementResponse.class));
        doReturn(serialize(sampleErrorResponse()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));
        try {
            rabbitManager.sendManageSSPRequest(sampleSmartSpaceManagementRequest(OperationType.CREATE));
        } catch (CommunicationException e) {
            communicationCaught = true;
        }

        assertTrue(communicationCaught);

        // Throw JsonProcessingException while serializing the request
        // Call writeValueAsString
        when(om.writeValueAsString(any(String.class))).thenThrow(new JsonProcessingException("") {});
        response = rabbitManager.sendManageSSPRequest(sampleSmartSpaceManagementRequest(OperationType.CREATE));

        assertNull(response);
    }

    @Test
    public void sendLoginRequest() throws Exception {

        boolean communicationCaught = false;

        // Successful Message
        doReturn(serialize(sampleActiveUserDetailsResponse(HttpStatus.OK)))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        UserDetailsResponse response = rabbitManager.sendLoginRequest(sampleCredentials());

        assertNotNull(response);
        assertEquals(UserRole.SERVICE_OWNER, response.getUserDetails().getRole());

        // Return null
        doReturn(null)
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        response = rabbitManager.sendLoginRequest(sampleCredentials());
        assertNull(response);


        // Throw Exception while deserializing the response
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

        assertTrue(communicationCaught);

        // Throw JsonProcessingException while serializing the request
        // Call writeValueAsString
        when(om.writeValueAsString(any(String.class))).thenThrow(new JsonProcessingException("") {});
        response = rabbitManager.sendLoginRequest(sampleCredentials());
        assertNull(response);
    }


    @Test
    public void sendOwnedPlatformDetailsRequest() throws Exception {

        boolean communicationCaught = false;

        // Successful Message
        doReturn(serialize(sampleOwnedServiceDetails()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        Set<OwnedService> response = rabbitManager.sendOwnedServiceDetailsRequest(
                sampleUserManagementRequest(UserRole.SERVICE_OWNER));

        assertNotNull(response);

        // Return null
        doReturn(null)
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));

        response = rabbitManager.sendOwnedServiceDetailsRequest(sampleUserManagementRequest(UserRole.SERVICE_OWNER));
        assertNull(response);


        // Throw Exception while deserializing the response
        ObjectMapper om = spy(new ObjectMapper());
        rabbitManager.setMapper(om);


        // Do not call readValue as it might fail
        doThrow(new IOException()).when(om).readValue(any(String.class), eq(UserDetailsResponse.class));
        doReturn(serialize(sampleErrorResponse()))
                .when(rabbitManager)
                .sendRpcMessage(any(), any(), any(), eq("application/json"));
        try {
            rabbitManager.sendOwnedServiceDetailsRequest(sampleUserManagementRequest(UserRole.SERVICE_OWNER));
        }
            catch (CommunicationException e) {
            communicationCaught = true;
        }

        assertTrue(communicationCaught);

        // Throw JsonProcessingException while serializing the request
        // Call writeValueAsString
        when(om.writeValueAsString(any(String.class))).thenThrow(new JsonProcessingException("") {});
        response = rabbitManager.sendOwnedServiceDetailsRequest(sampleUserManagementRequest(UserRole.SERVICE_OWNER));
        assertNull(response);
    }
}