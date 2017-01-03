package eu.h2020.symbiote.controller;

import java.util.Arrays; 
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
 
@Controller
public class Cpanel {
	
	@RequestMapping("/app/cpanel")
	public String appCPanel() {
		return "cpanel";
	}

	@RequestMapping("/platform/cpanel")
	public String platformCPanel() {
		return "cpanel";
	}

	@RequestMapping("/admin/cpanel")
	public String adminCPanel() {
		return "cpanel";
	}
}