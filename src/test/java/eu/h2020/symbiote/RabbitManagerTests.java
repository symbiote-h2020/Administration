package eu.h2020.symbiote;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;
import org.mockito.Spy;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.rabbitmq.client.RpcClient;

import eu.h2020.symbiote.communication.RabbitManager;
import eu.h2020.symbiote.communication.CommunicationException;

import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.core.model.InterworkingService;
import eu.h2020.symbiote.core.cci.PlatformRegistryResponse;
import eu.h2020.symbiote.security.communication.payloads.PlatformManagementRequest;
import eu.h2020.symbiote.security.communication.payloads.PlatformManagementResponse;
import eu.h2020.symbiote.security.communication.payloads.UserManagementRequest;
import eu.h2020.symbiote.security.commons.enums.OperationType;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import eu.h2020.symbiote.security.communication.payloads.Credentials;
import eu.h2020.symbiote.security.communication.payloads.UserDetails;
import eu.h2020.symbiote.security.communication.payloads.OwnedPlatformDetails;
import eu.h2020.symbiote.security.communication.payloads.ErrorResponseContainer;
import eu.h2020.symbiote.security.commons.Token;


/**
 * Test class for use in testing Rabbit Manager methods.
 */
public class RabbitManagerTests extends AdministrationTests {

    @Spy
    RabbitManager rabbitManager;

    // ==== Registry Communication ====
    @Test
    public void registerPlatformSuccess() throws Exception {

        doReturn( serialize(samplePlatformResponseSuccess()) )
            .when(rabbitManager)
            .sendRpcMessage(any(), any(), any());

        PlatformRegistryResponse response = rabbitManager.sendPlatformCreationRequest(samplePlatform());

        assertNotNull(response);
        assertNotNull(response.getPlatform());
        assertNotNull(response.getPlatform().getId());
        assertEquals(200, response.getStatus());

    }

    @Test
    public void registerPlatformFail() throws Exception {

        doReturn( serialize(samplePlatformResponseFail()) )
            .when(rabbitManager)
            .sendRpcMessage(any(), any(), any());

        PlatformRegistryResponse response = rabbitManager.sendPlatformCreationRequest(samplePlatform());

        assertNotNull(response);
        assertNull(response.getPlatform());
        assertEquals(400, response.getStatus());
    }

    @Test
    public void modifyPlatform() throws Exception {
        
        Platform newPlatform = samplePlatform();
        newPlatform.setComments(Arrays.asList("Changed description"));
        PlatformRegistryResponse newResponse = samplePlatformResponseSuccess();
        newResponse.setPlatform(newPlatform);
        String serializedResponse = serialize(newResponse);

        doReturn( serializedResponse )
            .when(rabbitManager)
            .sendRpcMessage(any(), any(), any());

        PlatformRegistryResponse response = rabbitManager.sendPlatformModificationRequest(newPlatform);

        assertNotNull(response);
        assertNotNull(response.getPlatform());
        assertNotNull(response.getPlatform().getId());
        assertEquals(platformId, response.getPlatform().getId());
        assertEquals("Changed description", response.getPlatform().getComments().get(0));
        assertEquals(200, response.getStatus());
    }

    @Test
    public void deletePlatform() throws Exception {

        doReturn( serialize(samplePlatformResponseSuccess()) )
            .when(rabbitManager)
            .sendRpcMessage(any(), any(), any());

        PlatformRegistryResponse response = rabbitManager.sendPlatformRemovalRequest(sampleEmptyPlatform());

        assertNotNull(response);
        assertEquals(200, response.getStatus());
    }

    // ==== AAM Communication ====

    @Test
    public void registerPlatformOwnerSuccess() throws Exception {

        doReturn( serialize(samplePlatformResponse()) )
            .when(rabbitManager)
            .sendRpcMessage(any(), any(), any());

        PlatformManagementRequest request = samplePlatformManagementRequest();
        PlatformManagementResponse response = rabbitManager.sendPlatformRegistrationRequest(request);

        assertNotNull(response);
        assertEquals(platformId, response.getPlatformId());
    }

    @Test
    public void registerPlatformOwnerFail() throws Exception {

        doReturn( serialize(sampleErrorResponse()) )
            .when(rabbitManager)
            .sendRpcMessage(any(), any(), any());

        PlatformManagementResponse response = null;
        String errorResponse = null;
        try{
            response = rabbitManager.sendPlatformRegistrationRequest(samplePlatformManagementRequest());
        } catch(CommunicationException e){
            errorResponse = e.getMessage();
        }
        assertNull(response);
        assertNotNull(errorResponse);
    }

    @Test
    public void loginPlatformOwnerSuccess() throws Exception {

        doReturn(serialize(sampleToken()))
            .when(rabbitManager).sendRpcMessage(any(), any(), any());

        Token responseToken = rabbitManager.sendLoginRequest(sampleCredentials());

        // assertNotNull(responseToken);
        // assertEquals(sampleTokenString, responseToken.getToken());
    }

    @Test
    public void loginPlatformOwnerFail() throws Exception {

        doReturn( serialize(sampleErrorResponse()) )
            .when(rabbitManager)
            .sendRpcMessage(any(), any(), any());

        Token responseToken = null;
        String errorResponse = null;
        try{
            responseToken = rabbitManager.sendLoginRequest(sampleCredentials());
        } catch(CommunicationException e){
            errorResponse = e.getMessage();
        }

        assertNull(responseToken);
        assertNotNull(errorResponse);
    }    

    @Test
    public void ownerDetailsSuccess() throws Exception {

        doReturn( serialize(sampleOwnerDetails()) )
            .when(rabbitManager)
            .sendRpcMessage(any(), any(), any());

        OwnedPlatformDetails responseDetails = rabbitManager.sendDetailsRequest(sampleTokenString);

        assertNotNull(responseDetails);
        assertEquals(platformId, responseDetails.getPlatformInstanceId());
    }

    @Test
    public void ownerDetailsFail() throws Exception {

        doReturn( serialize(sampleErrorResponse()) )
            .when(rabbitManager)
            .sendRpcMessage(any(), any(), any());

        OwnedPlatformDetails responseDetails = null;
        String errorResponse = null;
        try{
            responseDetails = rabbitManager.sendDetailsRequest(sampleTokenString);
        } catch(CommunicationException e){
            errorResponse = e.getMessage();
        }

        assertNull(responseDetails);
        assertNotNull(errorResponse);
    }
}