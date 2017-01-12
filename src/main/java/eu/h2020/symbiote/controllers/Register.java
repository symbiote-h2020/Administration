package eu.h2020.symbiote.controller;

import javax.validation.Valid;
import eu.h2020.symbiote.entities.AppAccount;
import eu.h2020.symbiote.entities.PlatformAccount;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
 
@Controller
public class Register {

	@GetMapping("/register/app")
	public String appRegisterForm(AppAccount appAccount) {
		return "register/app";
	}

	@PostMapping("/register/app")
	public String appRegister(@Valid AppAccount appAccount, BindingResult bindingResult, RedirectAttributes redirectAttributes) {

		if (bindingResult.hasErrors()) {
			return "register/app";
			
		}

		// Application registration form correct, communication with Registry component etc. should be done here

		
		redirectAttributes.addFlashAttribute("message","Registration successful, please log in with your new account to continue.");
		return "redirect:/app/login";
	}


	@GetMapping("/register/platform")
	public String platformRegisterForm(PlatformAccount platformAccount) {
		return "register/platform";
	}

	@PostMapping("/register/platform")
	public String platformRegister(@Valid PlatformAccount platformAccount, BindingResult bindingResult, RedirectAttributes redirectAttributes) {

		if (bindingResult.hasErrors()) {
			return "register/platform";
		}

		// Platform registration form correct, communication with Registry component etc. should be done here

		
		redirectAttributes.addFlashAttribute("message","Registration successful, please log in with your new account to continue.");
		return "redirect:/platform/login";
	}
}