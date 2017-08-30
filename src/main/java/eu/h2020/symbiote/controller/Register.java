package eu.h2020.symbiote.controller;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.validation.BindingResult;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;

import eu.h2020.symbiote.communication.RabbitManager;
import eu.h2020.symbiote.communication.CommunicationException;
import eu.h2020.symbiote.model.CoreUser;
import eu.h2020.symbiote.security.communication.payloads.Credentials;
import eu.h2020.symbiote.security.communication.payloads.PlatformManagementRequest;
import eu.h2020.symbiote.security.communication.payloads.PlatformManagementResponse;
import eu.h2020.symbiote.security.communication.payloads.UserManagementRequest;
import eu.h2020.symbiote.security.communication.payloads.UserDetails;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import eu.h2020.symbiote.security.commons.enums.OperationType;
import eu.h2020.symbiote.security.commons.enums.UserRole;

 
/**
 * Spring controller, handles user registration views and form validation.
 *
 * @author Tilemachos Pechlivanoglou (ICOM)
 */
@Controller
public class Register {

    private static Log log = LogFactory.getLog(Register.class);

    @Autowired
    private RabbitManager rabbitManager;


	@Value("${aam.deployment.owner.username}")
	private String aaMOwnerUsername;
	@Value("${aam.deployment.owner.password}")
	private String aaMOwnerPassword;
	@Value("${interworkingInterface.defaultPort}")
	private String defaultIIPort;


	// The CoreUser argument is needed so that the template can associate form attributes with a CoreUser
	@GetMapping("/register/platform")
	public String coreUserRegisterForm(CoreUser coreUser) {
		log.debug("GET request on /register/platform");
	    return "register";
	}

	@PostMapping("/register/platform")
	public String coreUserRegister(@Valid CoreUser coreUser, BindingResult bindingResult, Model model) {

        log.debug("POST request on /register/platform");

		if (bindingResult.hasErrors()) {

			return "register";
		}

		// if form is valid, do some processing of the fields


        // ToDo: placeholder?
		// String federatedId = (coreUser.getFederatedId() == null)? "placeholder" : coreUser.getFederatedId();

		String platformUrl = coreUser.getPlatformUrl();
		
		// make sure we are using https
		if(!platformUrl.startsWith("http")){
			platformUrl = "https://" + platformUrl;
			
		} else if(platformUrl.startsWith("http://")){
			platformUrl = platformUrl.replace("http://","https://");
		}

		// strip any trailing slashes
		platformUrl = platformUrl.replaceAll("/$", "");
		
		// if port isn't included, add the default
		if(!platformUrl.matches("[^:]+:[^:]+:[^:]+")){

			String[] parts = platformUrl.split("/");
			parts[2] += ":" + defaultIIPort;

			platformUrl = String.join("/",parts);
		}

		coreUser.setPlatformUrl(platformUrl);

		// after processing, construct the request

		UserManagementRequest userRegistrationRequest = new UserManagementRequest(
				new Credentials(aaMOwnerUsername, aaMOwnerPassword),
				new Credentials( coreUser.getValidUsername(), coreUser.getValidPassword()),
				new UserDetails(
					new Credentials( coreUser.getValidUsername(), coreUser.getValidPassword()),
					coreUser.getFederatedId(),
					coreUser.getRecoveryMail(),
					UserRole.PLATFORM_OWNER
				),
				OperationType.CREATE
			);

		PlatformManagementRequest platformRegistrationRequest = new PlatformManagementRequest(
				new Credentials(aaMOwnerUsername, aaMOwnerPassword),
				new Credentials(coreUser.getValidUsername(), coreUser.getValidPassword()),
				coreUser.getPlatformUrl(),
				coreUser.getPlatformName(),
				OperationType.CREATE
			);


		// Send platform owner registration to Core AAM
		try{
			PlatformManagementResponse response = rabbitManager.sendPlatformRegistrationRequest(platformRegistrationRequest);

			if(response != null && response.getRegistrationStatus() == ManagementStatus.OK){

			    // Todo: Should we remove the " "?
				model.addAttribute("pcert","response.getPlatformOwnerCertificate()");
				model.addAttribute("pkey","response.getPlatformOwnerPrivateKey()");
				model.addAttribute("pid",response.getPlatformId());

				return "success";

			} else {
				model.addAttribute("error","Authorization Manager is unreachable!");
				return "register";				
			}
		
		} catch(CommunicationException e){

				model.addAttribute("error",e.getMessage());
				return "register";	
		}
	}


    // The CoreUser argument is needed so that the template can associate form attributes with a CoreUser
	@GetMapping("/register/app")
	public String appOwnerRegisterForm(CoreUser coreUser) {
		log.debug("GET request on /register/app");
	    return "register";
	}

	// Todo: POST method for app

    /**
     * Used for testing
     */
    public void setRabbitManager(RabbitManager rabbitManager){
        this.rabbitManager = rabbitManager;
    }
}