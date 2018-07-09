package eu.h2020.symbiote.administration.services.user;

import eu.h2020.symbiote.administration.exceptions.generic.GenericBadRequestException;
import eu.h2020.symbiote.administration.exceptions.generic.GenericHttpErrorException;
import eu.h2020.symbiote.administration.exceptions.rabbit.CommunicationException;
import eu.h2020.symbiote.administration.exceptions.rabbit.EntityUnreachableException;
import eu.h2020.symbiote.administration.exceptions.token.VerificationTokenExpired;
import eu.h2020.symbiote.administration.exceptions.token.VerificationTokenNotFoundException;
import eu.h2020.symbiote.administration.exceptions.validation.ServiceValidationException;
import eu.h2020.symbiote.administration.model.*;
import eu.h2020.symbiote.security.communication.payloads.UserDetails;
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
    UserDetails getUserInformationWithLogin(String username, String password)
            throws CommunicationException, GenericHttpErrorException, EntityUnreachableException;

    UserDetails getUserInformationWithLogin(Principal principal)
            throws CommunicationException, GenericHttpErrorException, EntityUnreachableException;


    UserDetails getUserInformationWithForce(String username)
            throws CommunicationException, GenericHttpErrorException, EntityUnreachableException;

    UserDetails getUserInformationWithForce(Principal principal)
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

    void acceptTerms(Principal principal) throws GenericHttpErrorException;

    void deleteUser(Principal principal) throws GenericHttpErrorException;

    void deleteClient(String clientId, Principal principal) throws GenericHttpErrorException;
}