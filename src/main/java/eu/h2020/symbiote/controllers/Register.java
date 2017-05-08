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
import eu.h2020.symbiote.model.PlatformOwner;

 
@Controller
public class Register {

	@Value("${aam.deployment.owner.username}")
	private String AAMOwnerUsername;
	@Value("${aam.deployment.owner.password}")
	private String AAMOwnerPassword;


	@Autowired
	private RabbitManager rabbitManager;

	@GetMapping("/platform/register")
	public String platformOwnerRegisterForm(PlatformOwner platformOwner) {
		return "register";
	}

	@PostMapping("/platform/register")
	public String platformOwnerRegister(@Valid PlatformOwner platformOwner, BindingResult bindingResult, Model model) {

		final DeferredResult<String> deferredResult = new DeferredResult<>();

		if (bindingResult.hasErrors()) {

			return "register";
		}

		String platformAAMURL = "hardcoded temporarily";
		String platformInstanceFriendlyName = "placeholder";
		String federatedId = (platformOwner.getFederatedId() == null)? "placeholder" : platformOwner.getFederatedId();

		UserDetails  platformOwnerUserDetails = new UserDetails(
				new Credentials( platformOwner.getUsername(), platformOwner.getPassword()),
				federatedId,
				platformOwner.getRecoveryMail(),
				UserRole.PLATFORM_OWNER
			);

		PlatformRegistrationRequest platformRegistrationRequest = new PlatformRegistrationRequest(
				new Credentials(AAMOwnerUsername, AAMOwnerPassword),
				platformOwnerUserDetails,
				platformAAMURL,
				platformInstanceFriendlyName,
				platformOwner.getPlatformId()
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
				return "register";				
			}
		
		} catch(CommunicationException e){

				model.addAttribute("error",e.getMessage());
				return "register";	
		}
	}
	

	@GetMapping("/app/register")
	public String appOwnerRegisterForm(PlatformOwner platformOwner) {
		return "register";
	}
}