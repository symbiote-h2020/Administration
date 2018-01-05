package eu.h2020.symbiote.administration.controllers;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Spring controller for user login.
 *
 * @author Tilemachos Pechlivanoglou (ICOM)
 */
@Controller
@CrossOrigin
public class Login {
    private static Log log = LogFactory.getLog(Login.class);

	@GetMapping("/administration/user/login")
	public String userLogin() {

	    log.debug("GET on /administration/user/login");

	    // Checking if the Principal == null also works
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		if (!(auth instanceof AnonymousAuthenticationToken)) {
			log.debug("User is already logged in");
			log.debug("User authorities = " + auth.getAuthorities());

			if (auth.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
				log.debug("Redirecting to /administration/admin/cpanel");
				return "redirect:/administration/admin/cpanel";
			}
			else {
				log.debug("Redirecting to /administration/user/cpanel");
				return "redirect:/administration/user/cpanel";
			}
		} else {
			return "login";
		}
	}

	@GetMapping("/administration/admin/login")
	public String adminLogin() {

        log.debug("GET on /administration/admin/login");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		if (!(auth instanceof AnonymousAuthenticationToken)) {
			log.debug("User is already logged in");
			log.debug("Redirecting to /administration/admin/cpanel");
			return "redirect:/administration/admin/cpanel";
		} else {
			return "login";
		}
	}

    @GetMapping("/administration/user/logout")
    public String userLogout(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null){
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return "redirect:/administration";
    }

    @GetMapping("/administration/admin/logout")
    public String adminLogout(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null){
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return "redirect:/administration";
    }
}