package eu.h2020.symbiote.controller;

// import eu.h2020.symbiote.entities.AppAccount;
// import eu.h2020.symbiote.entities.PlatformAccount;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
 
@Controller
public class Cpanel {
	
	@RequestMapping("/app/cpanel")
	public String appCPanel(Model model) {
		model.addAttribute("role", "app");
		return "cpanel/root";
	}

	@RequestMapping("/platform/cpanel")
	public String platformCPanel(Model model) {
		model.addAttribute("role", "platform");
		return "cpanel/root";
	}

	@RequestMapping("/admin/cpanel")
	public String adminCPanel(Model model) {
		model.addAttribute("role", "admin");
		return "cpanel/root";
	}

	
	// @PostMapping("/platform/cpanel/unregister")
	// public String platformUnregister(@ModelAttribute(value="platformAccount") PlatformAccount platformAccount) {

	// 	// check if 

	// 	return "cpanel/root";
	// }

}