package eu.h2020.symbiote;

import javax.servlet.Filter;
import org.junit.Test;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.thymeleaf.spring4.view.ThymeleafViewResolver;
import org.thymeleaf.spring4.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import eu.h2020.symbiote.communication.RabbitManager;
import eu.h2020.symbiote.controller.Login;
import eu.h2020.symbiote.controller.Register;
import eu.h2020.symbiote.controller.Cpanel;

import org.springframework.security.core.context.SecurityContextHolder;



public class WebAppTests extends AdministrationTests {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private Filter springSecurityFilterChain;

    protected MockMvc mockMvc;
    protected MockMvc controllerMockMvc;

    @Mock
    RabbitManager mockRabbitManager;

    @InjectMocks
    Login loginController;
    @InjectMocks
    Register registerController;
    @InjectMocks
    Cpanel cpanelController;


    @Before
    public void setup(){

        this.mockMvc = MockMvcBuilders
            .webAppContextSetup(this.wac)
            .addFilters(springSecurityFilterChain)
            .build();

        MockitoAnnotations.initMocks(this);

        ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver();
		templateResolver.setTemplateMode("HTML5");
		templateResolver.setPrefix("templates/");
		templateResolver.setSuffix(".html");

		SpringTemplateEngine engine = new SpringTemplateEngine();
		engine.setTemplateResolver(templateResolver);

		ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
		viewResolver.setTemplateEngine(engine);

        this.controllerMockMvc = MockMvcBuilders
            .standaloneSetup(loginController, registerController, cpanelController)
            .addFilters(springSecurityFilterChain)
            .setViewResolvers(viewResolver)
            .build();

    }

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

    @Test
    public void postPlatformRegisterSuccess() throws Exception {

        when(mockRabbitManager.sendPlatformRegistrationRequest(any())).thenReturn(samplePlatformResponse());
        
        controllerMockMvc.perform(post("/platform/register")
                .param("validUsername", username)
                .param("validPassword", password)
                .param("platformUrl", url.replace("https://","http://"))
                .param("platformName", name)
                .param("recoveryMail", mail)
                .param("federatedId", federatedId)
                .param("platformId", platformId) )
            .andExpect(status().isOk())
            .andExpect(view().name("success"));
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
    public void getControlPanelSuccessActive() throws Exception {

        when(mockRabbitManager.sendPlatformModificationRequest(any())).thenReturn(samplePlatformResponseSuccess());
        when(mockRabbitManager.sendDetailsRequest(any())).thenReturn(sampleOwnerDetails());

        controllerMockMvc.perform(get("/user/cpanel")
                .with(authentication(sampleAuth())) )
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("user"))
            .andExpect(model().attribute("user", hasProperty("state", is(3))) );
    }

    @Test
    public void getControlPanelSuccessInactive() throws Exception {

        when(mockRabbitManager.sendPlatformModificationRequest(any())).thenReturn(samplePlatformResponseFail());
        when(mockRabbitManager.sendDetailsRequest(any())).thenReturn(sampleOwnerDetails());

        controllerMockMvc.perform(get("/user/cpanel")
                .with(authentication(sampleAuth())) )
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("user"))
            .andExpect(model().attribute("user", hasProperty("state", is(2))) );
    }

    @Test
    public void getControlPanelAsUserCommunicationError() throws Exception {

        mockMvc.perform( get("/user/cpanel").with(authentication(sampleAuth())) )
            .andExpect(model().attribute("user", hasProperty("state", is(0))) );
    }



    @Test
    public void postActivatePlatformWithErrors() throws Exception {

        mockMvc.perform( post("/user/cpanel/activate")
                .param("description", "ER")
                .with(authentication(sampleAuth()))
                .with(csrf().asHeader()) )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/user/cpanel"))
            .andExpect(flash().attribute("page", "activate") )
            .andExpect(flash().attribute("error_description", "Size must be between 3 and 300") );
    }

    @Test
    public void postActivatePlatformTimeout() throws Exception {

        mockMvc.perform( post("/user/cpanel/activate")
                .with(authentication(sampleAuth()))
                .with(csrf().asHeader()) )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/user/cpanel"))
            .andExpect(flash().attribute("error", "Error During Activation!") );
    }


    @Test
    public void postModifyPlatformWithErrors() throws Exception {

        mockMvc.perform( post("/user/cpanel/modify")
                .param("description", "ER")
                .with(authentication(sampleAuth()))
                .with(csrf().asHeader()) )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/user/cpanel"))
            .andExpect(flash().attribute("page", "modify") )
            .andExpect(flash().attribute("error_description", "Size must be between 3 and 300") );
    }

    @Test
    public void postModifyPlatformTimeout() throws Exception {

        mockMvc.perform( post("/user/cpanel/modify")
                .with(authentication(sampleAuth()))
                .with(csrf().asHeader()) )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/user/cpanel"))
            .andExpect(flash().attribute("error", "Error During Activation!") );
    }

    @Test
    public void postDisablePlatformTimeout() throws Exception {

        mockMvc.perform( post("/user/cpanel/disable")
                .with(authentication(sampleAuth()))
                .with(csrf().asHeader()) )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/user/cpanel"))
            .andExpect(flash().attribute("error", "Authorization Manager is unreachable!") );
    }
}