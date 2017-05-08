package eu.h2020.symbiote.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;

 
@Controller
public class Login {


	@GetMapping("/platform/login")
	public String platformOwnerLogin() {

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		if (!(auth instanceof AnonymousAuthenticationToken)) {

			/* The user is logged in :) */
			return "forward:/platform/cpanel";
		} else {
			return "login";
		}
	}

	@GetMapping("/app/login")
	public String appOwnerLogin() {

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		if (!(auth instanceof AnonymousAuthenticationToken)) {

			/* The user is logged in :) */
			return "forward:/app/cpanel";
		} else {
			return "login";
		}
	}

	@GetMapping("/admin/login")
	public String adminLogin() {

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		if (!(auth instanceof AnonymousAuthenticationToken)) {

			/* The user is logged in :) */
			return "forward:/admin/cpanel";
		} else {
			return "login";
		}
	}
}