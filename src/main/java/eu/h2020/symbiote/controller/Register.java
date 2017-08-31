package eu.h2020.symbiote.controller;


import eu.h2020.symbiote.communication.CommunicationException;
import eu.h2020.symbiote.model.UserRoleValueTextMapping;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.validation.BindingResult;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;

import eu.h2020.symbiote.communication.RabbitManager;
import eu.h2020.symbiote.model.CoreUser;
import eu.h2020.symbiote.security.communication.payloads.Credentials;
import eu.h2020.symbiote.security.communication.payloads.UserManagementRequest;
import eu.h2020.symbiote.security.communication.payloads.UserDetails;
import eu.h2020.symbiote.security.commons.enums.OperationType;
import eu.h2020.symbiote.security.commons.enums.UserRole;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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
            model.addAttribute("allRoles", UserRoleValueTextMapping.getList());
			return "register";
		}

		log.debug(ReflectionToStringBuilder.toString(coreUser));

		// Construct the UserManagementRequest
        // Todo: Change the federatedId in R4

		UserManagementRequest userRegistrationRequest = new UserManagementRequest(
				new Credentials(aaMOwnerUsername, aaMOwnerPassword),
				new Credentials(coreUser.getValidUsername(), coreUser.getValidPassword()),
				new UserDetails(
					new Credentials( coreUser.getValidUsername(), coreUser.getValidPassword()),
					"",
					coreUser.getRecoveryMail(),
					UserRole.PLATFORM_OWNER
				),
				OperationType.CREATE
			);

        try {
            ManagementStatus managementStatus = rabbitManager.sendUserManagementRequest(userRegistrationRequest);

            if(managementStatus != null ){
                return "success";

            } else {
                model.addAttribute("error","Authorization Manager is unreachable!");
                model.addAttribute("allRoles", UserRoleValueTextMapping.getList());
                return "register";
            }
        } catch (CommunicationException e) {
            model.addAttribute("error",e.getMessage());
            model.addAttribute("allRoles", UserRoleValueTextMapping.getList());
            return "register";
        }

	}

    /**
     * Used for testing
     */
    public void setRabbitManager(RabbitManager rabbitManager){
        this.rabbitManager = rabbitManager;
    }

}