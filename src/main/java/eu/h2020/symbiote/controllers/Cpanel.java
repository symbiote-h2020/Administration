package eu.h2020.symbiote.controller;

import javax.validation.Valid;
import eu.h2020.symbiote.model.Platform;
import eu.h2020.symbiote.model.UserAccount;
import eu.h2020.symbiote.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.ui.Model;
import java.security.Principal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import eu.h2020.symbiote.communication.RabbitManager;
 
@Controller
public class Cpanel {

    @Autowired
    private UserService userService;

    @Autowired
    private RabbitManager rabbitManager;


    // USER

    @GetMapping("/user/cpanel")
    public String userCPanel(Model model, Principal principal) {

        model.addAttribute("role", "user");
        String username = principal.getName(); //get logged in username

        UserAccount user = userService.getUserDetails(username);

        if( user.getPlatformId() == null){

            model.addAttribute("platform_pending", false);
            model.addAttribute("platform_exists", false);

        } else if (user.getPlatformId().equals("__PENDING__") ){

            model.addAttribute("platform_pending", true);
            model.addAttribute("platform_exists", false);
        
        } else {

            model.addAttribute("platform_pending", false);
            model.addAttribute("platform_exists", true);
            model.addAttribute("platform_id", user.getPlatformId());
        }
        return "cpanel/root";
    }


    @PostMapping("/user/cpanel/register/platform")
    public String registerPlatform(@Valid Platform platform, BindingResult bindingResult, Model model, Principal principal) {

        if (bindingResult.hasErrors()) {
            return "cpanel/root";            
        }

        // todo check for "__PENDING__"

        String username = principal.getName(); //get logged in username
        userService.setUserPlatform(username, "__PENDING__");

        // Send registration to Registry
        rabbitManager.sendPlatformCreationRequest(platform, rpcPlatformResponse ->{
                   
            System.out.println("Received response in interface: " + rpcPlatformResponse);
        	if( rpcPlatformResponse.getStatus() == 200 ){

                userService.setUserPlatform(username, rpcPlatformResponse.getPlatform().getPlatformId());
           	}
       	});

        return "redirect:/user/cpanel";
    }

    @PostMapping("/user/cpanel/update/platform")
    public String updatePlatform(@Valid Platform platform, BindingResult bindingResult, Model model, Principal principal) {

        if (bindingResult.hasErrors()) {
            return "cpanel/root";            
        }
        String username = principal.getName(); //get logged in username
        UserAccount user = userService.getUserDetails(username);

        platform.setPlatformId(user.getPlatformId());
        

        // Send update to Registry
		rabbitManager.sendPlatformModificationRequest(platform, rpcPlatformResponse ->{
                   
            System.out.println("Received response in interface: " + rpcPlatformResponse);
        	if( rpcPlatformResponse.getStatus() == 200 ){

                userService.setUserPlatform(username, rpcPlatformResponse.getPlatform().getPlatformId());
           	}
       	});

        return "redirect:/user/cpanel";
    }

    @PostMapping("/user/cpanel/unregister/platform")
    public String unregisterPlatform(Model model, Principal principal) {

        String username = principal.getName(); //get logged in username
        UserAccount user = userService.getUserDetails(username);

        // Create an empty Platform, add the id and send unregistration to Registry
        Platform platform = new Platform();
        platform.setPlatformId(user.getPlatformId());

        // Send update to Registry
		rabbitManager.sendPlatformRemovalRequest(platform, rpcPlatformResponse ->{
                   
            System.out.println("Received response in interface: " + rpcPlatformResponse);
        	if( rpcPlatformResponse.getStatus() == 200 ){

                userService.setUserPlatform(username,null);
           	}
       	});
        

        return "redirect:/user/cpanel";
    }

    // ADMIN

    @RequestMapping("/admin/cpanel")
    public String adminCPanel(Model model) {
        model.addAttribute("role", "admin");
        return "cpanel/root";
    }

}