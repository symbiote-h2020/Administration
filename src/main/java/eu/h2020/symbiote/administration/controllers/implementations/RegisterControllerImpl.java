package eu.h2020.symbiote.administration.controllers.implementations;

import eu.h2020.symbiote.administration.controllers.interfaces.RegisterController;
import eu.h2020.symbiote.administration.exceptions.ServiceValidationException;
import eu.h2020.symbiote.administration.exceptions.generic.GenericBadRequestException;
import eu.h2020.symbiote.administration.exceptions.generic.GenericHttpErrorException;
import eu.h2020.symbiote.administration.exceptions.generic.GenericInternalServerErrorException;
import eu.h2020.symbiote.administration.exceptions.rabbit.CommunicationException;
import eu.h2020.symbiote.administration.model.CoreUser;
import eu.h2020.symbiote.administration.model.ResendVerificationEmailRequest;
import eu.h2020.symbiote.administration.model.ResetPasswordRequest;
import eu.h2020.symbiote.administration.model.VerificationToken;
import eu.h2020.symbiote.administration.model.mappers.UserRoleValueTextMapping;
import eu.h2020.symbiote.administration.services.user.UserService;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;

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

    @Value("${aam.deployment.owner.username}")
    private String aaMOwnerUsername;

    @Value("${aam.deployment.owner.password}")
    private String aaMOwnerPassword;

    @Value("${symbiote.core.administration.email.verification}")
    private Boolean emailVerificationEnabled;

    private UserService userService;
    private MessageSource messages;

    @Autowired
    public RegisterControllerImpl(UserService userService,
                                  MessageSource messages,
                                  @Value("${aam.deployment.owner.username}") String aaMOwnerUsername,
                                  @Value("${aam.deployment.owner.password}") String aaMOwnerPassword,
                                  @Value("${symbiote.core.administration.email.verification}") Boolean emailVerificationEnabled) {

        Assert.notNull(userService,"UserService can not be null!");
        this.userService = userService;

        Assert.notNull(messages,"MessageSource can not be null!");
        this.messages = messages;

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
            throws CommunicationException, GenericHttpErrorException, ServiceValidationException {

        log.debug("POST request on /administration/register");
        log.debug("CoreUser = " + ReflectionToStringBuilder.toString(coreUser));

        coreUser.clearSensitiveData();
        userService.validateUserRegistrationForm(coreUser, bindingResult);
        userService.createUserAccount(coreUser, webRequest);

        Map<String, Object> response = new HashMap<>();
        response.put("validationErrors", new HashMap<>());
        response.put("successMessage", emailVerificationEnabled ?
                messages.getMessage("message.verifyEmail", null, webRequest.getLocale()) :
                messages.getMessage("message.successfulRegistrationNoEmail", null, webRequest.getLocale()));
        return response;
    }

    @Override
    public String confirmRegistration(String token) throws CommunicationException, GenericBadRequestException {
        log.trace("Get on /administration/registrationConfirm with token = " + token);

        // Get and verify that the token is not expired
        VerificationToken verificationToken = userService.verifyToken(token);

        userService.activateUserAccount(verificationToken);
        userService.deleteVerificationToken(verificationToken);

        return "email_verification_success";
    }

    @Override
    public Map<String, Object> forgotPassword(@Valid @RequestBody ResetPasswordRequest request,
                                              BindingResult bindingResult,
                                              WebRequest webRequest)
            throws GenericHttpErrorException {
        log.debug("POST request on /administration/generic/forgot_password for username = "
                + request.getUsername() + " and email = " + request.getEmail());
        userService.resetPassword(request, bindingResult, webRequest);

        Map<String, Object> response = new HashMap<>();
        response.put("successMessage", String.format("The new password has been sent to %s." +
                " Please, change it in the User Control Panel as soon as possible.", request.getEmail()));
        return response;
    }

    @Override
    public Map<String, Object> resendVerificationEmail(@Valid @RequestBody ResendVerificationEmailRequest request,
                                                       BindingResult bindingResult,
                                                       WebRequest webRequest)
            throws CommunicationException, GenericHttpErrorException {
        log.debug("POST request on /administration/resend_verification_email for username = "
                + request.getUsername());
        userService.resendVerificationEmail(request, bindingResult, webRequest);

        Map<String, Object> response = new HashMap<>();
        response.put("successMessage", messages.getMessage("message.verifyEmail", null, webRequest.getLocale()));
        return response;
    }
}