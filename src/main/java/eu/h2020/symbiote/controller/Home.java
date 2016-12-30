package eu.h2020.symbiote.controller;
 
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
 
@Controller
public class Home {
	
	@RequestMapping("/test")
	public String index() {
		return "index";
	}
}