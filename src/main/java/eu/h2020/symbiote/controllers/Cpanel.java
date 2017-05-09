package eu.h2020.symbiote.controller;

import javax.validation.Valid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.security.Principal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.model.CoreUser;

import eu.h2020.symbiote.communication.RabbitManager;
import eu.h2020.symbiote.model.PlatformResponse;
import eu.h2020.symbiote.communication.CommunicationException;
 
@Controller
public class Cpanel {
    private static Log log = LogFactory.getLog(Cpanel.class);


    @Autowired
    private RabbitManager rabbitManager;


    // USER

    @GetMapping("/platform/cpanel")
    public String userCPanel(Model model, Principal principal) {

        String username = principal.getName(); //get logged in username
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken)principal;
        CoreUser user = (CoreUser)token.getPrincipal();
        String platformId = user.getPlatformId(); //get logged in platform id
        Platform platform = user.getPlatform();

        if( platform == null){

            model.addAttribute("platform_pending", false);
            model.addAttribute("platform_exists", false);

        } else {

            model.addAttribute("platform_pending", false);
            model.addAttribute("platform_exists", true);
            model.addAttribute("platform_id", platformId);
            model.addAttribute("platform_name", platform.getName());
            model.addAttribute("platform_description", platform.getDescription());
            model.addAttribute("platform_url", platform.getUrl());
            model.addAttribute("platform_im", platform.getInformationModelId());           

        }

        return "cpanel/root";
    }


    @PostMapping("/platform/cpanel/register")
    public String registerPlatform(@Valid Platform platform, BindingResult bindingResult, RedirectAttributes model, Principal principal) {

        if (bindingResult.hasErrors()) {
            return "cpanel/root";            
        }


        String username = principal.getName(); //get logged in username
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken)principal;
        CoreUser user = (CoreUser)token.getPrincipal();
        String platformId = user.getPlatformId(); //get logged in platform id

        // Send registration to Registry
        try{
            PlatformResponse response = rabbitManager.sendPlatformCreationRequest(platform);
            System.out.println("Received response in interface: " + response);

            if(response != null && response.getStatus() == 200 ){

                Platform platformReply = response.getPlatform();
                user.setPlatform(platformReply);

            } else {
                model.addFlashAttribute("error","Authorization Manager is unreachable!");
            }
        
        } catch(CommunicationException e){

            model.addFlashAttribute("error",e.getMessage());
        }
        return "redirect:/platform/cpanel";  
    }

    @PostMapping("/platform/cpanel/update")
    public String updatePlatform(@Valid Platform platform, BindingResult bindingResult, RedirectAttributes model, Principal principal) {

        if (bindingResult.hasErrors()) {
            return "cpanel/root";            
        }


        String username = principal.getName(); //get logged in username
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken)principal;
        CoreUser user = (CoreUser)token.getPrincipal();
        String platformId = user.getPlatformId(); //get logged in platform id

        // Send registration to Registry
        try{
            PlatformResponse response = rabbitManager.sendPlatformModificationRequest(platform);
            System.out.println("Received response in interface: " + response);

            if(response != null && response.getStatus() == 200 ){

                Platform platformReply = response.getPlatform();
                user.setPlatform(platformReply);

            } else {
                model.addFlashAttribute("error","Authorization Manager is unreachable!");
            }
        
        } catch(CommunicationException e){

            model.addFlashAttribute("error",e.getMessage());
        }
        return "redirect:/platform/cpanel";  
    }

    @PostMapping("/platform/cpanel/unregister")
    public String unregisterPlatform(RedirectAttributes model, Principal principal) {

        String username = principal.getName(); //get logged in username
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken)principal;
        CoreUser user = (CoreUser)token.getPrincipal();
        String platformId = user.getPlatformId(); //get logged in platform id

        // Create an empty Platform, add the id and send unregistration to Registry
        Platform platform = new Platform();
        platform.setPlatformId(platformId);

        // Send update to Registry
		try{
            PlatformResponse response = rabbitManager.sendPlatformModificationRequest(platform);
            System.out.println("Received response in interface: " + response);

            if(response != null && response.getStatus() == 200 ){

                user.setPlatform(null);

            } else {
                model.addFlashAttribute("error","Authorization Manager is unreachable!");
            }
        
        } catch(CommunicationException e){

                model.addFlashAttribute("error",e.getMessage());
        }
        return "redirect:/platform/cpanel";
    }

    // ADMIN

    @RequestMapping("/admin/cpanel")
    public String adminCPanel(Model model) {
        model.addAttribute("role", "admin");
        return "cpanel/root";
    }

}