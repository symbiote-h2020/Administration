package eu.h2020.symbiote.administration.controllers;

import java.util.*;

import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.administration.model.*;
import eu.h2020.symbiote.administration.model.mappers.InformationModelMapper;
import eu.h2020.symbiote.core.cci.PlatformRegistryResponse;
import eu.h2020.symbiote.core.internal.InformationModelListResponse;
import eu.h2020.symbiote.core.model.InformationModel;
import eu.h2020.symbiote.core.model.InterworkingService;
import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import eu.h2020.symbiote.security.commons.enums.OperationType;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.communication.payloads.*;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.validation.Valid;
import java.security.Principal;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;

/**
 * Spring controller for the User control panel, handles management views and form validation.
 *
 * @author Tilemachos Pechlivanoglou (ICOM)
 */
@Controller
public class Cpanel {
    private static Log log = LogFactory.getLog(Cpanel.class);

    @Value("${aam.deployment.owner.username}")
    private String aaMOwnerUsername;

    @Value("${aam.deployment.owner.password}")
    private String aaMOwnerPassword;

    @Autowired
    private RabbitManager rabbitManager;

    private List<String> validInfoModelIds = new ArrayList<>();

    /**
     * Gets the default view. If the user is a platform owner, tries to fetch their details.
     * Registry is first polled and, if the platform isn't activated there, AAM is polled for them.
     */
    @GetMapping("/user/cpanel")
    public String userCPanel(Model model, Principal principal) {

        log.debug("GET request on /user/cpanel");

        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();

        log.debug("User state is: " + ReflectionToStringBuilder.toString(user));

        model.addAttribute("user", user);

        UserManagementRequest ownedPlatformDetailsRequest = new UserManagementRequest(
                new Credentials(aaMOwnerUsername, aaMOwnerPassword),
                new Credentials(user.getUsername(), ""),
                new UserDetails(
                        new Credentials(user.getUsername(), ""),
                        "",
                        "",
                        UserRole.NULL,
                        new HashMap<>(),
                        new HashMap<>()
                ),
                OperationType.CREATE
        );

        // Get OwnedPlatformDetails from AAM
        try {
            Set<OwnedPlatformDetails> ownedPlatformDetailsSet =
                    rabbitManager.sendOwnedPlatformDetailsRequest(ownedPlatformDetailsRequest);
            if (ownedPlatformDetailsSet != null) {
                for (OwnedPlatformDetails detail : ownedPlatformDetailsSet) {
                    log.debug("OwnedPlatformDetails: " + ReflectionToStringBuilder.toString(detail));
                }
                model.addAttribute("platforms", ownedPlatformDetailsSet);
            } else {
                model.addAttribute("ownedPlatformDetailsError",
                        "Could not get Owned Platform Details from Core AAM");
            }
        } catch (CommunicationException e) {
            e.printStackTrace();
            model.addAttribute("communicationException", e.getErrorMessage());
        }

        // Get InformationModelList from Registry
        try {
            InformationModelListResponse informationModelListResponse = rabbitManager.sendListInfoModelsRequest();
            if (informationModelListResponse != null && informationModelListResponse.getStatus() == HttpStatus.OK.value()) {
                List<InformationModelMapper> infoModelsList = new ArrayList<>();
                validInfoModelIds.clear();

                for (InformationModel informationModel : informationModelListResponse.getInformationModels()) {
                    log.debug("Information Model" + ReflectionToStringBuilder.toString(informationModel));
                    infoModelsList.add(new InformationModelMapper(informationModel.getId(), informationModel.getName()));
                    validInfoModelIds.add(informationModel.getId());
                }

                model.addAttribute("infoModels", infoModelsList);
            } else {
                model.addAttribute("infoModelListError",
                        "Could not retrieve information models list from registry");
            }
        } catch (CommunicationException e) {
            e.printStackTrace();
        }

        return "controlpanel";
    }


    @PostMapping("/user/cpanel/register_platform")
    public String registerPlatform(@Valid @ModelAttribute("platformDetails") PlatformDetails platformDetails,
                                   BindingResult bindingResult, RedirectAttributes model, Principal principal) {

        log.debug("POST request on /user/cpanel/register_platform");

        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();

        log.debug("User state is: " + ReflectionToStringBuilder.toString(user));
        log.debug(platformDetails.toString());

        boolean invalidInfoModel = false;

        int counter = 0;
        for (InterworkingService service : platformDetails.getInterworkingServices()) {
            if (!validInfoModelIds.contains(service.getInformationModelId())) {
                model.addFlashAttribute("pl_reg_error_interworkingServices_" + counter + "_informationModelId",
                        "Choose a valid information model");
                invalidInfoModel = true;
            }
            counter++;
        }

        if (bindingResult.hasErrors() || invalidInfoModel) {

            List<FieldError> errors = bindingResult.getFieldErrors();
            for (FieldError fieldError : errors) {
                String errorField = "";
                String errorMessage = fieldError.getDefaultMessage();
                String[] parts = fieldError.getField().split("\\.");

                if (parts.length > 1){
                    errorField = "pl_reg_error_" + parts[0].replace("[", "_").replace("]", "_") +
                    parts[1];
                }
                else
                    errorField = "pl_reg_error_" + fieldError.getField();

                log.debug(errorField + ": " + errorMessage);
                model.addFlashAttribute(errorField,
                        errorMessage.substring(0, 1).toUpperCase() + errorMessage.substring(1));
            }

            model.addFlashAttribute("platformRegistrationError", "Error in binding");
            model.addFlashAttribute("activeTab", "platform_details");
            model.addFlashAttribute("insertedPlatformDetails", platformDetails);
            return "redirect:/user/cpanel";  
        }


        // If form is valid, construct the PlatformManagementResponse to the AAM
        PlatformManagementRequest aamRequest = new PlatformManagementRequest(
                new Credentials(aaMOwnerUsername, aaMOwnerPassword), new Credentials(user.getUsername(), user.getPassword()),
                platformDetails.getInterworkingServices().get(0).getUrl(), platformDetails.getName(),
                platformDetails.getId(), OperationType.CREATE);
        try {
            PlatformManagementResponse aamResponse = rabbitManager.sendManagePlatformRequest(aamRequest);
            if(aamResponse != null) {
                log.debug("AAM responded with: " + aamResponse.getRegistrationStatus());

                if (aamResponse.getRegistrationStatus() == ManagementStatus.OK) {
                    // If AAM responds with OK construct the Platform registration request and send it to registry
                    Platform registryRequest = new Platform();
                    registryRequest.setId(platformDetails.getId());
                    registryRequest.setInterworkingServices(platformDetails.getInterworkingServices());
                    registryRequest.setEnabler(platformDetails.getIsEnabler());

                    // FIll in the labels. The first label is the name
                    ArrayList<String> labels = new ArrayList<>();
                    labels.add(platformDetails.getName());
                    for (Label label : platformDetails.getLabels())
                        labels.add(label.getLabel());
                    registryRequest.setLabels(labels);

                    // FIll in the comments. The first comment is the description
                    ArrayList<String> comments = new ArrayList<>();
                    comments.add(platformDetails.getDescription());
                    for (Comment comment : platformDetails.getComments())
                        comments.add(comment.getComment());
                    registryRequest.setComments(comments);

                    // Todo: Change to PlatformRegistryRequest?
                    try {
                        PlatformRegistryResponse registryResponse = rabbitManager.sendPlatformCreationRequest(registryRequest);
                        if (registryResponse != null) {
                            if (registryResponse.getStatus() == HttpStatus.OK.value()) {
                                // Platform registered successfully
                                model.addFlashAttribute("platformRegistrationSuccessful",
                                        "The Platform Registration was successful!");
                                model.addFlashAttribute("activeTab", "platform_details");

                            } else {
                                log.debug("Registration Failed: " + registryResponse.getMessage());

                                // Send deletion message to AAM
                                aamRequest.setOperationType(OperationType.DELETE);
                                aamResponse = rabbitManager.sendManagePlatformRequest(aamRequest);

                                // Todo: Check what happens when platform deletion request is not successful at this stage
                                if (aamResponse != null) {
                                    if (aamResponse.getRegistrationStatus() == ManagementStatus.OK) {
                                        log.debug("Platform was removed from AAM");
                                    } else {
                                        log.debug("Platform was NOT removed from AAM");
                                    }
                                } else {
                                    log.debug("AAM unreachable during platform deletion request");
                                }

                                model.addFlashAttribute("platformRegistrationError", registryResponse.getMessage());
                                model.addFlashAttribute("registryPlatformRegistrationError", registryResponse.getMessage());
                                model.addFlashAttribute("activeTab", "platform_details");
                                model.addFlashAttribute("insertedPlatformDetails", platformDetails);
                            }
                        } else {
                            log.debug("Registry unreachable!");
                            // Send deletion message to AAM
                            model.addFlashAttribute("platformRegistrationError", "Registry unreacheable");
                            model.addFlashAttribute("registryPlatformRegistrationError", "Registry unreacheable");
                            model.addFlashAttribute("activeTab", "platform_details");
                            model.addFlashAttribute("insertedPlatformDetails", platformDetails);
                        }
                    } catch (CommunicationException e) {
                        e.printStackTrace();
                        log.debug("Registry threw communication exception");
                    }

                } else if (aamResponse.getRegistrationStatus() == ManagementStatus.PLATFORM_EXISTS) {
                    model.addFlashAttribute("platformRegistrationError", "AAM says that the Platform exists!");
                    model.addFlashAttribute("aamPlatformRegistrationError", "AAM says that the Platform exists!");
                    model.addFlashAttribute("activeTab", "platform_details");
                    model.addFlashAttribute("insertedPlatformDetails", platformDetails);
                } else {
                    model.addFlashAttribute("platformRegistrationError", "AAM says that there was an ERROR");
                    model.addFlashAttribute("aamPlatformRegistrationError", "AAM says that there was an ERROR");
                    model.addFlashAttribute("activeTab", "platform_details");
                    model.addFlashAttribute("insertedPlatformDetails", platformDetails);
                }
            } else {
                log.debug("AAM unreachable!");
                model.addFlashAttribute("platformRegistrationError", "AAM unreacheable");
                model.addFlashAttribute("aamPlatformRegistrationError", "AAM unreacheable");
                model.addFlashAttribute("activeTab", "platform_details");
                model.addFlashAttribute("insertedPlatformDetails", platformDetails);
            }
        } catch (CommunicationException e) {
            e.printStackTrace();
            log.debug("AAM threw communication exception");
        }
        model.addFlashAttribute("activeTab", "platform_details");
        return "redirect:/user/cpanel";  
    }

    @PostMapping("/user/cpanel/modify")
    public String modifyPlatform(
        @Valid PlatformDetails platformDetails, BindingResult bindingResult, RedirectAttributes model, Principal principal) {

        log.debug("POST request on /user/cpanel/modify");

        if (bindingResult.hasErrors()) {

            List<FieldError> errors = bindingResult.getFieldErrors();
            String errorMessage = "";
            for (FieldError fieldError : errors) {
                errorMessage = fieldError.getDefaultMessage();
                model.addFlashAttribute("pl_mod_error_" + fieldError.getField(), errorMessage.substring(0, 1).toUpperCase() + errorMessage.substring(1));
            }
            model.addFlashAttribute("page", "modify");
            return "redirect:/user/cpanel";  
        }
        return "redirect:/user/cpanel";
    }

    @PostMapping("/user/cpanel/disable")
    public String disablePlatform(RedirectAttributes model, Principal principal) {

        log.debug("POST request on /user/cpanel/disable");


        return "redirect:/user/cpanel";  
    }

    @PostMapping("/user/cpanel/register_info_model")
    public String registerInformationModel(@Valid @ModelAttribute("informationModel") InformationModel informationModel,
                                           BindingResult bindingResult, RedirectAttributes model, Principal principal) {

        log.debug("POST request on /user/cpanel/register_info_model");

        if (bindingResult.hasErrors()) {

            List<FieldError> errors = bindingResult.getFieldErrors();
            String errorMessage = "";
            for (FieldError fieldError : errors) {
                errorMessage = fieldError.getDefaultMessage();
                log.debug(fieldError.getField() + ": " + errorMessage);
                model.addFlashAttribute("error_"+fieldError.getField(), errorMessage.substring(0, 1).toUpperCase() + errorMessage.substring(1));
            }

            model.addFlashAttribute("activeTab", "information_models");
            return "redirect:/user/cpanel";
        }

        log.debug(ReflectionToStringBuilder.toString(informationModel));

        // if form is valid, construct the request

        model.addFlashAttribute("activeTab", "information_models");
        return "redirect:/user/cpanel";
    }

    @PostMapping("/user/cpanel/create_federation")
    public String createFederation(RedirectAttributes model, Principal principal) {

        String username = principal.getName(); //get logged in username
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken)principal;
        CoreUser user = (CoreUser)token.getPrincipal();

        // Create a Federation object and send it to the Federation Manager

        Federation federation = new Federation();

        // Send update to Federation Manager
        // try{
        //     FederationResponse response = rabbitManager.sendFederationRequest(federation);

        //     if(response != null && response.getStatus() == 200 ){

        //         user.setFederation(federation);

        //     } else {
        //         model.addFlashAttribute("error","Authorization Manager is unreachable!");
        //     }
        
        // } catch(CommunicationException e){

        //         model.addFlashAttribute("error",e.getMessage());
        // }

        return "redirect:/user/cpanel";  
    }

    // ADMIN

    @RequestMapping("/admin/cpanel")
    public String adminCPanel(Model model) {
        log.debug("request on /admin/cpanel");

        model.addAttribute("role", "admin");
        return "/user/cpanel";
    }


    /**
     * Used for testing
     */
    public void setRabbitManager(RabbitManager rabbitManager){
        this.rabbitManager = rabbitManager;
    }

    @ModelAttribute("platformDetails")
    public PlatformDetails getEmptyPlatformDetails() {
        return new PlatformDetails();
    }

    @ModelAttribute("informationModel")
    public InformationModel getEmptyInformationModel() {
        return new InformationModel();
    }
}