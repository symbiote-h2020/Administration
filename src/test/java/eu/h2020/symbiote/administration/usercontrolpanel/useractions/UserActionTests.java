package eu.h2020.symbiote.administration.usercontrolpanel.useractions;

import eu.h2020.symbiote.administration.model.ChangeEmailRequest;
import eu.h2020.symbiote.administration.model.ChangePasswordRequest;
import eu.h2020.symbiote.administration.model.ChangePermissions;
import eu.h2020.symbiote.administration.usercontrolpanel.UserControlPanelBaseTestClass;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.communication.payloads.RevocationResponse;
import eu.h2020.symbiote.security.communication.payloads.UserDetailsResponse;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Test class for use in testing MVC and form validation.
 */
public class UserActionTests extends UserControlPanelBaseTestClass {

    @Test
    public void getControlPanelDenied() throws Exception {

        mockMvc.perform(get("/administration/user/cpanel"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/administration/user/login"));
    }

    @Test
    public void getControlPanelSuccess() throws Exception {

        mockMvc.perform(get("/administration/user/cpanel")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER))) )
            .andExpect(status().isOk());

    }

    @Test
    public void getUserInformationDenied() throws Exception {

        mockMvc.perform(get("/administration/user/information"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/administration/user/login"));
    }

    @Test
    public void getUserInformationAAMTimeout() throws Exception {
        doReturn(null).when(rabbitManager).sendLoginRequest(any());

        mockMvc.perform(get("/administration/user/information")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER))) )
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void getUserInformationNoSuchUserError() throws Exception {
        doReturn(sampleActiveUserDetailsResponse(HttpStatus.BAD_REQUEST)).when(rabbitManager).sendLoginRequest(any());

        mockMvc.perform(get("/administration/user/information")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER))) )
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Username does not exist!"));
    }

    @Test
    public void getUserInformationWrongUserPassword() throws Exception {
        doReturn(sampleActiveUserDetailsResponse(HttpStatus.UNAUTHORIZED)).when(rabbitManager).sendLoginRequest(any());

        mockMvc.perform(get("/administration/user/information")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER))) )
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Wrong user password!"));
    }

    @Test
    public void getUserInformationWrongAdminPassword() throws Exception {
        UserDetailsResponse response = new UserDetailsResponse(HttpStatus.FORBIDDEN, null);
        doReturn(response).when(rabbitManager).sendLoginRequest(any());

        mockMvc.perform(get("/administration/user/information")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER))) )
                .andExpect(status().isForbidden())
                .andExpect(content().string("Wrong admin password!"));
    }

    // Todo: Rethink about this test
    @Test
    public void getUserInformationInactiveUser() throws Exception {
        doReturn(sampleActiveUserDetailsResponse(HttpStatus.FORBIDDEN)).when(rabbitManager).sendLoginRequest(any());

        mockMvc.perform(get("/administration/user/information")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER))) )
                .andExpect(status().isOk());
    }

    @Test
    public void getUserInformationSuccess() throws Exception {
        doReturn(sampleActiveUserDetailsResponse(HttpStatus.OK)).when(rabbitManager).sendLoginRequest(any());

        mockMvc.perform(get("/administration/user/information")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER))) )
                .andExpect(status().isOk());
    }

    @Test
    public void changePasswordTimeout() throws Exception {
        doReturn(null).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/user/change_password")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleChangePasswordRequest())))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void changePasswordBadRequest() throws Exception {
        doThrow(sampleCommunicationException()).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/user/change_password")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleChangePasswordRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void changePasswordInvalidPasswords() throws Exception {
        ChangePasswordRequest invalidPasswords = new ChangePasswordRequest("wrongPassword","a", "b");

        mockMvc.perform(post("/administration/user/change_password")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(invalidPasswords)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.changePasswordError")
                        .value("Invalid Arguments"))
                .andExpect(jsonPath("$.error_oldPassword")
                        .value("Your old password is not correct"))
                .andExpect(jsonPath("$.error_newPassword")
                        .value("Enter a valid password"))
                .andExpect(jsonPath("$.error_newPasswordRetyped")
                        .value("Enter a valid password"));
    }

    @Test
    public void changePasswordDifferentPasswords() throws Exception {
        ChangePasswordRequest differentPasswords = new ChangePasswordRequest(password, "newPassword", "newPass");

        mockMvc.perform(post("/administration/user/change_password")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(differentPasswords)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.changePasswordError")
                        .value("Invalid Arguments"))
                .andExpect(jsonPath("$.error_newPasswordRetyped")
                        .value("The provided passwords do not match"));
    }

    @Test
    public void changePasswordSuccess() throws Exception {
        doReturn(ManagementStatus.OK).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/user/change_password")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleChangePasswordRequest())))
                .andExpect(status().isOk());
    }

    @Test
    public void changeEmailTimeout() throws Exception {
        doReturn(null).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/user/change_email")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleChangeEmailRequest())))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void changeEmailBadRequest() throws Exception {
        doThrow(sampleCommunicationException()).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/user/change_email")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleChangeEmailRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void changeEmailInvalidEmails() throws Exception {
        ChangeEmailRequest invalidEmails = new ChangeEmailRequest("a", "b");

        mockMvc.perform(post("/administration/user/change_email")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(invalidEmails)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.changeEmailError")
                        .value("Invalid Arguments"))
                .andExpect(jsonPath("$.error_newEmail")
                        .value("Enter a valid email"))
                .andExpect(jsonPath("$.error_newEmailRetyped")
                        .value("Enter a valid email"));
    }

    @Test
    public void changeEmailDifferentEmails() throws Exception {
        ChangeEmailRequest differentEmails = new ChangeEmailRequest("a@a.com", "b@a.com");

        mockMvc.perform(post("/administration/user/change_email")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(differentEmails)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.changeEmailError")
                        .value("Invalid Arguments"))
                .andExpect(jsonPath("$.error_newEmailRetyped")
                        .value("The provided emails do not match"));
    }

    @Test
    public void changeEmailSuccess() throws Exception {
        doReturn(ManagementStatus.OK).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/user/change_email")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleChangeEmailRequest())))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteClientTimeout() throws Exception {
        doReturn(null).when(rabbitManager).sendRevocationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_client")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("clientIdToDelete", clientId1))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void deleteClientException() throws Exception {
        doThrow(sampleCommunicationException()).when(rabbitManager).sendRevocationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_client")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("clientIdToDelete", clientId1))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void deleteClientIdError1() throws Exception {
        deleteClientIdError(new RevocationResponse(false, HttpStatus.BAD_REQUEST));
    }

    @Test
    public void deleteClientIdError2() throws Exception {
        deleteClientIdError(new RevocationResponse(false, HttpStatus.OK));
    }

    @Test
    public void deleteClientIdError3() throws Exception {
        deleteClientIdError(new RevocationResponse(true, HttpStatus.BAD_REQUEST));
    }

    @Test
    public void deleteClientIdSuccess() throws Exception {

        doReturn(new RevocationResponse(true, HttpStatus.OK)).when(rabbitManager).sendRevocationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_client")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("clientIdToDelete", clientId1))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteUserTimeout() throws Exception {
        doReturn(null).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/user/delete_user")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void deleteUserException() throws Exception {
        doThrow(sampleCommunicationException()).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/user/delete_user")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void deleteUserError() throws Exception {
        doReturn(ManagementStatus.ERROR).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/user/delete_user")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void deleteUserSuccess() throws Exception {
        doReturn(ManagementStatus.OK).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/user/delete_user")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isOk());
    }

    @Test
    public void changePermissionsTimeout() throws Exception {
        doReturn(null).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/user/change_permissions")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(
                        new ChangePermissions(false))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.changePermissionsError")
                        .value("Authorization Manager is unreachable!"));
    }

    @Test
    public void changePermissionsException() throws Exception {
        doThrow(sampleCommunicationException()).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/user/change_permissions")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(
                        new ChangePermissions(false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.changePermissionsError")
                        .value(sampleCommunicationException().getMessage()));
    }

    @Test
    public void changePermissionsError() throws Exception {
        doReturn(ManagementStatus.ERROR).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/user/change_permissions")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(
                        new ChangePermissions(false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.changePermissionsError")
                        .value("The Authorization Manager responded with ERROR"));

    }

    @Test
    public void changePermissionsSuccess() throws Exception {
        doReturn(ManagementStatus.OK).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/user/change_permissions")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(
                        new ChangePermissions(false))))
                .andExpect(status().isOk());
    }

    @Test
    public void acceptTermsTimeout() throws Exception {
        doReturn(null).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/user/accept_terms")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.acceptTermsError")
                        .value("Authorization Manager is unreachable!"));
    }

    @Test
    public void acceptTermsException() throws Exception {
        doThrow(sampleCommunicationException()).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/user/accept_terms")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.acceptTermsError")
                        .value(sampleCommunicationException().getMessage()));
    }

    @Test
    public void acceptTermsError() throws Exception {
        doReturn(ManagementStatus.ERROR).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/user/accept_terms")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.acceptTermsError")
                        .value("The Authorization Manager responded with ERROR"));

    }

    @Test
    public void acceptTermsSuccess() throws Exception {
        doReturn(ManagementStatus.OK).when(rabbitManager).sendUserManagementRequest(any());

        mockMvc.perform(post("/administration/user/accept_terms")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isOk());
    }


    private  void deleteClientIdError(RevocationResponse response) throws Exception {
        doReturn(response).when(rabbitManager).sendRevocationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_client")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("clientIdToDelete", clientId1))
                .andExpect(status().isBadRequest());
    }
}