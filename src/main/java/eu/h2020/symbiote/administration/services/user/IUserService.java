package eu.h2020.symbiote.administration.services.user;

import eu.h2020.symbiote.administration.model.CoreUser;
import eu.h2020.symbiote.administration.model.VerificationToken;

public interface IUserService {

//    CoreUser registerNewUserAccount(UserDto accountDto)
//            throws EmailExistsException;

//    CoreUser getUser(String verificationToken);
//
//    void saveRegisteredUser(CoreUser user);
//
    void createVerificationToken(CoreUser user, String token);

    VerificationToken getVerificationToken(String VerificationToken);
}