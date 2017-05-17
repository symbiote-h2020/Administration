package eu.h2020.symbiote;


import javax.servlet.Filter;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.context.junit4.SpringRunner;

import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;




@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
public class WebAppTests {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private Filter springSecurityFilterChain;

    private MockMvc mockMvc;

    @Before
    public void setup(){

        this.mockMvc = MockMvcBuilders
                        .webAppContextSetup(this.wac)
                        .addFilters(springSecurityFilterChain)
                        .build();
    }

    @Test
    public void getHomePage() throws Exception {
        
        ResultActions result = mockMvc.perform(get("/"))
                            .andExpect(status().isOk());

        // result.andDo(MockMvcResultHandlers.print());
    }

    @Test
    public void getPlatormRegisterPage() throws Exception {
        
        ResultActions result = mockMvc.perform(get("/platform/register"))
                            .andExpect(status().isOk());

        // result.andDo(MockMvcResultHandlers.print());
    }

    @Test
    public void getAppRegisterPage() throws Exception {
        
        ResultActions result = mockMvc.perform(get("/platform/register"))
                            .andExpect(status().isOk());

        // result.andDo(MockMvcResultHandlers.print());
    }

    @Test
    public void getLoginPage() throws Exception {
        
        ResultActions result = mockMvc.perform(get("/user/login"))
                            .andExpect(status().isOk());

        // result.andDo(MockMvcResultHandlers.print());
    }

    @Test
    public void getDeniedPage() throws Exception {
        
        ResultActions result = mockMvc.perform(get("/denied"))
                            .andExpect(status().isOk());

        // result.andDo(MockMvcResultHandlers.print());
    }

    // @Test
    // public void getErrorPage() throws Exception {
        
    //     ResultActions result = mockMvc.perform(get("/error"));
    //                         // .andExpect(status().isOk());

    //     result.andDo(MockMvcResultHandlers.print());
    // }

    @Test
    public void getControlPanelDenied() throws Exception {
        
        ResultActions result = mockMvc.perform(get("/user/cpanel"))
                            .andExpect(status().is3xxRedirection())
                            .andExpect(redirectedUrl("http://localhost/user/login"));

        // result.andDo(MockMvcResultHandlers.print());
    }

}
