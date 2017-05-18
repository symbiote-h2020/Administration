package eu.h2020.symbiote;

import org.junit.Test;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.hamcrest.Matchers.*;

import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;



public class WebAppTests extends AdministrationTests {


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

        Authentication auth = new UsernamePasswordAuthenticationToken(sampleCoreUser(), null, sampleAuths());

        mockMvc.perform( get("/user/cpanel").with(authentication(auth)) )
            .andExpect(model().attribute("user", hasProperty("state", is(0))) );
    }

}