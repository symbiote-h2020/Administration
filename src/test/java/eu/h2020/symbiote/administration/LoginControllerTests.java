package eu.h2020.symbiote.administration;

import eu.h2020.symbiote.administration.controllers.UserCpanelController;
import eu.h2020.symbiote.administration.exceptions.authentication.*;
import eu.h2020.symbiote.security.commons.enums.AccountStatus;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.communication.payloads.UserDetailsResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.Filter;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Test class for use in testing MVC and form validation.
 */
public class LoginControllerTests extends AdministrationBaseTestClass {

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private Filter springSecurityFilterChain;

    @Autowired
    private UserCpanelController userCpanelController;

    private MockMvc mockMvc;

    @Before
    public void setup() {

        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(this.wac)
                .addFilters(springSecurityFilterChain)
                .build();

        MockitoAnnotations.initMocks(this);
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
    public void postLoginPageActiveUser() throws Exception {

        doReturn(sampleActiveUserDetailsResponse(HttpStatus.OK)).when(rabbitManager).sendLoginRequest(any());

        mockMvc.perform(post("/administration/user/login")
            .with(csrf().asHeader())
                .param("username", username)
                .param("password", password) )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/administration")); // React-router takes care of redirecting to "/administration/user/cpanel"
    }

    @Test
    public void postLoginWrongUsername() throws Exception {

        doReturn(sampleActiveUserDetailsResponse(HttpStatus.BAD_REQUEST)).when(rabbitManager).sendLoginRequest(any());

        mockMvc.perform(post("/administration/user/login")
                .with(csrf().asHeader())
                .param("username", username)
                .param("password", password) )
                .andExpect(status().isBadRequest())
                .andExpect(content().string(new WrongUserNameException().getMessage()));
    }

    @Test
    public void postLoginWrongUserPassword() throws Exception {

        doReturn(sampleActiveUserDetailsResponse(HttpStatus.UNAUTHORIZED)).when(rabbitManager).sendLoginRequest(any());

        mockMvc.perform(post("/administration/user/login")
                .with(csrf().asHeader())
                .param("username", username)
                .param("password", password) )
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(new WrongUserPasswordException().getMessage()));
    }

    @Test
    public void postLoginWrongAdminPassword() throws Exception {

        UserDetailsResponse response = new UserDetailsResponse(HttpStatus.FORBIDDEN, null);
        doReturn(response).when(rabbitManager).sendLoginRequest(any());

        mockMvc.perform(post("/administration/user/login")
                .with(csrf().asHeader())
                .param("username", username)
                .param("password", password) )
                .andExpect(status().isForbidden())
                .andExpect(content().string(new WrongAdminPasswordException().getMessage()));
    }

    @Test
    public void postLoginNewUser() throws Exception {

        doReturn(sampleUserDetailsResponse(HttpStatus.FORBIDDEN, AccountStatus.NEW)).when(rabbitManager).sendLoginRequest(any());

        mockMvc.perform(post("/administration/user/login")
                .with(csrf().asHeader())
                .param("username", username)
                .param("password", password) )
                .andExpect(status().isForbidden())
                .andExpect(content().string(new InactiveAccountException().getMessage()));
    }

    @Test
    public void postLoginBlockedUser() throws Exception {

        doReturn(sampleUserDetailsResponse(HttpStatus.FORBIDDEN, AccountStatus.ACTIVITY_BLOCKED)).when(rabbitManager).sendLoginRequest(any());

        mockMvc.perform(post("/administration/user/login")
                .with(csrf().asHeader())
                .param("username", username)
                .param("password", password) )
                .andExpect(status().isForbidden())
                .andExpect(content().string(new ActivityBlockedException().getMessage()));

    }

    @Test
    public void postLoginConsentBlockedUser() throws Exception {

        doReturn(sampleUserDetailsResponse(HttpStatus.FORBIDDEN, AccountStatus.CONSENT_BLOCKED)).when(rabbitManager).sendLoginRequest(any());

        mockMvc.perform(post("/administration/user/login")
                .with(csrf().asHeader())
                .param("username", username)
                .param("password", password) )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/administration")); // React-router takes care of redirecting to "/administration/user/cpanel"
    }

    @Test
    public void postLogoutPage() throws Exception {

        mockMvc.perform(post("/administration/user/logout").with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER))) )
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