package eu.h2020.symbiote.administration;

import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import eu.h2020.symbiote.administration.controllers.UserCpanel;
import eu.h2020.symbiote.administration.controllers.Register;
import eu.h2020.symbiote.security.commons.enums.UserRole;

import org.junit.Before;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.Filter;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Test class for use in testing MVC and form validation.
 */
public class LoginTests extends AdministrationBaseTestClass {

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

        UserCpanel userCpanelController = appContext.getBean(UserCpanel.class);
        userCpanelController.setRabbitManager(mockRabbitManager);
    }

    @Test
    public void getLoginPage() throws Exception {

        mockMvc.perform(get("/administration/user/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/administration"));
    }

    @Test
    public void getLoginPageAsUser() throws Exception {

        mockMvc.perform(get("/administration/user/login").with(authentication(sampleUserAuth(UserRole.USER))) )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/administration/user/cpanel"));
    }

    @Test
    public void postLoginPage() throws Exception {

        when(mockRabbitManager.sendLoginRequest(any())).thenReturn(sampleUserDetailsResponse(HttpStatus.OK));

        mockMvc.perform(post("/administration/user/login")
            .with(csrf().asHeader())
                .param("username", username)
                .param("password", password) )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/administration")); // React-router takes care of redirecting to "/administration/user/cpanel"
    }

    @Test
    public void postLogoutPage() throws Exception {

        mockMvc.perform(post("/administration/user/logout").with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER))) )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/administration"));
    }

    @Test
    public void getControlPanelDenied() throws Exception {

        mockMvc.perform(get("/administration/user/cpanel"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/administration/user/login"));
    }
}