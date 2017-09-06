package eu.h2020.symbiote.administration.controllers;

import java.util.*;

import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.administration.model.*;
import eu.h2020.symbiote.core.cci.InformationModelRequest;
import eu.h2020.symbiote.core.cci.InformationModelResponse;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.security.Principal;

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

        return "controlpanel";
    }

    @PostMapping("/user/cpanel/list_user_platforms")
    public ResponseEntity<?> listUserPlatforms(Model model, Principal principal) {

        log.debug("POST request on /user/cpanel/list_user_platforms");

        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();

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
                return new ResponseEntity<>(ownedPlatformDetailsSet, new HttpHeaders(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>("AAM responded with null",
                        new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (CommunicationException e) {
            e.printStackTrace();
            return new ResponseEntity<>("Communication exception when tried to get the owned platform details",
                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }

    @PostMapping("/user/cpanel/register_platform")
    public String registerPlatform(@Valid @ModelAttribute("platformDetails") PlatformDetails platformDetails,
                                   BindingResult bindingResult, RedirectAttributes model, Principal principal) {

        log.debug("POST request on /user/cpanel/register_platform");

        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();
        String password = (String) token.getCredentials();

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
            model.addFlashAttribute("activeTab", "platform-details");
            model.addFlashAttribute("insertedPlatformDetails", platformDetails);
            return "redirect:/user/cpanel";  
        }


        // If form is valid, construct the PlatformManagementResponse to the AAM
        PlatformManagementRequest aamRequest = new PlatformManagementRequest(
                new Credentials(aaMOwnerUsername, aaMOwnerPassword), new Credentials(user.getUsername(), password),
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

                    // FIll in the labels. The first label is the platform name
                    ArrayList<String> labels = new ArrayList<>();
                    labels.add(platformDetails.getName());
                    for (Label label : platformDetails.getLabels())
                        labels.add(label.getLabel());
                    registryRequest.setLabels(labels);

                    // FIll in the comments. The first comment is the platform description
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
                                model.addFlashAttribute("activeTab", "platform-details");

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
                                model.addFlashAttribute("insertedPlatformDetails", platformDetails);
                            }
                        } else {
                            log.debug("Registry unreachable!");
                            // Send deletion message to AAM
                            model.addFlashAttribute("platformRegistrationError", "Registry unreacheable");
                            model.addFlashAttribute("registryPlatformRegistrationError", "Registry unreacheable");
                            model.addFlashAttribute("insertedPlatformDetails", platformDetails);
                        }
                    } catch (CommunicationException e) {
                        e.printStackTrace();
                        log.debug("Registry threw communication exception");
                        model.addFlashAttribute("platformRegistrationError", "Registry unreacheable");
                        model.addFlashAttribute("registryPlatformRegistrationError", "Registry unreacheable");
                        model.addFlashAttribute("insertedPlatformDetails", platformDetails);
                    }

                } else if (aamResponse.getRegistrationStatus() == ManagementStatus.PLATFORM_EXISTS) {
                    model.addFlashAttribute("platformRegistrationError", "AAM says that the Platform exists!");
                    model.addFlashAttribute("aamPlatformRegistrationError", "AAM says that the Platform exists!");
                    model.addFlashAttribute("insertedPlatformDetails", platformDetails);
                } else {
                    model.addFlashAttribute("platformRegistrationError", "AAM says that there was an ERROR");
                    model.addFlashAttribute("aamPlatformRegistrationError", "AAM says that there was an ERROR");
                    model.addFlashAttribute("insertedPlatformDetails", platformDetails);
                }
            } else {
                log.debug("AAM unreachable!");
                model.addFlashAttribute("platformRegistrationError", "AAM unreacheable");
                model.addFlashAttribute("aamPlatformRegistrationError", "AAM unreacheable");
                model.addFlashAttribute("insertedPlatformDetails", platformDetails);
            }
        } catch (CommunicationException e) {
            e.printStackTrace();
            log.debug("AAM threw communication exception");
            model.addFlashAttribute("platformRegistrationError", "AAM unreacheable");
            model.addFlashAttribute("aamPlatformRegistrationError", "AAM unreacheable");
            model.addFlashAttribute("insertedPlatformDetails", platformDetails);
        }

        model.addFlashAttribute("activeTab", "platform-details");
        return "redirect:/user/cpanel";  
    }

    @PostMapping("/user/cpanel/modify_platform")
    public String modifyPlatform(@Valid PlatformDetails platformDetails, BindingResult bindingResult,
                                 RedirectAttributes model, Principal principal) {

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

    @PostMapping("/user/cpanel/delete_platform")
    public ResponseEntity<?> deletePlatforms(@RequestParam String platformIdToDelete, RedirectAttributes model, Principal principal) {

        log.debug("POST request on /user/cpanel/delete_platform for platform with id: " +
                platformIdToDelete);

        // Checking if the user owns the platform
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();
        String password = (String) token.getCredentials();

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

        try {
            Set<OwnedPlatformDetails> ownedPlatformDetailsSet =
                    rabbitManager.sendOwnedPlatformDetailsRequest(ownedPlatformDetailsRequest);
            if (ownedPlatformDetailsSet != null) {
                boolean ownsPlatform = false;
                for (OwnedPlatformDetails platformDetails : ownedPlatformDetailsSet) {
                    log.debug(platformDetails);
                    if (platformDetails.getPlatformInstanceId().equals(platformIdToDelete)) {
                        log.debug("The user owns the platform with " + platformIdToDelete + " which tried to delete.");
                        ownsPlatform = true;
                        break;
                    }
                }

                if (!ownsPlatform) {
                    log.debug("You do not own the platform you tried to delete");
                    return new ResponseEntity<>("You do not own the platform you tried to delete",
                            new HttpHeaders(), HttpStatus.BAD_REQUEST);
                }
            } else {
                log.debug("AAM unreachable");
                return new ResponseEntity<>("AAM unreachable",
                        new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (CommunicationException e) {
            e.printStackTrace();
            log.debug("AAM threw communication exception");
            return new ResponseEntity<>("AAM threw communication exception",
                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Check with Registry
        try {
            Platform registryRequest = new Platform();
            registryRequest.setId(platformIdToDelete);

            PlatformRegistryResponse registryResponse = rabbitManager.sendPlatformRemovalRequest(registryRequest);
            if (registryResponse != null) {
                if (registryResponse.getStatus() != HttpStatus.OK.value()) {
                    log.debug(registryResponse.getMessage());
                    return new ResponseEntity<>(registryResponse.getMessage(),
                            new HttpHeaders(), HttpStatus.valueOf(registryResponse.getStatus()));
                }
            } else {
                log.debug("Registry unreachable!");
                // Send deletion message to AAM
                return new ResponseEntity<>("Registry unreachable!",
                        new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (CommunicationException e) {
            e.printStackTrace();
            log.debug("Registry threw communication exception");
            return new ResponseEntity<>("Registry threw communication exception",
                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Check with AAM
        PlatformManagementRequest aamRequest = new PlatformManagementRequest(
                new Credentials(aaMOwnerUsername, aaMOwnerPassword), new Credentials(user.getUsername(), password),
                "", "", platformIdToDelete, OperationType.DELETE);
        try {
            PlatformManagementResponse aamResponse = rabbitManager.sendManagePlatformRequest(aamRequest);
            if(aamResponse != null) {
                log.debug("AAM responded with: " + aamResponse.getRegistrationStatus());

                if (aamResponse.getRegistrationStatus() != ManagementStatus.OK) {
                    log.debug("AAM says that the Platform does not exist!");
                    return new ResponseEntity<>("AAM says that the Platform does not exist!",
                            new HttpHeaders(), HttpStatus.BAD_REQUEST);
                }
            } else {
                log.debug("AAM unreachable!");
                return new ResponseEntity<>("AAM unreachable",
                        new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (CommunicationException e) {
            e.printStackTrace();
            log.debug("AAM threw communication exception");
            return new ResponseEntity<>("AAM threw communication exception",
                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(new HttpHeaders(), HttpStatus.OK);
    }


    @PostMapping("/user/cpanel/list_all_info_models")
    public ResponseEntity<?> listAllInformationModels(Principal principal) {

        log.debug("POST request on /user/cpanel/list_all_info_models");

        // Get InformationModelList from Registry
        return getInformationModels();

    }

    @PostMapping("/user/cpanel/list_user_info_models")
    public ResponseEntity<?> listUserInformationModels(Principal principal) {

        log.debug("POST request on /user/cpanel/list_user_info_models");

        // Checking if the user owns the platform
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();

        // Get InformationModelList from Registry
        ResponseEntity<?> responseEntity = getInformationModels();
        if (responseEntity.getStatusCode() != HttpStatus.OK)
            return responseEntity;
        else {
            ArrayList<InformationModel> userInfoModels = new ArrayList<>();

            for (InformationModel informationModel : (List<InformationModel>)responseEntity.getBody()) {
                if (informationModel.getOwner().equals(user.getUsername()))
                    userInfoModels.add(informationModel);
            }
            return new ResponseEntity<>(userInfoModels, new HttpHeaders(), HttpStatus.OK);
        }
    }

    @PostMapping("/user/cpanel/register_information_model")
    public ResponseEntity<InformationModel> registerInformationModel(@Valid @RequestBody InformationModel informationModel,
                                                                     BindingResult bindingResult, Principal principal) {

        log.debug("POST request on /user/cpanel/register_information_model");

        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();

        log.debug("User state is: " + ReflectionToStringBuilder.toString(user));

        if (bindingResult.hasErrors()) {

            List<FieldError> errors = bindingResult.getFieldErrors();
            String errorMessage = "";
            for (FieldError fieldError : errors) {
                errorMessage = fieldError.getDefaultMessage();
                log.debug(fieldError.getField() + ": " + errorMessage);
            }

        }

        log.debug(ReflectionToStringBuilder.toString(informationModel));

        // if form is valid, construct the request
        InformationModel response = new InformationModel();
        response.setId("Works!");
        return new ResponseEntity<>(response, new HttpHeaders(), HttpStatus.CREATED);
    }

    @PostMapping("/user/cpanel/delete_information_model")
    public ResponseEntity<?> deleteInformationModel(@RequestParam String infoModelIdToDelete,
                                                    Principal principal) {

        log.debug("POST request on /user/cpanel/delete_information_model for info model with id = " + infoModelIdToDelete);

        // Checking if the user owns the information model
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();

        // Get InformationModelList from Registry
        ResponseEntity<?> responseEntity = getInformationModels();
        if (responseEntity.getStatusCode() != HttpStatus.OK)
            return responseEntity;
        else {
            ArrayList<InformationModel> userInfoModels = new ArrayList<>();

            for (InformationModel informationModel : (List<InformationModel>)responseEntity.getBody()) {
                log.debug(informationModel.getId() + " " + informationModel.getOwner());
                if (informationModel.getId().equals(infoModelIdToDelete) &&
                        informationModel.getOwner().equals(user.getUsername())) {

                    // Ask Registry
                    try {
                        InformationModelRequest request = new InformationModelRequest();
                        request.setInformationModel(informationModel);

                        InformationModelResponse response = rabbitManager.sendDeleteInfoModelRequest(request);
                        if (response != null) {
                            if (response.getStatus() != HttpStatus.OK.value()) {

                                return new ResponseEntity<>(response.getMessage(),
                                        new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
                            }
                        } else {
                            log.debug("Registry unreachable!");
                            return new ResponseEntity<>("Registry unreachable!",
                                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);                        }
                    } catch (CommunicationException e) {
                        e.printStackTrace();
                        log.debug("Registry threw communication exception");
                        return new ResponseEntity<>("Registry threw communication exception",
                                new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
                    }

                    return new ResponseEntity<>(new HttpHeaders(), HttpStatus.OK);
                }
            }

            return new ResponseEntity<>("You do not own the platform you tried to delete",
                    new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }

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
    public PlatformDetails getEmptyPlatformDetails() { return new PlatformDetails(); }

    @ModelAttribute("informationModel")
    public InformationModel getEmptyInformationModel() { return new InformationModel(); }

    private ResponseEntity<?> getInformationModels() {
        try {
            InformationModelListResponse informationModelListResponse = rabbitManager.sendListInfoModelsRequest();
            if (informationModelListResponse != null && informationModelListResponse.getStatus() == HttpStatus.OK.value()) {
                return new ResponseEntity<>(informationModelListResponse.getInformationModels(),
                        new HttpHeaders(), HttpStatus.OK);

            } else {
                if (informationModelListResponse != null)
                    return new ResponseEntity<>(informationModelListResponse.getMessage(),
                            new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
                else
                    return new ResponseEntity<>("Could not retrieve the information models from registry",
                            new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);

            }
        } catch (CommunicationException e) {
            e.printStackTrace();
            return new ResponseEntity<>("Communication exception while retrieving the information models",
                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }
}