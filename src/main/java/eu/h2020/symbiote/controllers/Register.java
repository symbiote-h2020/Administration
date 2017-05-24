package eu.h2020.symbiote.controller;


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
import eu.h2020.symbiote.security.enums.UserRole;
import eu.h2020.symbiote.security.payloads.*;
import eu.h2020.symbiote.model.CoreUser;

 
/**
 * Spring controller, handles user registration views and form validation.
 *
 * @author Tilemachos Pechlivanoglou (ICOM)
 */
@Controller
public class Register {

    @Autowired
    private RabbitManager rabbitManager;


	@Value("${aam.deployment.owner.username}")
	private String aaMOwnerUsername;
	@Value("${aam.deployment.owner.password}")
	private String aaMOwnerPassword;
	@Value("${interworkingInterface.defaultPort}")
	private String defaultIIPort;



	@GetMapping("/platform/register")
	public String coreUserRegisterForm(CoreUser coreUser) {
		return "register";
	}

	@PostMapping("/platform/register")
	public String coreUserRegister(@Valid CoreUser coreUser, BindingResult bindingResult, Model model) {

		if (bindingResult.hasErrors()) {

			return "/register";
		}

		// if form is valid, do some processing of the fields

		String federatedId = (coreUser.getFederatedId() == null)? "placeholder" : coreUser.getFederatedId();
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

		// after processing, construct the request

		UserDetails  coreUserUserDetails = new UserDetails(
				new Credentials( coreUser.getValidUsername(), coreUser.getValidPassword()),
				federatedId,
				coreUser.getRecoveryMail(),
				UserRole.PLATFORM_OWNER
			);

		PlatformRegistrationRequest platformRegistrationRequest = new PlatformRegistrationRequest(
				new Credentials(aaMOwnerUsername, aaMOwnerPassword),
				coreUserUserDetails,
				coreUser.getPlatformUrl(),
				coreUser.getPlatformName(),
				coreUser.getPlatformId()
			);


		// Send platform owner registration to Core AAM
		try{
			PlatformRegistrationResponse response = rabbitManager.sendPlatformRegistrationRequest(platformRegistrationRequest);

			if(response != null ){

				model.addAttribute("pcert",response.getPlatformOwnerCertificate());
				model.addAttribute("pkey",response.getPlatformOwnerPrivateKey());
				model.addAttribute("pid",response.getPlatformId());

				return "success";

			} else {
				model.addAttribute("error","Authorization Manager is unreachable!");
				return "/register";				
			}
		
		} catch(CommunicationException e){

				model.addAttribute("error",e.getMessage());
				return "/register";	
		}
	}
	

	@GetMapping("/app/register")
	public String appOwnerRegisterForm(CoreUser coreUser) {
		return "register";
	}


    /**
     * Used for testing
     */
    public void setRabbitManager(RabbitManager rabbitManager){
        this.rabbitManager = rabbitManager;
    }
}