package eu.h2020.symbiote.administration;

import eu.h2020.symbiote.administration.controllers.implementations.RegisterControllerImpl;
import eu.h2020.symbiote.administration.controllers.interfaces.RegisterController;
import eu.h2020.symbiote.administration.exceptions.rabbit.CommunicationException;
import eu.h2020.symbiote.administration.model.VerificationToken;
import eu.h2020.symbiote.administration.repository.VerificationTokenRepository;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.Filter;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

    @Autowired
    private VerificationTokenRepository tokenRepository;

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

    @Test
    public void confirmAccountTokenNotFound() throws Exception {
        String token = "dummy";

        mockMvc.perform(get("/administration/registrationConfirm")
                .with(csrf().asHeader())
                .param("token", token))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("The token " + token + " was not found. Get a new verification token"));
    }

    @Test
    public void confirmAccountTokenExpired() throws Exception {
        VerificationToken verificationToken = createAndStoreVerificationToken(TokenStatus.INVALID);

        mockMvc.perform(get("/administration/registrationConfirm")
                .with(csrf().asHeader())
                .param("token", verificationToken.getToken()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("The token " + verificationToken.getToken() + " has been expired"));
    }

    @Test
    public void confirmAccountAAMUnreachable() throws Exception {
        doReturn(null).when(rabbitManager).sendUserManagementRequest(any());
        VerificationToken verificationToken = createAndStoreVerificationToken(TokenStatus.VALID);

        mockMvc.perform(get("/administration/registrationConfirm")
                .with(csrf().asHeader())
                .param("token", verificationToken.getToken()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorMessage").value("The component AAM is unreachable"));
    }

    @Test
    public void confirmAccountAAMError() throws Exception {
        doReturn(ManagementStatus.ERROR).when(rabbitManager).sendUserManagementRequest(any());
        VerificationToken verificationToken = createAndStoreVerificationToken(TokenStatus.VALID);

        mockMvc.perform(get("/administration/registrationConfirm")
                .with(csrf().asHeader())
                .param("token", verificationToken.getToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("An error occurred : ERROR"));
    }

    @Test
    public void confirmAccountAAMCommunicationException() throws Exception {
        doThrow(new CommunicationException("ERROR")).when(rabbitManager).sendUserManagementRequest(any());
        VerificationToken verificationToken = createAndStoreVerificationToken(TokenStatus.VALID);

        mockMvc.perform(get("/administration/registrationConfirm")
                .with(csrf().asHeader())
                .param("token", verificationToken.getToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("ERROR"));
    }

    @Test
    public void confirmAccountSuccess() throws Exception {
        when(rabbitManager.sendUserManagementRequest(any())).thenReturn(ManagementStatus.OK);
        VerificationToken verificationToken = createAndStoreVerificationToken(TokenStatus.VALID);

        mockMvc.perform(get("/administration/registrationConfirm")
                .with(csrf().asHeader())
                .param("token", verificationToken.getToken()))
                .andExpect(status().isOk())
                .andExpect(content().string(RegisterControllerImpl.USER_ACCOUNT_ACTIVATED_MESSAGE));
    }

    private VerificationToken createAndStoreVerificationToken(TokenStatus tokenStatus) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Timestamp(cal.getTime().getTime()));

        VerificationToken verificationToken = null;

        switch (tokenStatus) {
            case VALID:
                cal.add(Calendar.HOUR, 1);
                verificationToken = new VerificationToken(
                        "validToken",
                        "validToken",
                        sampleCoreUser(UserRole.SERVICE_OWNER),
                        new Date(cal.getTime().getTime()));
                break;
            case INVALID:
                cal.add(Calendar.HOUR, -tokenExpirationTimeInHours);
                verificationToken = new VerificationToken(
                        "expiredTokenString",
                        "expiredTokenString",
                        sampleCoreUser(UserRole.SERVICE_OWNER),
                        new Date(cal.getTime().getTime()));
                break;
        }

        return tokenRepository.save(verificationToken);
    }

    private enum TokenStatus {
        VALID, INVALID;
    }
}