package eu.h2020.symbiote.administration.controllers;


import eu.h2020.symbiote.administration.application.events.OnRegistrationCompleteEvent;
import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.administration.model.CoreUser;
import eu.h2020.symbiote.administration.model.mappers.UserRoleValueTextMapping;
import eu.h2020.symbiote.administration.repository.UserRepository;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import eu.h2020.symbiote.security.commons.enums.OperationType;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.communication.payloads.Credentials;
import eu.h2020.symbiote.security.communication.payloads.UserDetails;
import eu.h2020.symbiote.security.communication.payloads.UserManagementRequest;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.context.request.WebRequest;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Spring controller, handles user registration views and form validation.
 *
 * @author Tilemachos Pechlivanoglou (ICOM)
 */
@Controller
@CrossOrigin
public class RegisterController {

    private static Log log = LogFactory.getLog(RegisterController.class);

	@Value("${aam.deployment.owner.username}")
	private String aaMOwnerUsername;

	@Value("${aam.deployment.owner.password}")
	private String aaMOwnerPassword;

    private RabbitManager rabbitManager;
    private UserRepository userRepository;
    private ApplicationEventPublisher eventPublisher;



    @Autowired
    public RegisterController(RabbitManager rabbitManager, UserRepository userRepository, ApplicationEventPublisher eventPublisher) {
        Assert.notNull(rabbitManager,"RabbitManager can not be null!");
        this.rabbitManager = rabbitManager;

        Assert.notNull(userRepository,"UserRepository can not be null!");
        this.userRepository = userRepository;

        Assert.notNull(eventPublisher,"EventPublisher can not be null!");
        this.eventPublisher = eventPublisher;
    }

    // The CoreUser argument is needed so that the template can associate form attributes with a CoreUser
	@GetMapping("/administration/register")
	public String coreUserRegisterForm(Model model) {
		log.debug("GET request on /administration/register");

		model.addAttribute("allRoles", UserRoleValueTextMapping.getList());
	    return "index";
	}

    @GetMapping("/administration/register/roles")
    public ResponseEntity<List<UserRoleValueTextMapping>> getUserRoles() {
        log.debug("GET request on /administration/register/roles");

        return new ResponseEntity<>(UserRoleValueTextMapping.getList(), new HttpHeaders(), HttpStatus.OK);
    }

    @PostMapping("/administration/register")
    public ResponseEntity<Map<String, Object>> coreUserRegister(@Valid CoreUser coreUser,
                                                                BindingResult bindingResult,
                                                                WebRequest webRequest) {

        log.debug("POST request on /administration/register");
        log.debug("CoreUser = " + ReflectionToStringBuilder.toString(coreUser));
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

            return new ResponseEntity<>(response, new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }

        log.debug(ReflectionToStringBuilder.toString(coreUser));

        // Construct the UserManagementRequest
        UserManagementRequest userRegistrationRequest = new UserManagementRequest(
                new Credentials(aaMOwnerUsername, aaMOwnerPassword),
                new Credentials(coreUser.getValidUsername(), coreUser.getValidPassword()),
                new UserDetails(
                        new Credentials(coreUser.getValidUsername(), coreUser.getValidPassword()),
                        coreUser.getRecoveryMail(),
                        UserRole.SERVICE_OWNER,
                        new HashMap<>(),
                        new HashMap<>()
                ),
                OperationType.CREATE
        );

        Map<String, Object> response = new HashMap<>();
        response.put("validationErrors", new HashMap<>());

        try {
            ManagementStatus managementStatus = rabbitManager.sendUserManagementRequest(userRegistrationRequest);

            if (managementStatus == null) {
                response.put("errorMessage","Authorization Manager is unreachable!");
                return new ResponseEntity<>(response, new HttpHeaders(),
                        HttpStatus.INTERNAL_SERVER_ERROR);

            } else if(managementStatus == ManagementStatus.OK ) {
                try {
                    String appUrl = webRequest.getContextPath();
                    eventPublisher.publishEvent(new OnRegistrationCompleteEvent
                            (coreUser, webRequest.getLocale(), appUrl, coreUser.getRecoveryMail()));
                } catch (Exception me) {
                    response.put("errorMessage","Could not send verification email");
                    return new ResponseEntity<>(response, new HttpHeaders(),
                            HttpStatus.INTERNAL_SERVER_ERROR);
                }
                coreUser.clearSensitiveData();
                userRepository.save(coreUser);
                return new ResponseEntity<>(response, new HttpHeaders(),
                        HttpStatus.CREATED);
            } else if (managementStatus == ManagementStatus.USERNAME_EXISTS) {
                response.put("errorMessage","Username exist!");
                return new ResponseEntity<>(response, new HttpHeaders(),
                        HttpStatus.BAD_REQUEST);
            } else {
                response.put("errorMessage","Authorization Manager responded with ERROR!");
                return new ResponseEntity<>(response, new HttpHeaders(),
                        HttpStatus.BAD_REQUEST);
            }
        } catch (CommunicationException e) {
            response.put("errorMessage",e.getMessage());
            return  new ResponseEntity<>(response, new HttpHeaders(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Used for testing
     */
    public void setRabbitManager(RabbitManager rabbitManager){
        this.rabbitManager = rabbitManager;
    }
}