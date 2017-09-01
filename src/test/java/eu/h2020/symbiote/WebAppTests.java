package eu.h2020.symbiote;

import javax.servlet.Filter;

import eu.h2020.symbiote.administration.CustomAuthenticationProvider;
import org.junit.Test;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import eu.h2020.symbiote.administration.controllers.Register;
import eu.h2020.symbiote.administration.controllers.Cpanel;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.hamcrest.Matchers.*;





/**
 * Test class for use in testing MVC and form validation.
 */
public class WebAppTests extends AdministrationTests {

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private Filter springSecurityFilterChain;

    private MockMvc mockMvc;

    @Mock
    private RabbitManager mockRabbitManager;

    @Before
    public void setup(){

        this.mockMvc = MockMvcBuilders
            .webAppContextSetup(this.wac)
            .addFilters(springSecurityFilterChain)
            .build();

        MockitoAnnotations.initMocks(this);

        CustomAuthenticationProvider provider = appContext.getBean(CustomAuthenticationProvider.class);
        provider.setRabbitManager(mockRabbitManager);

        Register registerController = appContext.getBean(Register.class);
        registerController.setRabbitManager(mockRabbitManager);

        Cpanel cpanelController = appContext.getBean(Cpanel.class);
        cpanelController.setRabbitManager(mockRabbitManager);
    }

    @Test
    public void getHomePage() throws Exception {
        
        mockMvc.perform(get("/"))
            .andExpect(status().isOk());
            // .andDo(MockMvcResultHandlers.print());
    }

    @Test
    public void getPlatormRegisterPage() throws Exception {
        
        mockMvc.perform(get("/register/platform"))
            .andExpect(status().isOk());
    }

    @Test
    public void postPlatformRegisterErrors() throws Exception {
        
        mockMvc.perform(post("/register/platform"))
            .andExpect(status().isOk())
            .andExpect(model().hasErrors());
    }

    @Test
    public void postPlatformRegisterUnreachable() throws Exception {
        
        mockMvc.perform(post("/register/platform")
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
        
        mockMvc.perform(post("/register/platform")
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
        
        mockMvc.perform(get("/register/app"))
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
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/user/cpanel"));
    }

    @Test
    public void postLoginPage() throws Exception {
        
        when(mockRabbitManager.sendLoginRequest(any())).thenReturn(sampleToken());

        mockMvc.perform(post("/user/login")
            .with(csrf().asHeader())
                .param("username", username)
                .param("password", password) )
            .andExpect(status().is3xxRedirection());
            // Todo: Fix it
        // .andExpect(redirectedUrl("/user/cpanel"));
    }

    @Test
    public void getLogoutPage() throws Exception {
        
        mockMvc.perform(get("/user/logout").with(authentication(sampleAuth())) )
            .andExpect(status().is3xxRedirection());
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

        mockMvc.perform(get("/user/cpanel")
                .with(authentication(sampleAuth())) )
            .andExpect(status().isOk());
            // .andExpect(model().attributeExists("user"))
            // .andExpect(model().attribute("user", hasProperty("state", is(3))) );
    }

    @Test
    public void getControlPanelSuccessInactive() throws Exception {

        when(mockRabbitManager.sendPlatformModificationRequest(any())).thenReturn(samplePlatformResponseFail());
        when(mockRabbitManager.sendDetailsRequest(any())).thenReturn(sampleOwnerDetails());

        mockMvc.perform(get("/user/cpanel")
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
    public void postActivatePlatformSuccess() throws Exception {

        when(mockRabbitManager.sendPlatformCreationRequest(any())).thenReturn(samplePlatformResponseSuccess());

        mockMvc.perform( post("/user/cpanel/activate")
                .param("description", description)
                .param("informationModelId", informationModelId)
                .with(authentication(sampleAuth()))
                .with(csrf().asHeader()) )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/user/cpanel"))
            .andExpect(flash().attribute("error", nullValue() ) )
            .andExpect(flash().attribute("error_description", nullValue() ) )
            .andExpect(flash().attribute("error_informationModelId", nullValue() ) );
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
    public void postModifyPlatformSuccess() throws Exception {

        when(mockRabbitManager.sendPlatformModificationRequest(any())).thenReturn(samplePlatformResponseSuccess());

        mockMvc.perform( post("/user/cpanel/modify")
                .param("description", "Modified Description")
                .with(authentication(sampleAuth()))
                .with(csrf().asHeader()) )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/user/cpanel"))
            .andExpect(flash().attribute("error", nullValue() ) )
            .andExpect(flash().attribute("error_description", nullValue() ) );
    }

    @Test
    public void postDisablePlatformTimeout() throws Exception {

        mockMvc.perform( post("/user/cpanel/disable")
                .with(authentication(sampleAuth()))
                .with(csrf().asHeader()) )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/user/cpanel"))
            .andExpect(flash().attribute("error", "Registry is unreachable!") );
    }

    @Test
    public void postDisablePlatformSuccess() throws Exception {

        when(mockRabbitManager.sendPlatformRemovalRequest(any())).thenReturn(samplePlatformResponseSuccess());

        mockMvc.perform( post("/user/cpanel/disable")
                .param("description", "Modified Description")
                .with(authentication(sampleAuth()))
                .with(csrf().asHeader()) )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/user/cpanel"))
            .andExpect(flash().attribute("error", nullValue() ) );
    }
}