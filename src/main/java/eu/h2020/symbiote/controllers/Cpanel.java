package eu.h2020.symbiote.controller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
 
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
}