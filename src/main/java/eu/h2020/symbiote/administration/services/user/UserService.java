package eu.h2020.symbiote.administration.services.user;

import eu.h2020.symbiote.administration.exceptions.ServiceValidationException;
import eu.h2020.symbiote.administration.exceptions.generic.GenericBadRequestException;
import eu.h2020.symbiote.administration.exceptions.generic.GenericHttpErrorException;
import eu.h2020.symbiote.administration.exceptions.rabbit.CommunicationException;
import eu.h2020.symbiote.administration.exceptions.rabbit.EntityUnreachableException;
import eu.h2020.symbiote.administration.exceptions.token.VerificationTokenExpired;
import eu.h2020.symbiote.administration.exceptions.token.VerificationTokenNotFoundException;
import eu.h2020.symbiote.administration.model.*;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.WebRequest;

import java.security.Principal;

public interface UserService {

    // Token actions
    void createVerificationToken(CoreUser user, String token);

    VerificationToken verifyToken(String token)
            throws VerificationTokenNotFoundException, VerificationTokenExpired;

    void deleteVerificationToken(VerificationToken verificationToken);

    // Registration actions
    void validateUserRegistrationForm(CoreUser coreUser, BindingResult bindingResult) throws ServiceValidationException;

    void createUserAccount(CoreUser coreUser, WebRequest webRequest)
            throws CommunicationException, GenericHttpErrorException;

    void activateUserAccount(VerificationToken verificationToken)
            throws CommunicationException, GenericBadRequestException;

    void resendVerificationEmail(ResendVerificationEmailRequest request, BindingResult bindingResult, WebRequest webRequest)
            throws CommunicationException, GenericHttpErrorException;

    // Getting user info
    UserDetailsDTO getUserInformation(String username, String password)
            throws CommunicationException, GenericHttpErrorException, EntityUnreachableException;

    UserDetailsDTO getUserInformation(Principal principal)
            throws CommunicationException, GenericHttpErrorException, EntityUnreachableException;

    // User update actions
    void changeEmail(ChangeEmailRequest message, BindingResult bindingResult, Principal principal)
            throws GenericHttpErrorException;

    void changePermissions(ChangePermissions message, BindingResult bindingResult, Principal principal)
            throws GenericHttpErrorException;

    void changePassword(ChangePasswordRequest message, BindingResult bindingResult, Principal principal)
            throws GenericHttpErrorException;

    void resetPassword(ResetPasswordRequest request, BindingResult bindingResult, WebRequest webRequest)
            throws GenericHttpErrorException;

    void deleteUser(Principal principal) throws GenericHttpErrorException;

    void deleteClient(String clientId, Principal principal) throws GenericHttpErrorException;
}