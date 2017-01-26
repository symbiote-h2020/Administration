package eu.h2020.symbiote.controller;

import javax.validation.Valid;
import eu.h2020.symbiote.model.UserAccount;
import eu.h2020.symbiote.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
 
@Controller
public class Register {


    @Autowired
	private UserService userService;

	@GetMapping("/register")
	public String userRegisterForm(UserAccount userAccount) {
		return "register";
	}

	@PostMapping("/register")
	public String appRegister(@Valid UserAccount userAccount, BindingResult bindingResult, RedirectAttributes redirectAttributes) {

		if (bindingResult.hasErrors()) {
			return "register";
			
		}

		// User registration form correct
		userService.registerNewUser(userAccount);

		
		redirectAttributes.addFlashAttribute("message","Registration successful, please log in with your new account to continue.");
		return "redirect:/user/login";
	}
}