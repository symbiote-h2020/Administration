package eu.h2020.symbiote.administration.services.user;

import eu.h2020.symbiote.administration.application.listeners.RegistrationListener;
import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import eu.h2020.symbiote.administration.exceptions.generic.GenericErrorException;
import eu.h2020.symbiote.administration.exceptions.rabbit.CommunicationException;
import eu.h2020.symbiote.administration.exceptions.rabbit.EntityUnreachable;
import eu.h2020.symbiote.administration.exceptions.token.VerificationTokenExpired;
import eu.h2020.symbiote.administration.exceptions.token.VerificationTokenNotFoundException;
import eu.h2020.symbiote.administration.model.CoreUser;
import eu.h2020.symbiote.administration.model.VerificationToken;
import eu.h2020.symbiote.administration.repository.UserRepository;
import eu.h2020.symbiote.administration.repository.VerificationTokenRepository;
import eu.h2020.symbiote.security.commons.enums.AccountStatus;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import eu.h2020.symbiote.security.commons.enums.OperationType;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.communication.payloads.Credentials;
import eu.h2020.symbiote.security.communication.payloads.UserDetails;
import eu.h2020.symbiote.security.communication.payloads.UserManagementRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {
    private static Log log = LogFactory.getLog(RegistrationListener.class);

    private final RabbitManager rabbitManager;
    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;

    @Value("${aam.deployment.owner.username}")
    private String aaMOwnerUsername;

    @Value("${aam.deployment.owner.password}")
    private String aaMOwnerPassword;
    private Integer tokenExpirationTimeInHours;

    @Autowired
    public UserServiceImpl(RabbitManager rabbitManager,
                           VerificationTokenRepository tokenRepository,
                           UserRepository userRepository,
                           @Value("${aam.deployment.owner.username}") String aaMOwnerUsername,
                           @Value("${aam.deployment.owner.password}") String aaMOwnerPassword,
                           @Value("${verificationToken.expirationTime.hours}") Integer tokenExpirationTimeInHours) {
        this.rabbitManager = rabbitManager;
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;

        Assert.notNull(tokenExpirationTimeInHours,"tokenExpirationTimeInHours can not be null!");
        this.tokenExpirationTimeInHours = tokenExpirationTimeInHours;

        Assert.notNull(aaMOwnerUsername,"aaMOwnerUsername can not be null!");
        this.aaMOwnerUsername = aaMOwnerUsername;

        Assert.notNull(aaMOwnerPassword,"aaMOwnerPassword can not be null!");
        this.aaMOwnerPassword = aaMOwnerPassword;
    }

    @Override
    public void createVerificationToken(CoreUser user, String tokenString) {
        log.debug("Got tokenString " + tokenString + " for user = " + user);
        VerificationToken token = new VerificationToken(tokenString, user, tokenExpirationTimeInHours);
        tokenRepository.save(token);
    }

    @Override
    public void saveUser(CoreUser user) {
        userRepository.save(user);
    }

    @Override
    public VerificationToken verifyToken(String token)
            throws VerificationTokenNotFoundException, VerificationTokenExpired {

        VerificationToken verificationToken = tokenRepository.findOne(token);

        if (verificationToken == null)
            throw new VerificationTokenNotFoundException(token);

        // Get dates in days
        Long currentDate = convertDateToHours(new Date());
        Long tokenExpirationDate = convertDateToHours(verificationToken.getExpirationDate());

        if (currentDate > tokenExpirationDate)
            throw new VerificationTokenExpired(token);

        return verificationToken;
    }

    @Override
    public void deleteVerificationToken(VerificationToken verificationToken) {
        tokenRepository.delete(verificationToken);
    }

    @Override
    public void activateUserAccount(VerificationToken verificationToken) throws CommunicationException, GenericErrorException {
        CoreUser coreUser = verificationToken.getUser();

        // Todo: change the consents below to read them from token
        // Construct the UserManagementRequest
        UserManagementRequest userRegistrationRequest = new UserManagementRequest(
                new Credentials(aaMOwnerUsername, aaMOwnerPassword),
                new Credentials(coreUser.getValidUsername(), coreUser.getValidPassword()),
                new UserDetails(
                        new Credentials(coreUser.getValidUsername(), coreUser.getValidPassword()),
                        coreUser.getRecoveryMail(),
                        coreUser.getRole(),
                        AccountStatus.ACTIVE,
                        new HashMap<>(),
                        new HashMap<>(),
                        true,
                        false
                ),
                OperationType.FORCE_UPDATE
        );

        ManagementStatus managementStatus = rabbitManager.sendUserManagementRequest(userRegistrationRequest);

        if (managementStatus == null)
            throw new EntityUnreachable("AAM");
        else if (managementStatus != ManagementStatus.OK)
            throw new GenericErrorException(managementStatus.toString());
    }

    private Long convertDateToHours(Date date) {
        return TimeUnit.MILLISECONDS.toHours(date.getTime());
    }
}
