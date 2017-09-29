package eu.h2020.symbiote.administration;

import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import eu.h2020.symbiote.administration.controllers.Register;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;

import org.junit.Before;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.Filter;

import java.util.Collections;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Test class for use in testing MVC and form validation.
 */
public class RegisterTests extends AdministrationTests {

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

        Register registerController = appContext.getBean(Register.class);
        registerController.setRabbitManager(mockRabbitManager);

    }

    @Test
    public void getHomePage() throws Exception {

        mockMvc.perform(get("/"))
            .andExpect(status().isOk());
            // .andDo(MockMvcResultHandlers.print());
    }

    @Test
    public void getRegisterPage() throws Exception {

        mockMvc.perform(get("/register"))
            .andExpect(status().isOk());
    }

    @Test
    public void postRegisterErrors() throws Exception {

        // Everything is NULL
        mockMvc.perform(post("/register"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("coreUser", "validUsername", "NotNull"))
                .andExpect(model().attributeHasFieldErrorCode("coreUser", "validPassword", "NotNull"))
                .andExpect(model().attributeHasFieldErrorCode("coreUser", "recoveryMail", "NotNull"))
                .andExpect(model().attributeHasFieldErrorCode("coreUser", "role", "NotNull"));


        // Username and password are only 3 characters
        mockMvc.perform(post("/register")
                .param("validUsername", "val")
                .param("validPassword", "val")
                .param("recoveryMail", mail)
                .param("role", "PLATFORM_OWNER"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("coreUser", "validUsername", "Pattern"))
                .andExpect(model().attributeHasFieldErrorCode("coreUser", "validPassword", "Size"));

        // Username and password are 31 characters
        mockMvc.perform(post("/register")
                .param("validUsername", String.join("", String.join("", Collections.nCopies(11, "val")), "1"))
                .param("validPassword", String.join("", String.join("", Collections.nCopies(11, "val")), "1"))
                .param("recoveryMail", mail)
                .param("role", "PLATFORM_OWNER"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("coreUser", "validUsername", "Size"))
                .andExpect(model().attributeHasFieldErrorCode("coreUser", "validPassword", "Size"));

        // Wrong role
        mockMvc.perform(post("/register")
                .param("validUsername", username)
                .param("validPassword", password)
                .param("recoveryMail", mail)
                .param("role", "PLATFORM"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("coreUser", "role", "typeMismatch"));

        // NULL role
        mockMvc.perform(post("/register")
                .param("validUsername", username)
                .param("validPassword", password)
                .param("recoveryMail", mail)
                .param("role", "NULL"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error_role", "Choose a valid User Role"));
    }

    @Test
    public void postRegisterUnreachable() throws Exception {

        mockMvc.perform(post("/register")
                .param("validUsername", username)
                .param("validPassword", password)
                .param("recoveryMail", mail)
                .param("role", "PLATFORM_OWNER"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "Authorization Manager is unreachable!"));
    }

    @Test
    public void postRegisterSuccess() throws Exception {

        when(mockRabbitManager.sendUserManagementRequest(any())).thenReturn(ManagementStatus.OK);

        mockMvc.perform(post("/register")
                .param("validUsername", username)
                .param("validPassword", password)
                .param("recoveryMail", mail)
                .param("role", "PLATFORM_OWNER"))
                .andExpect(status().isOk())
                .andExpect(view().name("success"));
    }

    @Test
    public void postRegisterUsernameExists() throws Exception {

        when(mockRabbitManager.sendUserManagementRequest(any())).thenReturn(ManagementStatus.USERNAME_EXISTS);

        mockMvc.perform(post("/register")
                .param("validUsername", username)
                .param("validPassword", password)
                .param("recoveryMail", mail)
                .param("role", "PLATFORM_OWNER"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "Username exist!"));
    }

    @Test
    public void postRegisterAAMError() throws Exception {

        when(mockRabbitManager.sendUserManagementRequest(any())).thenReturn(ManagementStatus.ERROR);

        mockMvc.perform(post("/register")
                .param("validUsername", username)
                .param("validPassword", password)
                .param("recoveryMail", mail)
                .param("role", "PLATFORM_OWNER"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "Authorization Manager responded with ERROR!"));
    }

    @Test
    public void postRegisterCommunicationException() throws Exception {

        when(mockRabbitManager.sendUserManagementRequest(any())).thenThrow(sampleCommunicationException());

        mockMvc.perform(post("/register")
                .param("validUsername", username)
                .param("validPassword", password)
                .param("recoveryMail", mail)
                .param("role", "PLATFORM_OWNER"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "SAMPLE_ERROR"));
    }
}