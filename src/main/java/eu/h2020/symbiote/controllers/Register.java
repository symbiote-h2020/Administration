package eu.h2020.symbiote.controller;


import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.validation.BindingResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.context.request.async.DeferredResult;

import eu.h2020.symbiote.communication.RabbitManager;
import eu.h2020.symbiote.communication.CommunicationException;
import eu.h2020.symbiote.security.payloads.*;
import eu.h2020.symbiote.security.enums.UserRole;
import eu.h2020.symbiote.model.CoreUser;

 
@Controller
public class Register {

    @Autowired
    private RabbitManager rabbitManager;


	@Value("${aam.deployment.owner.username}")
	private String AAMOwnerUsername;
	@Value("${aam.deployment.owner.password}")
	private String AAMOwnerPassword;



	@GetMapping("/platform/register")
	public String coreUserRegisterForm(CoreUser coreUser) {
		return "register";
	}

	@PostMapping("/platform/register")
	public String coreUserRegister(@Valid CoreUser coreUser, BindingResult bindingResult, Model model) {

		final DeferredResult<String> deferredResult = new DeferredResult<>();

		if (bindingResult.hasErrors()) {

			return "/register";
		}

		String federatedId = (coreUser.getFederatedId() == null)? "placeholder" : coreUser.getFederatedId();
		String platformUrl = coreUser.getPlatformUrl();
		
		if(!platformUrl.startsWith("http")){
			platformUrl = "https://" + platformUrl;
			
		} else if(platformUrl.startsWith("http://")){
			platformUrl.replace("http://","https://");
		}

		if(!platformUrl.matches("[^:]+:[^:]+:[^:]+")){ // if port isn't included

			String[] parts = platformUrl.split("/");
			parts[2] += ":8101";
			platformUrl = String.join("/",parts);
		}

		UserDetails  coreUserUserDetails = new UserDetails(
				new Credentials( coreUser.getValidUsername(), coreUser.getValidPassword()),
				federatedId,
				coreUser.getRecoveryMail(),
				UserRole.PLATFORM_OWNER
			);

		PlatformRegistrationRequest platformRegistrationRequest = new PlatformRegistrationRequest(
				new Credentials(AAMOwnerUsername, AAMOwnerPassword),
				coreUserUserDetails,
				coreUser.getPlatformUrl(),
				coreUser.getPlatformName(),
				coreUser.getPlatformId()
			);


		// Send platform owner registration to Core AAM
		try{
			PlatformRegistrationResponse response = rabbitManager.sendPlatformRegistrationRequest(platformRegistrationRequest);
			System.out.println("Received response in interface: " + response);

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
}