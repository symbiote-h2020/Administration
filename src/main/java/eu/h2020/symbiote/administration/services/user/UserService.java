package eu.h2020.symbiote.administration.services.user;

import eu.h2020.symbiote.administration.application.listeners.RegistrationListener;
import eu.h2020.symbiote.administration.model.CoreUser;
import eu.h2020.symbiote.administration.model.VerificationToken;
import eu.h2020.symbiote.administration.repository.VerificationTokenRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService implements IUserService {
    private static Log log = LogFactory.getLog(RegistrationListener.class);

    private VerificationTokenRepository tokenRepository;

    @Autowired
    public UserService(VerificationTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Override
    public void createVerificationToken(CoreUser user, String tokenString) {
        log.debug("Got tokenString " + tokenString + " for user = " + user);
        VerificationToken token = new VerificationToken(tokenString, user);
        tokenRepository.save(token);
    }

    @Override
    public VerificationToken getVerificationToken(String verificationToken) {
        return tokenRepository.findOne(verificationToken);
    }
}
