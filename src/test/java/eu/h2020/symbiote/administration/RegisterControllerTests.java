package eu.h2020.symbiote.administration;

import eu.h2020.symbiote.administration.controllers.RegisterController;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.Filter;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Test class for use in testing MVC and form validation.
 */
public class RegisterControllerTests extends AdministrationBaseTestClass {

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private Filter springSecurityFilterChain;

    @Autowired
    private RegisterController registerController;

    private MockMvc mockMvc;

    @Before
    public void setup(){

        this.mockMvc = MockMvcBuilders
            .webAppContextSetup(this.wac)
            .addFilters(springSecurityFilterChain)
            .build();

        MockitoAnnotations.initMocks(this);

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
                .andExpect(jsonPath("$.validationErrors.validUsername").value(notNullValidationMessage))
                .andExpect(jsonPath("$.validationErrors.validPassword").value(notNullValidationMessage))
                .andExpect(jsonPath("$.validationErrors.recoveryMail").value(notNullValidationMessage))
                .andExpect(jsonPath("$.validationErrors.role").value(notNullValidationMessage));
    }

    @Test
    public void credentialsFewerThanMinLength() throws Exception {
        // Username and password are only 3 characters
        mockMvc.perform(post("/administration/register")
                .with(csrf().asHeader())
                .param("validUsername", "val")
                .param("validPassword", "val")
                .param("recoveryMail", mail)
                .param("role", "SERVICE_OWNER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.validUsername")
                        .value(userIdValidationMessage))
                .andExpect(jsonPath("$.validationErrors.validPassword")
                        .value("Length must be between 4 and 30 characters"));
    }

    @Test
    public void credentialsFewerThanMaxLength() throws Exception {
        // Username and password are 31 characters
        mockMvc.perform(post("/administration/register")
                .with(csrf().asHeader())
                .param("validUsername", String.join("", String.join("", Collections.nCopies(11, "val")), "1"))
                .param("validPassword", String.join("", String.join("", Collections.nCopies(11, "val")), "1"))
                .param("recoveryMail", mail)
                .param("role", "SERVICE_OWNER"))
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
        doReturn(null).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/register")
                .with(csrf().asHeader())
                .param("validUsername", username)
                .param("validPassword", password)
                .param("recoveryMail", mail)
                .param("role", "SERVICE_OWNER"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorMessage").value("Authorization Manager is unreachable!"));
        }

    @Test
    public void postRegisterSuccess() throws Exception {

        doReturn(ManagementStatus.OK).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/register")
                .with(csrf().asHeader())
                .param("validUsername", username)
                .param("validPassword", password)
                .param("recoveryMail", mail)
                .param("role", "SERVICE_OWNER"))
                .andExpect(status().isCreated());
    }

    @Test
    public void postRegisterUsernameExists() throws Exception {

        doReturn(ManagementStatus.USERNAME_EXISTS).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/register")
                .with(csrf().asHeader())
                .param("validUsername", username)
                .param("validPassword", password)
                .param("recoveryMail", mail)
                .param("role", "SERVICE_OWNER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Username exist!"));
        }

    @Test
    public void postRegisterAAMError() throws Exception {

        doReturn(ManagementStatus.ERROR).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/register")
                .with(csrf().asHeader())
                .param("validUsername", username)
                .param("validPassword", password)
                .param("recoveryMail", mail)
                .param("role", "SERVICE_OWNER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Authorization Manager responded with ERROR!"));
        }

    @Test
    public void postRegisterCommunicationException() throws Exception {

        doThrow(sampleCommunicationException()).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/register")
                .with(csrf().asHeader())
                .param("validUsername", username)
                .param("validPassword", password)
                .param("recoveryMail", mail)
                .param("role", "SERVICE_OWNER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("SAMPLE_ERROR"));
        }
}