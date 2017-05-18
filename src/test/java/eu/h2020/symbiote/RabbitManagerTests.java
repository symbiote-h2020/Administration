package eu.h2020.symbiote;

import java.util.List;
import java.util.ArrayList;

import org.junit.Test;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.communication.RabbitManager;
import eu.h2020.symbiote.model.PlatformResponse;
import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.security.payloads.PlatformRegistrationRequest;
import eu.h2020.symbiote.security.payloads.PlatformRegistrationResponse;


public class RabbitManagerTests extends AdministrationTests {

    // ==== Registry Communication ====
    @Test
    public void registerPlatformSuccess() throws Exception {
        
        ObjectMapper mapper = new ObjectMapper();
        String platformResponseString = mapper.writeValueAsString(samplePlatformResponseSuccess());

        RabbitManager rabbitManager = spy(new RabbitManager());
        doReturn(platformResponseString).when(rabbitManager).sendRpcMessage(any(), any(), any());

        PlatformResponse response = rabbitManager.sendPlatformCreationRequest(samplePlatform());

        assertNotNull(response);
        assertNotNull(response.getPlatform());
        assertNotNull(response.getPlatform().getPlatformId());
        assertEquals(200, response.getStatus());

    }

    @Test
    public void registerPlatformFail() throws Exception {
        
        ObjectMapper mapper = new ObjectMapper();
        String platformResponseString = mapper.writeValueAsString(samplePlatformResponseFail());

        RabbitManager rabbitManager = spy(new RabbitManager());
        doReturn(platformResponseString).when(rabbitManager).sendRpcMessage(any(), any(), any());

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

        ObjectMapper mapper = new ObjectMapper();
        String platformResponseString = mapper.writeValueAsString(newResponse);

        RabbitManager rabbitManager = spy(new RabbitManager());
        doReturn(platformResponseString).when(rabbitManager).sendRpcMessage(any(), any(), any());

        PlatformResponse response = rabbitManager.sendPlatformModificationRequest(newPlatform);

        assertNotNull(response);
        assertNotNull(response.getPlatform());
        assertNotNull(response.getPlatform().getPlatformId());
        assertEquals(newPlatform.getPlatformId(), response.getPlatform().getPlatformId());
        assertEquals("Changed description", response.getPlatform().getDescription());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void deletePlatform() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        String platformResponseString = mapper.writeValueAsString(samplePlatformResponseSuccess());

        RabbitManager rabbitManager = spy(new RabbitManager());
        doReturn(platformResponseString).when(rabbitManager).sendRpcMessage(any(), any(), any());

        PlatformResponse response = rabbitManager.sendPlatformRemovalRequest(sampleEmptyPlatform());

        assertNotNull(response);
        assertEquals(200, response.getStatus());
    }

    // ==== AAM Communication ====

    @Test
    public void registerPlatformOwnerSuccess() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        String platformOwnerResponseString = mapper.writeValueAsString(samplePlatformResponse());

        RabbitManager rabbitManager = spy(new RabbitManager());
        doReturn(platformOwnerResponseString).when(rabbitManager).sendRpcMessage(any(), any(), any());

        PlatformRegistrationRequest request = samplePlatformRequest();
        PlatformRegistrationResponse response = rabbitManager.sendPlatformRegistrationRequest(request);

        assertNotNull(response);
        assertEquals(request.getPlatformInstanceId(), response.getPlatformId());
    }

}