package eu.h2020.symbiote.administration.services.user;

import eu.h2020.symbiote.administration.application.events.OnRegistrationCompleteEvent;
import eu.h2020.symbiote.administration.application.listeners.RegistrationListener;
import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import eu.h2020.symbiote.administration.exceptions.ValidationException;
import eu.h2020.symbiote.administration.exceptions.generic.GenericBadRequestException;
import eu.h2020.symbiote.administration.exceptions.generic.GenericInternalServerErrorException;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.context.request.WebRequest;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {
    private static Log log = LogFactory.getLog(RegistrationListener.class);

    private RabbitManager rabbitManager;
    private VerificationTokenRepository tokenRepository;
    private UserRepository userRepository;
    private ApplicationEventPublisher eventPublisher;

    private String aaMOwnerUsername;
    private String aaMOwnerPassword;
    private Integer tokenExpirationTimeInHours;
    private Boolean emailVerificationEnabled;

    @Autowired
    public UserServiceImpl(RabbitManager rabbitManager,
                           VerificationTokenRepository tokenRepository,
                           UserRepository userRepository,
                           ApplicationEventPublisher eventPublisher,
                           @Value("${aam.deployment.owner.username}") String aaMOwnerUsername,
                           @Value("${aam.deployment.owner.password}") String aaMOwnerPassword,
                           @Value("${verificationToken.expirationTime.hours}") Integer tokenExpirationTimeInHours,
                           @Value("${symbiote.core.administration.email.verification}") Boolean emailVerificationEnabled) {
        this.rabbitManager = rabbitManager;
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;

        Assert.notNull(tokenExpirationTimeInHours,"tokenExpirationTimeInHours can not be null!");
        this.tokenExpirationTimeInHours = tokenExpirationTimeInHours;

        Assert.notNull(aaMOwnerUsername,"aaMOwnerUsername can not be null!");
        this.aaMOwnerUsername = aaMOwnerUsername;

        Assert.notNull(aaMOwnerPassword,"aaMOwnerPassword can not be null!");
        this.aaMOwnerPassword = aaMOwnerPassword;

        Assert.notNull(emailVerificationEnabled,"emailVerificationEnabled can not be null!");
        this.emailVerificationEnabled = emailVerificationEnabled;
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

        if (currentDate > tokenExpirationDate) {
            tokenRepository.delete(verificationToken);
            throw new VerificationTokenExpired(token);
        }

        return verificationToken;
    }

    @Override
    public void deleteVerificationToken(VerificationToken verificationToken) {
        tokenRepository.delete(verificationToken);
    }

    @Override
    public void validateUserRegistrationForm(CoreUser coreUser, BindingResult bindingResult) throws ValidationException {
        boolean invalidUserRole = (coreUser.getRole() == UserRole.NULL);

        if (bindingResult.hasErrors() || invalidUserRole) {
            List<FieldError> errors = bindingResult.getFieldErrors();
            Map<String, Object> response = new HashMap<>();
            Map<String, String> errorMessages = new HashMap<>();

            for (FieldError fieldError : errors) {
                String errorMessage = fieldError.getDefaultMessage();
                String errorField = fieldError.getField();
                errorMessages.put(errorField, errorMessage);
                log.debug(errorField + ": " + errorMessage);

            }

            if (invalidUserRole)
                errorMessages.put("role", "Invalid User Role");

            response.put("validationErrors", errorMessages);

            throw new ValidationException("Invalid Arguments", errorMessages);
        }
    }

    @Override
    public void createUserAccount(CoreUser coreUser, WebRequest webRequest)
            throws CommunicationException, GenericBadRequestException, GenericInternalServerErrorException {

        // Construct the UserManagementRequest
        UserManagementRequest userRegistrationRequest = new UserManagementRequest(
                new Credentials(aaMOwnerUsername, aaMOwnerPassword),
                new Credentials(coreUser.getValidUsername(), coreUser.getValidPassword()),
                new UserDetails(
                        new Credentials(coreUser.getValidUsername(), coreUser.getValidPassword()),
                        coreUser.getRecoveryMail(),
                        UserRole.SERVICE_OWNER,
                        emailVerificationEnabled ? AccountStatus.NEW : AccountStatus.ACTIVE,
                        new HashMap<>(),
                        new HashMap<>(),
                        coreUser.isConditionsAccepted(),
                        coreUser.isAnalyticsAndResearchConsent()
                ),
                OperationType.CREATE
        );

        ManagementStatus managementStatus = rabbitManager.sendUserManagementRequest(userRegistrationRequest);

        if (managementStatus == null) {
            throw new EntityUnreachable("AAM");

        } else if(managementStatus == ManagementStatus.OK ) {
            if (emailVerificationEnabled) {
                try {
                    String appUrl = webRequest.getContextPath();
                    eventPublisher.publishEvent(new OnRegistrationCompleteEvent
                            (coreUser, webRequest.getLocale(), appUrl, coreUser.getRecoveryMail()));
                } catch (Exception me) {
                    throw new GenericInternalServerErrorException("Could not send verification email");
                }
            }

            coreUser.clearSensitiveData();
            saveUser(coreUser);
        } else if (managementStatus == ManagementStatus.USERNAME_EXISTS) {
            throw new GenericBadRequestException("Username exists!");
        } else
            throw new GenericBadRequestException(managementStatus.toString());
    }

    @Override
    public void activateUserAccount(VerificationToken verificationToken)
            throws CommunicationException, GenericBadRequestException, EntityUnreachable {
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
                        coreUser.isConditionsAccepted(),
                        coreUser.isAnalyticsAndResearchConsent()
                ),
                OperationType.FORCE_UPDATE
        );

        ManagementStatus managementStatus = rabbitManager.sendUserManagementRequest(userRegistrationRequest);

        if (managementStatus == null)
            throw new EntityUnreachable("AAM");
        else if (managementStatus != ManagementStatus.OK)
            throw new GenericBadRequestException(managementStatus.toString());
    }

    private Long convertDateToHours(Date date) {
        return TimeUnit.MILLISECONDS.toHours(date.getTime());
    }
}
