package eu.h2020.symbiote.administration.services.user;

import eu.h2020.symbiote.administration.exceptions.generic.GenericErrorException;
import eu.h2020.symbiote.administration.exceptions.rabbit.CommunicationException;
import eu.h2020.symbiote.administration.exceptions.token.VerificationTokenExpired;
import eu.h2020.symbiote.administration.exceptions.token.VerificationTokenNotFoundException;
import eu.h2020.symbiote.administration.model.CoreUser;
import eu.h2020.symbiote.administration.model.VerificationToken;

public interface UserService {

    void createVerificationToken(CoreUser user, String token);

    void saveUser(CoreUser user);

    VerificationToken verifyToken(String token)
            throws VerificationTokenNotFoundException, VerificationTokenExpired;

    void deleteVerificationToken(VerificationToken verificationToken);

    void activateUserAccount(VerificationToken verificationToken)
            throws CommunicationException, GenericErrorException;
}