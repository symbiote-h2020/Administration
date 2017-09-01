package eu.h2020.symbiote.administration.controllers;


import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.administration.model.UserRoleValueTextMapping;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.validation.BindingResult;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;

import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import eu.h2020.symbiote.administration.model.CoreUser;
import eu.h2020.symbiote.security.communication.payloads.Credentials;
import eu.h2020.symbiote.security.communication.payloads.UserManagementRequest;
import eu.h2020.symbiote.security.communication.payloads.UserDetails;
import eu.h2020.symbiote.security.commons.enums.OperationType;
import eu.h2020.symbiote.security.commons.enums.UserRole;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Spring controller, handles user registration views and form validation.
 *
 * @author Tilemachos Pechlivanoglou (ICOM)
 */
@Controller
public class Register {

    private static Log log = LogFactory.getLog(Register.class);

	@Value("${aam.deployment.owner.username}")
	private String aaMOwnerUsername;

	@Value("${aam.deployment.owner.password}")
	private String aaMOwnerPassword;

    private RabbitManager rabbitManager;


    @Autowired
    public Register(RabbitManager rabbitManager) {
        Assert.notNull(rabbitManager,"RabbitManager can not be null!");
        this.rabbitManager = rabbitManager;
    }

    // The CoreUser argument is needed so that the template can associate form attributes with a CoreUser
	@GetMapping("/register")
	public String coreUserRegisterForm(CoreUser coreUser, Model model) {
		log.debug("GET request on /register");

		model.addAttribute("allRoles", UserRoleValueTextMapping.getList());
	    return "register";
	}

	@PostMapping("/register")
	public String coreUserRegister(@Valid CoreUser coreUser, BindingResult bindingResult, Model model) {

        log.debug("POST request on /register");

		if (bindingResult.hasErrors()) {
            model.addAllAttributes(getAllAttributes(coreUser, bindingResult));
			return "register";
		}

		log.debug(ReflectionToStringBuilder.toString(coreUser));

		// Construct the UserManagementRequest
        // Todo: Change the federatedId in R4

		UserManagementRequest userRegistrationRequest = new UserManagementRequest(
				new Credentials(aaMOwnerUsername, aaMOwnerPassword),
				new Credentials(coreUser.getValidUsername(), coreUser.getValidPassword()),
				new UserDetails(
				        new Credentials(coreUser.getValidUsername(), coreUser.getValidPassword()),
                        "",
                        coreUser.getRecoveryMail(),
                        UserRole.PLATFORM_OWNER,
                        new HashMap<>(),
                        new HashMap<>()
				),
				OperationType.CREATE
			);

        try {
            ManagementStatus managementStatus = rabbitManager.sendUserManagementRequest(userRegistrationRequest);

            if (managementStatus == null) {
                model.addAttribute("error","Authorization Manager is unreachable!");
                model.addAllAttributes(getAllAttributes(coreUser, bindingResult));
                return "register";

            } else if(managementStatus == ManagementStatus.OK ){
                return "success";

            } else if (managementStatus == ManagementStatus.USERNAME_EXISTS) {
                model.addAttribute("error","Username exist!");
                model.addAllAttributes(getAllAttributes(coreUser, bindingResult));
                return "register";

            } else {
                model.addAttribute("error","Authorization Manager is unreachable!");
                model.addAllAttributes(getAllAttributes(coreUser, bindingResult));
                return "register";
            }
        } catch (CommunicationException e) {
            model.addAttribute("error",e.getMessage());
            model.addAllAttributes(getAllAttributes(coreUser, bindingResult));
            return "register";
        }
	}

    /**
     * Used for testing
     */
    public void setRabbitManager(RabbitManager rabbitManager){
        this.rabbitManager = rabbitManager;
    }

    private Map<String, Object> getAllAttributes(CoreUser coreUser, BindingResult bindingResult) {
        Map<String, Object> map = new HashMap<>();
        map.put("allRoles", UserRoleValueTextMapping.getList());

        List<String> errors = bindingResult.getFieldErrors().stream()
                .map(FieldError::getField)
                .collect(Collectors.toList());

        if (!errors.contains("validUsername"))
            map.put("usernameSelected", coreUser.getUsername());
        if (!errors.contains("recoveryMail"))
            map.put("emailSelected", coreUser.getUsername());
        if (!errors.contains("role"))
            map.put("roleSelected", coreUser.getUsername());

        return map;
    }

}