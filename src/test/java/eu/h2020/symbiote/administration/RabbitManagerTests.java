package eu.h2020.symbiote.administration;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import eu.h2020.symbiote.security.commons.enums.OperationType;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.communication.payloads.UserDetailsResponse;
import org.junit.Test;
import org.mockito.Spy;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.core.cci.PlatformRegistryResponse;
import eu.h2020.symbiote.security.communication.payloads.PlatformManagementResponse;
import eu.h2020.symbiote.security.communication.payloads.OwnedPlatformDetails;
import org.springframework.http.HttpStatus;


/**
 * Test class for use in testing Rabbit Manager methods.
 */
public class RabbitManagerTests extends AdministrationTests {

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
            response = rabbitManager.sendRegistryPlatformMessage("exchangeName",
                    "routingKey", samplePlatform());
        } catch (CommunicationException e) {
            communicationCaught = true;
        }

        assertEquals(true, communicationCaught);

        // Throw JsonProcessingException while serialializing the request
        rabbitManager.setMapper(om);

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
        newPlatform.setComments(Arrays.asList("Changed description"));
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
        assertEquals("Changed description", response.getBody().getComments().get(0));
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
            response = rabbitManager.sendGetPlatformDetailsMessage("platformId");
        } catch (CommunicationException e) {
            communicationCaught = true;
        }

        assertEquals(true, communicationCaught);

    }


    // ==== AAM Communication ====

    @Test
    public void registerPlatformSuccessAAM() throws Exception {

        doReturn(serialize(samplePlatformManagementResponse(ManagementStatus.OK)))
            .when(rabbitManager)
            .sendRpcMessage(any(), any(), any(), eq("application/json"));

        PlatformManagementResponse response = rabbitManager.
                sendManagePlatformRequest(samplePlatformManagementRequest(OperationType.CREATE));

        assertNotNull(response);
        assertEquals(platformId, response.getPlatformId());
    }

    @Test
    public void registerPlatformFailAAM() throws Exception {

        doReturn(serialize(sampleErrorResponse()))
            .when(rabbitManager)
            .sendRpcMessage(any(), any(), any(), eq("application/json"));

        PlatformManagementResponse response = null;
        String errorResponse = null;
        try{
            response = rabbitManager.
                    sendManagePlatformRequest(samplePlatformManagementRequest(OperationType.CREATE));
        } catch(CommunicationException e){
            errorResponse = e.getMessage();
        }
        assertNull(response);
        assertNotNull(errorResponse);
    }

    @Test
    public void loginSuccess() throws Exception {

        doReturn(serialize(sampleUserDetailsResponse(HttpStatus.OK)))
            .when(rabbitManager).sendRpcMessage(any(), any(), any(), eq("application/json"));

        UserDetailsResponse userDetailsResponse = rabbitManager.sendLoginRequest(sampleCredentials());

        assertNotNull(userDetailsResponse);
        assertEquals(HttpStatus.OK, userDetailsResponse.getHttpStatus());
        assertEquals(username, userDetailsResponse.getUserDetails().getCredentials().getUsername());
        assertEquals(password, userDetailsResponse.getUserDetails().getCredentials().getPassword());
        assertEquals(UserRole.PLATFORM_OWNER, userDetailsResponse.getUserDetails().getRole());
    }

    @Test
    public void loginFail() throws Exception {

        doReturn(serialize(sampleErrorResponse()))
            .when(rabbitManager)
            .sendRpcMessage(any(), any(), any(), eq("application/json"));

        UserDetailsResponse userDetailsResponse = null;
        String errorResponse = null;
        try{
            userDetailsResponse = rabbitManager.sendLoginRequest(sampleCredentials());
        } catch(CommunicationException e){
            errorResponse = e.getMessage();
        }

        assertNull(userDetailsResponse);
        assertNotNull(errorResponse);
    }    

    @Test
    public void ownerDetailsSuccess() throws Exception {

        doReturn(serialize(sampleOwnedPlatformDetails()))
            .when(rabbitManager)
            .sendRpcMessage(any(), any(), any(), eq("application/json"));

        Set<OwnedPlatformDetails> responseDetails = rabbitManager.sendOwnedPlatformDetailsRequest(
                sampleUserManagementRequest(UserRole.PLATFORM_OWNER));

        assertNotNull(responseDetails);
        assertEquals(1, responseDetails.size());
        assertEquals(platformId, responseDetails.iterator().next().getPlatformInstanceId());
    }

    @Test
    public void ownerDetailsFail() throws Exception {

        doReturn(serialize(sampleErrorResponse()))
            .when(rabbitManager)
            .sendRpcMessage(any(), any(), any(), eq("application/json"));

        Set<OwnedPlatformDetails> responseDetails = null;
        String errorResponse = null;
        try{
            responseDetails = rabbitManager.sendOwnedPlatformDetailsRequest(
                    sampleUserManagementRequest(UserRole.PLATFORM_OWNER));
        } catch(CommunicationException e){
            errorResponse = e.getMessage();
        }

        assertNull(responseDetails);
        assertNotNull(errorResponse);
    }
}