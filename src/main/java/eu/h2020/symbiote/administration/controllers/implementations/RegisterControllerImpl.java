package eu.h2020.symbiote.administration.controllers.implementations;


import eu.h2020.symbiote.administration.controllers.interfaces.RegisterController;
import eu.h2020.symbiote.administration.exceptions.ValidationException;
import eu.h2020.symbiote.administration.exceptions.generic.GenericBadRequestException;
import eu.h2020.symbiote.administration.exceptions.generic.GenericInternalServerErrorException;
import eu.h2020.symbiote.administration.exceptions.rabbit.CommunicationException;
import eu.h2020.symbiote.administration.model.CoreUser;
import eu.h2020.symbiote.administration.model.VerificationToken;
import eu.h2020.symbiote.administration.model.mappers.UserRoleValueTextMapping;
import eu.h2020.symbiote.administration.services.user.UserService;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.context.request.WebRequest;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Spring controller, handles user registration views and form validation.
 *
 * @author Vasilis Glykantzis (ICOM)
 */
@Controller
@CrossOrigin
public class RegisterControllerImpl implements RegisterController {

    private static Log log = LogFactory.getLog(RegisterControllerImpl.class);
    public static final String USER_ACCOUNT_ACTIVATED_MESSAGE = "Your account has been activated";
    public static final String SUCCESSFUL_REGISTRATION_MESSAGE = "Please, login with your new credentials";
    public static final String VERIFY_EMAIL = "We have sent you an email. Please, click on the link to verify your " +
            "account and then login with your new credentials";

    @Value("${aam.deployment.owner.username}")
    private String aaMOwnerUsername;

    @Value("${aam.deployment.owner.password}")
    private String aaMOwnerPassword;

    @Value("${symbiote.core.administration.email.verification}")
    private Boolean emailVerificationEnabled;

    private UserService userService;

    @Autowired
    public RegisterControllerImpl(UserService userService,
                                  @Value("${aam.deployment.owner.username}") String aaMOwnerUsername,
                                  @Value("${aam.deployment.owner.password}") String aaMOwnerPassword,
                                  @Value("${symbiote.core.administration.email.verification}") Boolean emailVerificationEnabled) {

        Assert.notNull(userService,"UserService can not be null!");
        this.userService = userService;

        Assert.notNull(aaMOwnerUsername,"aaMOwnerUsername can not be null!");
        this.aaMOwnerUsername = aaMOwnerUsername;

        Assert.notNull(aaMOwnerPassword,"aaMOwnerPassword can not be null!");
        this.aaMOwnerPassword = aaMOwnerPassword;

        Assert.notNull(emailVerificationEnabled,"emailVerificationEnabled can not be null!");
        this.emailVerificationEnabled = emailVerificationEnabled;
    }

    // The CoreUser argument is needed so that the template can associate form attributes with a CoreUser
    @Override
    public String coreUserRegisterForm(Model model) {
        log.debug("GET request on /administration/register");

        model.addAttribute("allRoles", UserRoleValueTextMapping.getList());
        return "index";
    }

    @Override
    public ResponseEntity<List<UserRoleValueTextMapping>> getUserRoles() {
        log.debug("GET request on /administration/register/roles");

        return new ResponseEntity<>(UserRoleValueTextMapping.getList(), new HttpHeaders(), HttpStatus.OK);
    }

    @Override
    public Map<String, Object> coreUserRegister(@Valid CoreUser coreUser,
                                                BindingResult bindingResult,
                                                WebRequest webRequest)
            throws CommunicationException, GenericBadRequestException, GenericInternalServerErrorException, ValidationException {

        log.debug("POST request on /administration/register");
        log.debug("CoreUser = " + ReflectionToStringBuilder.toString(coreUser));

        userService.validateUserRegistrationForm(coreUser, bindingResult);
        userService.createUserAccount(coreUser, webRequest);

        Map<String, Object> response = new HashMap<>();
        response.put("validationErrors", new HashMap<>());
        response.put("successMessage", emailVerificationEnabled ? VERIFY_EMAIL : SUCCESSFUL_REGISTRATION_MESSAGE);
        return response;
    }

    @Override
    public String confirmRegistration(String token) throws CommunicationException, GenericBadRequestException {
        log.trace("Get on /administration/registrationConfirm with token = " + token);

        // Get and verify that the token is not expired
        VerificationToken verificationToken = userService.verifyToken(token);

        userService.activateUserAccount(verificationToken);
        userService.deleteVerificationToken(verificationToken);

        return USER_ACCOUNT_ACTIVATED_MESSAGE;
    }
}