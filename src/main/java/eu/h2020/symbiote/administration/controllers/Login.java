package eu.h2020.symbiote.administration.controllers;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.Map;


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
			return "redirect:/administration";
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
			return "redirect:/administration";
		}
	}

    @GetMapping("/administration/isAuthenticated")
    public ResponseEntity<?> isAuthenticated() {

        log.debug("GET on /administration/isAuthenticated");

        // Checking if the Principal == null also works
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth instanceof AnonymousAuthenticationToken)) {
            log.debug("User is already logged in");
            log.debug("User authorities = " + auth.getAuthorities());

            Map<String, String> response = new HashMap<>();

            if (auth.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
                log.debug("The user is an admin");
                response.put("role", "ADMIN");
                return new ResponseEntity<>(response, new HttpHeaders(), HttpStatus.OK);
            } else {
                log.debug("The user is a normal user");
                response.put("role", "USER");
                return new ResponseEntity<>(response, new HttpHeaders(), HttpStatus.OK);
            }
        } else {
            return new ResponseEntity<>(new HttpHeaders(), HttpStatus.UNAUTHORIZED);
        }
    }
}