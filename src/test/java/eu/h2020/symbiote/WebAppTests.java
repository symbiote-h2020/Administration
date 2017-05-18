package eu.h2020.symbiote;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.InjectMocks;

import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import eu.h2020.symbiote.communication.RabbitManager;
import eu.h2020.symbiote.controller.Login;



public class WebAppTests extends AdministrationTests {

    @Mock
    RabbitManager rabbitManager;

    @Test
    public void getHomePage() throws Exception {
        
        mockMvc.perform(get("/"))
            .andExpect(status().isOk());
            // .andDo(MockMvcResultHandlers.print());
    }

    @Test
    public void getPlatormRegisterPage() throws Exception {
        
        mockMvc.perform(get("/platform/register"))
            .andExpect(status().isOk());
    }

    @Test
    public void postPlatformRegisterErrors() throws Exception {
        
        mockMvc.perform(post("/platform/register"))
            .andExpect(status().isOk())
            .andExpect(model().hasErrors());
    }

    @Test
    public void postPlatformRegisterUnreachable() throws Exception {
        
        mockMvc.perform(post("/platform/register")
        		.param("validUsername", username)
        		.param("validPassword", password)
        		.param("platformUrl", url)
        		.param("platformName", name)
        		.param("recoveryMail", mail)
        		.param("federatedId", federatedId)
        		.param("platformId", platformId) )
            .andExpect(status().isOk())
            .andExpect(model().attribute("error", "Authorization Manager is unreachable!") );
    }

    // @Test
    // public void postPlatformRegisterSuccess() throws Exception {

    // 	when(rabbitManager.sendPlatformRegistrationRequest(any())).thenReturn(samplePlatformResponse());
        
    //     mockMvc.perform(post("/platform/register")
    //     		.param("validUsername", username)
    //     		.param("validPassword", password)
    //     		.param("platformUrl", url)
    //     		.param("platformName", name)
    //     		.param("recoveryMail", mail)
    //     		.param("federatedId", federatedId)
    //     		.param("platformId", platformId) )
    //         // .andExpect(status().isOk());
    //         .andDo(MockMvcResultHandlers.print());
    // }

    @Test
    public void getAppRegisterPage() throws Exception {
        
        mockMvc.perform(get("/platform/register"))
            .andExpect(status().isOk());
    }

    @Test
    public void getLoginPage() throws Exception {
        
        mockMvc.perform(get("/user/login"))
            .andExpect(status().isOk());
    }

    @Test
    public void getLoginPageAsUser() throws Exception {
        
        mockMvc.perform(get("/user/login").with(authentication(sampleAuth())) )
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/user/cpanel"));
    }

    @Test
    public void getLogoutPage() throws Exception {
        
        mockMvc.perform(get("/user/logout").with(authentication(sampleAuth())) )
            .andExpect(status().isOk());
    }

    @Test
    public void getDeniedPage() throws Exception {
        
        mockMvc.perform(get("/denied"))
            .andExpect(status().isOk());
    }

    // @Test
    // public void getErrorPage() throws Exception {
        
    //     mockMvc.perform(get("/error"));
    // }

    @Test
    public void getControlPanelDenied() throws Exception {
        
        mockMvc.perform(get("/user/cpanel"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/user/login"));
    }

    @Test
    public void getControlPanelAsUserCommunicationError() throws Exception {

        mockMvc.perform( get("/user/cpanel").with(authentication(sampleAuth())) )
            .andExpect(model().attribute("user", hasProperty("state", is(0))) );
    }

}