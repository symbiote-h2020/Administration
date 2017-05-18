package eu.h2020.symbiote;

import java.util.List;
import java.util.ArrayList;

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
import eu.h2020.symbiote.model.PlatformResponse;
import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.security.payloads.PlatformRegistrationRequest;
import eu.h2020.symbiote.security.payloads.PlatformRegistrationResponse;
import eu.h2020.symbiote.security.payloads.ErrorResponseContainer;
import eu.h2020.symbiote.communication.CommunicationException;
import eu.h2020.symbiote.security.token.Token;
import eu.h2020.symbiote.security.payloads.OwnedPlatformDetails;


public class RabbitManagerTests extends AdministrationTests {

    @Spy
    RabbitManager rabbitManager;

    // ==== Registry Communication ====
    @Test
    public void registerPlatformSuccess() throws Exception {

        doReturn( serialize(samplePlatformResponseSuccess()) )
            .when(rabbitManager)
            .sendRpcMessage(any(), any(), any());

        PlatformResponse response = rabbitManager.sendPlatformCreationRequest(samplePlatform());

        assertNotNull(response);
        assertNotNull(response.getPlatform());
        assertNotNull(response.getPlatform().getPlatformId());
        assertEquals(200, response.getStatus());

    }

    @Test
    public void registerPlatformFail() throws Exception {

        doReturn( serialize(samplePlatformResponseFail()) )
            .when(rabbitManager)
            .sendRpcMessage(any(), any(), any());

        PlatformResponse response = rabbitManager.sendPlatformCreationRequest(samplePlatform());

        assertNotNull(response);
        assertNull(response.getPlatform());
        assertEquals(400, response.getStatus());
    }

    @Test
    public void modifyPlatform() throws Exception {
        
        Platform newPlatform = samplePlatform();
        newPlatform.setDescription("Changed description");
        PlatformResponse newResponse = samplePlatformResponseSuccess();
        newResponse.setPlatform(newPlatform);

        doReturn( serialize(newResponse) )
            .when(rabbitManager)
            .sendRpcMessage(any(), any(), any());

        PlatformResponse response = rabbitManager.sendPlatformModificationRequest(newPlatform);

        assertNotNull(response);
        assertNotNull(response.getPlatform());
        assertNotNull(response.getPlatform().getPlatformId());
        assertEquals(platformId, response.getPlatform().getPlatformId());
        assertEquals("Changed description", response.getPlatform().getDescription());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void deletePlatform() throws Exception {

        doReturn( serialize(samplePlatformResponseSuccess()) )
            .when(rabbitManager)
            .sendRpcMessage(any(), any(), any());

        PlatformResponse response = rabbitManager.sendPlatformRemovalRequest(sampleEmptyPlatform());

        assertNotNull(response);
        assertEquals(200, response.getStatus());
    }

    // ==== AAM Communication ====

    @Test
    public void registerPlatformOwnerSuccess() throws Exception {

        doReturn( serialize(samplePlatformResponse()) )
            .when(rabbitManager)
            .sendRpcMessage(any(), any(), any());

        PlatformRegistrationRequest request = samplePlatformRequest();
        PlatformRegistrationResponse response = rabbitManager.sendPlatformRegistrationRequest(request);

        assertNotNull(response);
        assertEquals(platformId, response.getPlatformId());
    }

    @Test
    public void registerPlatformOwnerFail() throws Exception {

        doReturn( serialize(sampleErrorResponse()) )
            .when(rabbitManager)
            .sendRpcMessage(any(), any(), any());

        PlatformRegistrationResponse response = null;
        String errorResponse = null;
        try{
            response = rabbitManager.sendPlatformRegistrationRequest(samplePlatformRequest());
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

        assertNotNull(responseToken);
        assertEquals(sampleTokenString, responseToken.getToken());
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