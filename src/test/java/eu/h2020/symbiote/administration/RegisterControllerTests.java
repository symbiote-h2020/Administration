package eu.h2020.symbiote.administration;

import eu.h2020.symbiote.administration.controllers.interfaces.RegisterController;
import eu.h2020.symbiote.administration.exceptions.rabbit.CommunicationException;
import eu.h2020.symbiote.administration.exceptions.rabbit.EntityUnreachableException;
import eu.h2020.symbiote.administration.model.ResendVerificationEmailRequest;
import eu.h2020.symbiote.administration.model.ResetPasswordRequest;
import eu.h2020.symbiote.administration.model.VerificationToken;
import eu.h2020.symbiote.administration.repository.VerificationTokenRepository;
import eu.h2020.symbiote.security.commons.enums.AccountStatus;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.communication.payloads.Credentials;
import eu.h2020.symbiote.security.communication.payloads.UserDetails;
import eu.h2020.symbiote.security.communication.payloads.UserDetailsResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.Filter;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


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

    @Autowired
    private MessageSource messages;

    private MockMvc mockMvc;

    @Before
    public void setup(){

        this.mockMvc = MockMvcBuilders
            .webAppContextSetup(this.wac)
            .addFilters(springSecurityFilterChain)
            .build();

        MockitoAnnotations.initMocks(this);
        tokenRepository.deleteAll();
    }

    @After
    public void tearDown() {
        tokenRepository.deleteAll();
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
                .param("recoveryMail", email)
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
                .param("recoveryMail", email)
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
                .param("recoveryMail", email)
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
                .param("recoveryMail", email)
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
                .param("recoveryMail", email)
                .param("role", "SERVICE_OWNER"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorMessage").value("The component AAM is unreachable"));
    }

    @Test
    public void postRegisterSuccess() throws Exception {

        doReturn(ManagementStatus.OK).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/register")
                .header("Accept-Language", Locale.US.toString())
                .with(csrf().asHeader())
                .param("validUsername", username)
                .param("validPassword", password)
                .param("recoveryMail", email)
                .param("role", "SERVICE_OWNER"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.successMessage")
                        .value(messages.getMessage("message.verifyEmail", null, Locale.US)));

        TimeUnit.MILLISECONDS.sleep(200);

        List<VerificationToken> tokens = tokenRepository.findAll();
        assertEquals(1, tokens.size());
        assertEquals("", tokens.get(0).getUser().getValidPassword());
        assertEquals("placeholder", tokens.get(0).getUser().getPassword());
    }

    @Test
    public void replaceExistingToken() throws Exception {
        doReturn(ManagementStatus.OK).when(rabbitManager).sendUserManagementRequest(any());
        createAndStoreVerificationToken(TokenStatus.VALID);

        mockMvc.perform(post("/administration/register")
                .header("Accept-Language", Locale.US.toString())
                .with(csrf().asHeader())
                .param("validUsername", username)
                .param("validPassword", password)
                .param("recoveryMail", email)
                .param("role", "SERVICE_OWNER"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.successMessage")
                        .value(messages.getMessage("message.verifyEmail", null, Locale.US)));

        TimeUnit.MILLISECONDS.sleep(200);
        assertEquals(1, tokenRepository.findAll().size());
    }

    @Test
    public void postRegisterUsernameExists() throws Exception {

        doReturn(ManagementStatus.USERNAME_EXISTS).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/register")
                .with(csrf().asHeader())
                .param("validUsername", username)
                .param("validPassword", password)
                .param("recoveryMail", email)
                .param("role", "SERVICE_OWNER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("An error occurred : Username exists!"));
    }

    @Test
    public void postRegisterAAMError() throws Exception {

        doReturn(ManagementStatus.ERROR).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/register")
                .with(csrf().asHeader())
                .param("validUsername", username)
                .param("validPassword", password)
                .param("recoveryMail", email)
                .param("role", "SERVICE_OWNER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("An error occurred : ERROR"));
    }

    @Test
    public void postRegisterCommunicationException() throws Exception {

        doThrow(sampleCommunicationException()).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/register")
                .with(csrf().asHeader())
                .param("validUsername", username)
                .param("validPassword", password)
                .param("recoveryMail", email)
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
                .andExpect(view().name("error"));
    }

    @Test
    public void confirmAccountTokenExpired() throws Exception {
        VerificationToken verificationToken = createAndStoreVerificationToken(TokenStatus.INVALID);

        mockMvc.perform(get("/administration/registrationConfirm")
                .with(csrf().asHeader())
                .param("token", verificationToken.getToken()))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"));

        assertEquals(0, tokenRepository.findAll().size());
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

        assertEquals(1, tokenRepository.findAll().size());
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

        assertEquals(1, tokenRepository.findAll().size());
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

        assertEquals(1, tokenRepository.findAll().size());
    }

    @Test
    public void confirmAccountSuccess() throws Exception {
        doReturn(ManagementStatus.OK).when(rabbitManager).sendUserManagementRequest(any());
        VerificationToken verificationToken = createAndStoreVerificationToken(TokenStatus.VALID);

        mockMvc.perform(get("/administration/registrationConfirm")
                .with(csrf().asHeader())
                .param("token", verificationToken.getToken()))
                .andExpect(status().isOk())
                .andExpect(view().name("email_verification_success"));

        assertEquals(0, tokenRepository.findAll().size());
    }

    @Test
    public void resetPasswordGetUserDetailsAAMUnreachable() throws Exception {
        doReturn(null).when(rabbitManager).sendForceReadRequest(any());

        mockMvc.perform(post("/administration/forgot_password")
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleResetPasswordRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.resetPasswordError")
                        .value("The component AAM is unreachable"));

    }

    @Test
    public void resetPasswordGetUserDetailsCommunicationException() throws Exception {
        doThrow(sampleCommunicationException()).when(rabbitManager).sendForceReadRequest(any());

        mockMvc.perform(post("/administration/forgot_password")
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleResetPasswordRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resetPasswordError")
                        .value("SAMPLE_ERROR"));
    }

    @Test
    public void resetPasswordGetUserDetailsNoSuchUserError() throws Exception {
        doReturn(sampleUserDetailsResponse(HttpStatus.BAD_REQUEST)).when(rabbitManager).sendForceReadRequest(any());

        mockMvc.perform(post("/administration/forgot_password")
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleResetPasswordRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resetPasswordError")
                        .value("Username does not exist!"));

    }

    @Test
    public void resetPasswordGetUserDetailsWrongAdminPassword() throws Exception {
        UserDetailsResponse response = new UserDetailsResponse(HttpStatus.FORBIDDEN, null);
        doReturn(response).when(rabbitManager).sendForceReadRequest(any());

        mockMvc.perform(post("/administration/forgot_password")
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleResetPasswordRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resetPasswordError")
                        .value("Wrong admin password!"));

    }

    @Test
    public void resetPasswordWrongCredentials() throws Exception {
        doReturn(sampleUserDetailsResponse(HttpStatus.OK)).when(rabbitManager).sendForceReadRequest(any());
        ResetPasswordRequest request = new ResetPasswordRequest(username, "dummyEmail");

        mockMvc.perform(post("/administration/forgot_password")
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resetPasswordError")
                        .value("No user with such credentials"));
    }

    @Test
    public void resetPasswordForceUpdateAAMUnreachable() throws Exception {
        doReturn(sampleUserDetailsResponse(HttpStatus.OK)).when(rabbitManager).sendForceReadRequest(any());
        doReturn(null).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/forgot_password")
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleResetPasswordRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.resetPasswordError")
                        .value("Authorization Manager is unreachable!"));

    }

    @Test
    public void resetPasswordForceUpdateCommunicationException() throws Exception {
        doReturn(sampleUserDetailsResponse(HttpStatus.OK)).when(rabbitManager).sendForceReadRequest(any());
        doThrow(sampleCommunicationException()).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/forgot_password")
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleResetPasswordRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resetPasswordError")
                        .value("SAMPLE_ERROR"));
    }

    @Test
    public void resetPasswordForceUpdateError() throws Exception {
        doReturn(sampleUserDetailsResponse(HttpStatus.OK)).when(rabbitManager).sendForceReadRequest(any());
        doReturn(ManagementStatus.ERROR).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/forgot_password")
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleResetPasswordRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resetPasswordError")
                        .value("The Authorization Manager responded with " + ManagementStatus.ERROR));

    }

    @Test
    public void resetPasswordGetUserDetailsInactiveUser() throws Exception {
        doReturn(sampleUserDetailsResponse(HttpStatus.FORBIDDEN)).when(rabbitManager).sendForceReadRequest(any());

        mockMvc.perform(post("/administration/forgot_password")
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleResetPasswordRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successMessage").value(String.format("The new password has been sent to %s." +
                        " Please, change it in the User Control Panel as soon as possible.", email)));

    }

    @Test
    public void resetPasswordSuccess() throws Exception {
        doReturn(sampleUserDetailsResponse(HttpStatus.OK)).when(rabbitManager).sendForceReadRequest(any());
        doReturn(ManagementStatus.OK).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/forgot_password")
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleResetPasswordRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successMessage").value(String.format("The new password has been sent to %s." +
                        " Please, change it in the User Control Panel as soon as possible.", email)));

    }

    @Test
    public void resendVerificationEmailEntityUnreachableException() throws Exception {
        EntityUnreachableException entityUnreachableException = new EntityUnreachableException("AAM");
        doThrow(entityUnreachableException).when(rabbitManager).sendLoginRequest(any());

        mockMvc.perform(post("/administration/resend_verification_email")
                .header("Accept-Language", Locale.US.toString())
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleResendVerificationEmailRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.resendVerificationEmailError")
                        .value(entityUnreachableException.getMessage()));
    }

    @Test
    public void resendVerificationEmailCommunicationException() throws Exception {
        doThrow(sampleCommunicationException()).when(rabbitManager).sendLoginRequest(any());

        mockMvc.perform(post("/administration/resend_verification_email")
                .header("Accept-Language", Locale.US.toString())
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleResendVerificationEmailRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resendVerificationEmailError")
                        .value(sampleCommunicationException().getMessage()));
    }

    @Test
    public void resendVerificationEmailAccountNoSuchUser() throws Exception {
        doReturn(sampleUserDetailsResponse(HttpStatus.BAD_REQUEST)).when(rabbitManager).sendLoginRequest(any());

        mockMvc.perform(post("/administration/resend_verification_email")
                .header("Accept-Language", Locale.US.toString())
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleResendVerificationEmailRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resendVerificationEmailError")
                        .value("Username does not exist!"));
    }

    @Test
    public void resendVerificationEmailAccountWrongUserPassword() throws Exception {
        doReturn(sampleUserDetailsResponse(HttpStatus.UNAUTHORIZED)).when(rabbitManager).sendLoginRequest(any());

        mockMvc.perform(post("/administration/resend_verification_email")
                .header("Accept-Language", Locale.US.toString())
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleResendVerificationEmailRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resendVerificationEmailError")
                        .value("Wrong user password!"));
    }

    @Test
    public void resendVerificationEmailAccountWrongAdminPassword() throws Exception {
        UserDetailsResponse response = new UserDetailsResponse(HttpStatus.FORBIDDEN, null);
        doReturn(response).when(rabbitManager).sendLoginRequest(any());

        mockMvc.perform(post("/administration/resend_verification_email")
                .header("Accept-Language", Locale.US.toString())
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleResendVerificationEmailRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resendVerificationEmailError")
                        .value("Wrong admin password!"));
    }

    @Test
    public void resendVerificationEmailAccountAlreadyActive() throws Exception {
        doReturn(sampleUserDetailsResponse(HttpStatus.OK)).when(rabbitManager).sendLoginRequest(any());

        mockMvc.perform(post("/administration/resend_verification_email")
                .header("Accept-Language", Locale.US.toString())
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleResendVerificationEmailRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resendVerificationEmailError")
                        .value("Account already Active"));
    }

    @Test
    public void resendVerificationEmailSuccess() throws Exception {
        UserDetailsResponse inActive = new UserDetailsResponse(HttpStatus.FORBIDDEN,
                new UserDetails(
                        new Credentials(username, password),
                        email,
                        UserRole.SERVICE_OWNER,
                        AccountStatus.NEW,
                        new HashMap<>(),
                        new HashMap<>(),
                        true,
                        true
                ));

        doReturn(inActive).when(rabbitManager).sendLoginRequest(any());

        mockMvc.perform(post("/administration/resend_verification_email")
                .header("Accept-Language", Locale.US.toString())
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleResendVerificationEmailRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successMessage")
                        .value(messages.getMessage("message.verifyEmail", null, Locale.US)));
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

    private ResetPasswordRequest sampleResetPasswordRequest() {
        return new ResetPasswordRequest(username, email);
    }
    private ResendVerificationEmailRequest sampleResendVerificationEmailRequest() {
        return new ResendVerificationEmailRequest(username, password);
    }

    private enum TokenStatus {
        VALID, INVALID
    }
}