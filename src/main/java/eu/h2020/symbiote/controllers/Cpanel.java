package eu.h2020.symbiote.controller;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.validation.Valid;
import java.security.Principal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.core.model.InterworkingService;
import eu.h2020.symbiote.model.CoreUser;

import eu.h2020.symbiote.communication.RabbitManager;
import eu.h2020.symbiote.core.cci.PlatformRegistryResponse;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryRequest;
import eu.h2020.symbiote.core.internal.ResourceListResponse;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.core.model.internal.CoreResource;
import eu.h2020.symbiote.model.PlatformDetails;
import eu.h2020.symbiote.security.communication.payloads.OwnedPlatformDetails;
import eu.h2020.symbiote.communication.CommunicationException;
 
/**
 * Spring controller for the User control panel, handles management views and form validation.
 *
 * @author Tilemachos Pechlivanoglou (ICOM)
 */
@Controller
public class Cpanel {
    private static Log log = LogFactory.getLog(Cpanel.class);


    @Autowired
    private RabbitManager rabbitManager;


    /**
     * Gets the default view. If the user is a platform owner, tries to fetch their details.
     * Registry is first polled and, if the platform isn't activated there, AAM is polled for them.
     */
    @GetMapping("/user/cpanel")
    public String userCPanel(Model model, Principal principal) {

        String username = principal.getName(); //get logged in username
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken)principal;
        CoreUser user = (CoreUser)token.getPrincipal();

        // If we hanen't fetched the details already

        if(user.getState() == CoreUser.PLATFORM_INACTIVE || user.getState() == CoreUser.ERROR){

            // request owner's platform details from Registry
            Platform emptyPlatform = new Platform();
            emptyPlatform.setId(user.getPlatformId());

            try{
                PlatformRegistryResponse response = rabbitManager.sendPlatformModificationRequest(emptyPlatform);
                
                // if platform exists
                if(response!=null && response.getStatus() == 200){ 

                    Platform platformReply = response.getPlatform();
                    InterworkingService interworkingServiceReply = platformReply.getInterworkingServices().get(0);
                    user.setState(CoreUser.PLATFORM_ACTIVE);
                    user.setPlatformName(platformReply.getLabels().get(0));
                    user.setPlatformUrl(interworkingServiceReply.getUrl());
                    PlatformDetails platformDetails = 
                        new PlatformDetails(platformReply.getComments().get(0), interworkingServiceReply.getInformationModelId());
                    user.setPlatformDetails(platformDetails);

                    CoreResourceRegistryRequest resourcesRequest = new CoreResourceRegistryRequest();
                    resourcesRequest.setToken("Token"); // TODO set token
                    resourcesRequest.setPlatformId(user.getPlatformId());

                    ResourceListResponse resourceList = sendRegistryResourcesRequest(resourcesRequest);

                    List<Resource> resourceList = platformReply.getResources();
                    List<String> resourceStringList = new ArrayList<String>();

                    for (Resource resource : resourceList ) {
                        resourceStringList.add(
                            "Id: " + resource.getId() +
                            ", Name: " + resource.getLabels().get(0) +
                            ", Description: " + resource.getComments().get(0))
                    }
                    
                    model.addObject("resources", resourceList);


                // if only owner exists
                } else if (response!=null && response.getStatus() == 400){ 

                    // get owner details from AAM
                    OwnedPlatformDetails ownerDetails = rabbitManager.sendDetailsRequest(user.getToken().getToken());
                    if(ownerDetails != null && ownerDetails.getPlatformInstanceId().equals(user.getPlatformId() )){
                        // plaform ids match, all is well

                        user.setPlatformDetails(null);
                        user.setPlatformName(ownerDetails.getPlatformInstanceFriendlyName());
                        user.setPlatformUrl(ownerDetails.getPlatformInterworkingInterfaceAddress());

                    } else {
                        user.setState(CoreUser.ERROR);
                    }
                }  else {
                    user.setState(CoreUser.ERROR);
                }
            } catch(CommunicationException e){
                user.setState(CoreUser.ERROR);
            } catch(NullPointerException e){
                user.setState(CoreUser.ERROR);
            }
        }
            
        model.addAttribute("user", user);

        return "controlpanel";
    }


    @PostMapping("/user/cpanel/activate")
    public String activatePlatform(
        @Valid PlatformDetails platformDetails, BindingResult bindingResult, RedirectAttributes model, Principal principal) {

        if (bindingResult.hasErrors()) {

            List<FieldError> errors = bindingResult.getFieldErrors();
            String errorMessage = "";
            for (FieldError fieldError : errors) {
                errorMessage = fieldError.getDefaultMessage();
                model.addFlashAttribute("error_"+fieldError.getField(), errorMessage.substring(0, 1).toUpperCase() + errorMessage.substring(1));
            }
            model.addFlashAttribute("page", "activate");
            return "redirect:/user/cpanel";  
        }

        // if form is valid, construct the request

        String username = principal.getName(); //get logged in username
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken)principal;
        CoreUser user = (CoreUser)token.getPrincipal();
        
        InterworkingService interworkingService = new InterworkingService();
        interworkingService.setInformationModelId(platformDetails.getInformationModelId());
        interworkingService.setUrl(user.getPlatformUrl());

        Platform platform = new Platform();
        platform.setId(user.getPlatformId());
        platform.setLabels(Arrays.asList(user.getPlatformName()));
        platform.setComments(Arrays.asList(platformDetails.getDescription()));
        platform.setInterworkingServices(Arrays.asList(interworkingService));

        // Send registration to Registry
        try{
            PlatformRegistryResponse response = rabbitManager.sendPlatformCreationRequest(platform);

            if(response != null && response.getStatus() == 200 ){

                Platform platformReply = response.getPlatform();
                user.setPlatformDetails(platformDetails);

            } else {
                model.addFlashAttribute("error","Error During Activation!");
            }
        
        } catch(CommunicationException e){

            model.addFlashAttribute("error",e.getMessage());
        }

        return "redirect:/user/cpanel";  
    }

    @PostMapping("/user/cpanel/modify")
    public String modifyPlatform(
        @Valid PlatformDetails platformDetails, BindingResult bindingResult, RedirectAttributes model, Principal principal) {

        if (bindingResult.hasErrors()) {

            List<FieldError> errors = bindingResult.getFieldErrors();
            String errorMessage = "";
            for (FieldError fieldError : errors) {
                errorMessage = fieldError.getDefaultMessage();
                model.addFlashAttribute("error_"+fieldError.getField(), errorMessage.substring(0, 1).toUpperCase() + errorMessage.substring(1));
            }
            model.addFlashAttribute("page", "modify");
            return "redirect:/user/cpanel";  
        }

        // if form is valid, construct the request
        
        String username = principal.getName(); //get logged in username
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken)principal;
        CoreUser user = (CoreUser)token.getPrincipal();
        
        InterworkingService interworkingService = new InterworkingService();
        interworkingService.setInformationModelId(platformDetails.getInformationModelId());
        interworkingService.setUrl(user.getPlatformUrl());

        Platform platform = new Platform();
        platform.setId(user.getPlatformId());
        platform.setLabels(Arrays.asList(user.getPlatformName()));
        platform.setComments(Arrays.asList(platformDetails.getDescription()));
        platform.setInterworkingServices(Arrays.asList(interworkingService));

        // Send registration to Registry
        try{
            PlatformRegistryResponse response = rabbitManager.sendPlatformModificationRequest(platform);

            if(response != null && response.getStatus() == 200 ){

                Platform platformReply = response.getPlatform();
                user.setPlatformDetails(platformDetails);

            } else {
                model.addFlashAttribute("error","Error During Activation!");
            }
        
        } catch(CommunicationException e){

            model.addFlashAttribute("error",e.getMessage());
        }

        return "redirect:/user/cpanel";  
    }

    @PostMapping("/user/cpanel/disable")
    public String disablePlatform(RedirectAttributes model, Principal principal) {

        String username = principal.getName(); //get logged in username
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken)principal;
        CoreUser user = (CoreUser)token.getPrincipal();
        String platformId = user.getPlatformId(); //get logged in platform id

        // Create an empty Platform, add the id and send unregistration to Registry

        Platform platform = new Platform();
        platform.setId(platformId);

        Platform testPlatform = new Platform();
        testPlatform.setId(platformId); //null platform

        // Send update to Registry
        try{
            PlatformRegistryResponse response = rabbitManager.sendPlatformRemovalRequest(testPlatform);

            if(response != null && response.getStatus() == 200 ){

                user.setState(CoreUser.PLATFORM_INACTIVE);
                user.setPlatformDetails(null);

            } else {
                model.addFlashAttribute("error","Authorization Manager is unreachable!");
            }
        
        } catch(CommunicationException e){

                model.addFlashAttribute("error",e.getMessage());
        }

        return "redirect:/user/cpanel";  
    }

    // ADMIN

    @RequestMapping("/admin/cpanel")
    public String adminCPanel(Model model) {
        model.addAttribute("role", "admin");
        return "/user/cpanel";
    }


    /**
     * Used for testing
     */
    public void setRabbitManager(RabbitManager rabbitManager){
        this.rabbitManager = rabbitManager;
    }
}