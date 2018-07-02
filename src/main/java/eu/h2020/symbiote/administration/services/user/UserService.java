package eu.h2020.symbiote.administration.services.user;

import eu.h2020.symbiote.administration.exceptions.ValidationException;
import eu.h2020.symbiote.administration.exceptions.generic.GenericBadRequestException;
import eu.h2020.symbiote.administration.exceptions.generic.GenericInternalServerErrorException;
import eu.h2020.symbiote.administration.exceptions.rabbit.CommunicationException;
import eu.h2020.symbiote.administration.exceptions.token.VerificationTokenExpired;
import eu.h2020.symbiote.administration.exceptions.token.VerificationTokenNotFoundException;
import eu.h2020.symbiote.administration.model.CoreUser;
import eu.h2020.symbiote.administration.model.VerificationToken;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.WebRequest;

public interface UserService {

    void createVerificationToken(CoreUser user, String token);

    void saveUser(CoreUser user);

    VerificationToken verifyToken(String token)
            throws VerificationTokenNotFoundException, VerificationTokenExpired;

    void deleteVerificationToken(VerificationToken verificationToken);

    void validateUserRegistrationForm(CoreUser coreUser, BindingResult bindingResult) throws ValidationException;

    void createUserAccount(CoreUser coreUser, WebRequest webRequest)
            throws CommunicationException, GenericBadRequestException, GenericInternalServerErrorException;

    void activateUserAccount(VerificationToken verificationToken)
            throws CommunicationException, GenericBadRequestException;
}