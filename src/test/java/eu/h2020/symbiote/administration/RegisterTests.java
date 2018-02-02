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

import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Test class for use in testing MVC and form validation.
 */
public class RegisterTests extends AdministrationBaseTestClass {

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

        mockMvc.perform(get("/administration"))
            .andExpect(status().isOk());
            // .andDo(MockMvcResultHandlers.print());
    }

    @Test
    public void getRegisterPage() throws Exception {

        mockMvc.perform(get("/administration/register"))
            .andExpect(status().isOk());
    }

    @Test public void everythingIsNull() throws Exception {
        mockMvc.perform(post("/administration/register")
                .with(csrf().asHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.validUsername").value("may not be null"))
                .andExpect(jsonPath("$.validationErrors.validPassword").value("may not be null"))
                .andExpect(jsonPath("$.validationErrors.recoveryMail").value("may not be null"))
                .andExpect(jsonPath("$.validationErrors.role").value("may not be null"));
    }

    @Test
    public void credentialsFewerThanMinLength() throws Exception {
        // Username and password are only 3 characters
        mockMvc.perform(post("/administration/register")
                .with(csrf().asHeader())
                .param("validUsername", "val")
                .param("validPassword", "val")
                .param("recoveryMail", mail)
                .param("role", "PLATFORM_OWNER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.validUsername").value("must match \"^[\\w-]{4,}$\""))
                .andExpect(jsonPath("$.validationErrors.validPassword").value("Length must be between 4 and 30 characters"));
    }

    @Test
    public void credentialsFewerThanMaxLength() throws Exception {
        // Username and password are 31 characters
        mockMvc.perform(post("/administration/register")
                .with(csrf().asHeader())
                .param("validUsername", String.join("", String.join("", Collections.nCopies(11, "val")), "1"))
                .param("validPassword", String.join("", String.join("", Collections.nCopies(11, "val")), "1"))
                .param("recoveryMail", mail)
                .param("role", "PLATFORM_OWNER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.validUsername").value("Length must be between 0 and 30 characters"))
                .andExpect(jsonPath("$.validationErrors.validPassword").value("Length must be between 4 and 30 characters"));
    }

    @Test
    public void wrongRole() throws Exception {
        mockMvc.perform(post("/administration/register")
                .with(csrf().asHeader())
                .param("validUsername", username)
                .param("validPassword", password)
                .param("recoveryMail", mail)
                .param("role", "PLATFORM"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.role").value(containsString("Failed to convert property value of type")));
    }

    @Test
    public void nullRole() throws Exception {
        mockMvc.perform(post("/administration/register")
                .with(csrf().asHeader())
                .param("validUsername", username)
                .param("validPassword", password)
                .param("recoveryMail", mail)
                .param("role", "NULL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.role").value("Invalid User Role"));
    }

    @Test
    public void postRegisterUnreachable() throws Exception {
        mockMvc.perform(post("/administration/register")
                .with(csrf().asHeader())
                .param("validUsername", username)
                .param("validPassword", password)
                .param("recoveryMail", mail)
                .param("role", "PLATFORM_OWNER"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorMessage").value("Authorization Manager is unreachable!"));
        }

    @Test
    public void postRegisterSuccess() throws Exception {

        when(mockRabbitManager.sendUserManagementRequest(any())).thenReturn(ManagementStatus.OK);

        mockMvc.perform(post("/administration/register")
                .with(csrf().asHeader())
                .param("validUsername", username)
                .param("validPassword", password)
                .param("recoveryMail", mail)
                .param("role", "PLATFORM_OWNER"))
                .andExpect(status().isCreated());
    }

    @Test
    public void postRegisterUsernameExists() throws Exception {

        when(mockRabbitManager.sendUserManagementRequest(any())).thenReturn(ManagementStatus.USERNAME_EXISTS);

        mockMvc.perform(post("/administration/register")
                .with(csrf().asHeader())
                .param("validUsername", username)
                .param("validPassword", password)
                .param("recoveryMail", mail)
                .param("role", "PLATFORM_OWNER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Username exist!"));
        }

    @Test
    public void postRegisterAAMError() throws Exception {

        when(mockRabbitManager.sendUserManagementRequest(any())).thenReturn(ManagementStatus.ERROR);

        mockMvc.perform(post("/administration/register")
                .with(csrf().asHeader())
                .param("validUsername", username)
                .param("validPassword", password)
                .param("recoveryMail", mail)
                .param("role", "PLATFORM_OWNER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Authorization Manager responded with ERROR!"));
        }

    @Test
    public void postRegisterCommunicationException() throws Exception {

        when(mockRabbitManager.sendUserManagementRequest(any())).thenThrow(sampleCommunicationException());

        mockMvc.perform(post("/administration/register")
                .with(csrf().asHeader())
                .param("validUsername", username)
                .param("validPassword", password)
                .param("recoveryMail", mail)
                .param("role", "PLATFORM_OWNER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("SAMPLE_ERROR"));
        }
}