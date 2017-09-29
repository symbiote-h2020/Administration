package eu.h2020.symbiote.administration.controllers;

import java.io.IOException;
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
import eu.h2020.symbiote.core.model.RDFFormat;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import eu.h2020.symbiote.security.commons.enums.OperationType;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.communication.payloads.*;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.commons.validator.routines.UrlValidator;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.security.Principal;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;

/**
 * Spring controller for the User control panel, handles management views and form validation.
 *
 * @author Vasileios Glykantzis (ICOM)
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
    public ResponseEntity<ListUserPlatformsResponse> listUserPlatforms(Principal principal) {

        log.debug("POST request on /user/cpanel/list_user_platforms");

        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();
        ArrayList<String> unavailablePlatforms = new ArrayList<>();
        ArrayList<Platform> availablePlatforms = new ArrayList<>();
        ListUserPlatformsResponse response = new ListUserPlatformsResponse();

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
                for (OwnedPlatformDetails platformDetails : ownedPlatformDetailsSet) {
                    log.debug("OwnedPlatformDetails: " + ReflectionToStringBuilder.toString(platformDetails));

                    // Get Platform information from Registry
                    try {
                        PlatformRegistryResponse registryResponse = rabbitManager.sendGetPlatformDetailsMessage(
                                platformDetails.getPlatformInstanceId());
                        if (registryResponse != null) {
                            if (registryResponse.getStatus() != HttpStatus.OK.value()) {
                                log.debug(registryResponse.getMessage());
                                unavailablePlatforms.add(platformDetails.getPlatformInstanceFriendlyName());
                            } else {
                                availablePlatforms.add(registryResponse.getBody());
                            }
                        } else {
                            String message = "Registry unreachable!";
                            log.info(message);
                            response.setMessage(message);
                            return new ResponseEntity<>(response,
                                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    } catch (CommunicationException e) {
                        String message = "Registry threw CommunicationException";

                        log.info(message, e);
                        response.setMessage(message + ": " + e.getMessage());
                        return new ResponseEntity<>(response,
                                new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }

                ArrayList<PlatformDetails> availablePlatformDetails = new ArrayList<>();
                for (Platform platform : availablePlatforms) {
                    PlatformDetails platformDetails = new PlatformDetails(platform);
                    availablePlatformDetails.add(platformDetails);
                }
                response.setAvailablePlatforms(availablePlatformDetails);

                if (unavailablePlatforms.size() == 0) {
                    String message = "All the owned platform details were successfully received";
                    log.debug(message);
                    response.setMessage(message);
                    return new ResponseEntity<>(response, new HttpHeaders(), HttpStatus.OK);
                }
                else {
                    String message = "Could NOT retrieve information from Registry for the following platform that you own:";
                    StringBuilder stringBuilder = new StringBuilder(message);

                    for (String unavailablePlatform : unavailablePlatforms) {
                        stringBuilder.append(" ").append(unavailablePlatform).append(",");

                    }

                    message = stringBuilder.toString();
                    message = message.substring(0, message.length() - 1);
                    log.debug(message);

                    response.setMessage(message);
                    return new ResponseEntity<>(response, new HttpHeaders(), HttpStatus.PARTIAL_CONTENT);
                }
            } else {
                String message = "AAM responded with null";
                log.info(message);
                response.setMessage(message);
                return new ResponseEntity<>(response,
                        new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (CommunicationException e) {
            String message = "AAM threw CommunicationException";
            log.info(message, e);
            response.setMessage(message + ": " + e.getMessage());
            return new ResponseEntity<>(response,
                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }

    @PostMapping("/user/cpanel/register_platform")
    public ResponseEntity<?> registerPlatform(@Valid @RequestBody PlatformDetails platformDetails,
                                              BindingResult bindingResult, Principal principal) {

        log.debug("POST request on /user/cpanel/register_platform");
        boolean invalidInfoModel = false;
        List<String> validInfoModelIds = new ArrayList<>();
        Map<String, Object> responseBody = new HashMap<>();

        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();
        String password = (String) token.getCredentials();

        log.debug("User state is: " + ReflectionToStringBuilder.toString(user));
        log.debug(platformDetails.toString());



        ResponseEntity<?> listOfInformationModels = getInformationModels();

        if (listOfInformationModels.getStatusCode() != HttpStatus.OK) {
            log.debug("Could not get information models from Registry");
            return new ResponseEntity<>(listOfInformationModels.getBody(), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            for (InformationModel informationModel: (List<InformationModel>) listOfInformationModels.getBody()) {
                validInfoModelIds.add(informationModel.getId());
            }
        }

        for (InterworkingService service : platformDetails.getInterworkingServices()) {
            if (!validInfoModelIds.contains(service.getInformationModelId())) {
                log.debug("The information model id is not valid");
                invalidInfoModel = true;
            }
        }

        if (bindingResult.hasErrors() || invalidInfoModel) {

            List<FieldError> errors = bindingResult.getFieldErrors();
            for (FieldError fieldError : errors) {
                String errorField;
                String errorMessage = fieldError.getDefaultMessage();
                String[] parts = fieldError.getField().split("\\[");

                if (parts.length > 1){

                    int errorFieldIndex = Integer.parseInt(parts[1].split("]")[0]);
                    log.debug("errorFieldIndex = " + errorFieldIndex);
                    errorField = "pl_reg_error_" + parts[0] + parts[1].replace(".", "_").split("]")[1];
                    ArrayList<String> errorList;

                    if (responseBody.get(errorField) == null) {
                        errorList = new ArrayList<>();
                        for (int i = 0; i < errorFieldIndex; i++)
                            errorList.add(null);
                        errorList.add(errorMessage);
                    } else {
                        errorList = (ArrayList<String>) responseBody.get(errorField);
                        for (int i = errorList.size(); i < errorFieldIndex; i++)
                            errorList.add(null);
                        errorList.add(errorMessage);
                    }

                    responseBody.put(errorField, errorList);
                    log.debug(responseBody);
                }
                else {
                    errorField = "pl_reg_error_" + fieldError.getField();
                    responseBody.put(errorField, errorMessage);
                }
                log.debug(errorField + ": " + errorMessage);

            }

            responseBody.put("platformRegistrationError", "Invalid Arguments");

            return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST);
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
                    registryRequest.setId(aamResponse.getPlatformId()); // To take into account the empty id
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

                    try {
                        PlatformRegistryResponse registryResponse = rabbitManager.sendPlatformCreationRequest(registryRequest);
                        if (registryResponse != null) {
                            if (registryResponse.getStatus() == HttpStatus.OK.value()) {
                                // Platform registered successfully
                                log.debug("Platform registered successfully!");
                                responseBody.put("platform-registration-success", "Successful Registration!");
                                return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.CREATED);

                            } else {
                                log.debug("Registration Failed: " + registryResponse.getMessage());

                                sendPlatformDeleteMessageToAAM(aamRequest);

                                responseBody.put("platformRegistrationError", registryResponse.getMessage());
                                return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.valueOf(registryResponse.getStatus()));
                            }
                        } else {
                            log.debug("Registry unreachable!");

                            sendPlatformDeleteMessageToAAM(aamRequest);

                            responseBody.put("platformRegistrationError", "Registry unreachable!");
                            return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    } catch (CommunicationException e) {
                        e.printStackTrace();
                        log.debug("Registry threw communication exception: " + e.getMessage());

                        sendPlatformDeleteMessageToAAM(aamRequest);

                        responseBody.put("platformRegistrationError", "Registry threw CommunicationException");
                        return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
                    }

                } else if (aamResponse.getRegistrationStatus() == ManagementStatus.PLATFORM_EXISTS) {
                    log.debug("AAM says that the Platform exists!");
                    responseBody.put("platformRegistrationError", "AAM says that the Platform exists!");
                    return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST);
                } else {
                    log.debug("AAM says that there was an ERROR");
                    responseBody.put("platformRegistrationError", "AAM says that there was an ERROR");
                    return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST);
                }
            } else {
                log.debug("AAM unreachable!");
                responseBody.put("platformRegistrationError", "AAM unreachable!");
                return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (CommunicationException e) {
            e.printStackTrace();
            String message = "AAM threw CommunicationException: " + e.getMessage();
            log.debug(message);
            responseBody.put("platformRegistrationError", message);
            return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

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
    public ResponseEntity<?> deletePlatforms(@RequestParam String platformIdToDelete, Principal principal) {

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
                        String message = "The user owns the platform with id " + platformIdToDelete + " which tried to delete.";
                        log.info(message);
                        ownsPlatform = true;
                        break;
                    }
                }

                if (!ownsPlatform) {
                    String message = "You do not own the platform that you tried to delete";
                    log.info(message);
                    return new ResponseEntity<>("You do not own the platform that you tried to delete",
                            new HttpHeaders(), HttpStatus.BAD_REQUEST);
                }
            } else {
                String message = "AAM unreachable";
                log.info(message);
                return new ResponseEntity<>("AAM unreachable",
                        new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (CommunicationException e) {
            String message = "AAM threw communication exception";
            log.info(message, e);
            return new ResponseEntity<>(message + ": " + e.getMessage(),
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
                String message = "Registry unreachable!";
                log.info(message);
                // Send deletion message to AAM
                return new ResponseEntity<>(message,
                        new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (CommunicationException e) {
            String message = "Registry threw communication exception";
            log.info(message, e);
            return new ResponseEntity<>(message + ": " + e.getMessage(),
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
                    String message = "AAM says that the Platform does not exist!";
                    log.info(message);
                    return new ResponseEntity<>(message,
                            new HttpHeaders(), HttpStatus.BAD_REQUEST);
                }
            } else {
                String message = "AAM unreachable!";
                log.info(message);
                return new ResponseEntity<>(message,
                        new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (CommunicationException e) {
            String message = "AAM threw communication exception: " + e.getMessage();
            log.info(message, e);
            return new ResponseEntity<>(message,
                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(new HttpHeaders(), HttpStatus.OK);
    }


    @PostMapping("/user/cpanel/list_all_info_models")
    public ResponseEntity<?> listAllInformationModels() {

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
    public ResponseEntity<?> registerInformationModel(@RequestParam("info-model-name") String name,
                                                      @RequestParam("info-model-uri") String uri,
                                                      @RequestParam("info-model-rdf") MultipartFile rdfFile,
                                                      Principal principal) {

        log.debug("POST request on /user/cpanel/register_information_model");

        UrlValidator urlValidator = new UrlValidator();

        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();
        Map<String, String> response = new HashMap<>();

        log.debug("User state is: " + ReflectionToStringBuilder.toString(user));

        if (name.length() < 2 || name.length() > 30)
            response.put("info_model_reg_error_name", "The name should have from 2 to 30 characters");
        if (!urlValidator.isValid(uri))
            response.put("info_model_reg_error_uri", "The uri is invalid");
        if (!rdfFile.getOriginalFilename().matches("^[\\w]+\\.(ttl|nt|rdf|xml|n3|jsonld)$"))
            response.put("info_model_reg_error_rdf", "This format is not supported");

        if (response.size() > 0) {
            response.put("error", "Invalid Arguments");
            return new ResponseEntity<>(response, new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }


        try {
            InformationModel informationModel = new InformationModel();
            informationModel.setName(name);
            informationModel.setOwner(user.getUsername());
            informationModel.setUri(uri);
            informationModel.setRdf(new String(rdfFile.getBytes(), "UTF-8"));

            String[] parts = rdfFile.getOriginalFilename().split("\\.");
            informationModel.setRdfFormat(RDFFormat.fromFilenameExtension(parts[parts.length-1]));


            InformationModelRequest request = new InformationModelRequest();
            request.setBody(informationModel);

            InformationModelResponse registryResponse = rabbitManager.sendRegisterInfoModelRequest(request);
            if (registryResponse != null) {
                if (registryResponse.getStatus() != HttpStatus.OK.value()) {
                    String message = "Registry responded with: " + registryResponse.getStatus();
                    log.info(message);
                    response.put("error", registryResponse.getMessage());
                    return new ResponseEntity<>(response,
                            new HttpHeaders(), HttpStatus.valueOf(registryResponse.getStatus()));
                }
            } else {
                String message = "Registry unreachable!";
                log.info(message);
                response.put("error", message);
                return new ResponseEntity<>(response,
                        new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (CommunicationException e) {
            String message = "Registry threw communication exception: " + e.getMessage();;
            log.info(message);
            response.put("error", message);
            return new ResponseEntity<>(response,
                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IOException e) {
            String message = "Could not read the rdfFile";
            log.info(message);
            response.put("error", message);
            return new ResponseEntity<>(response,
                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(new HttpHeaders(), HttpStatus.CREATED);
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

            for (InformationModel informationModel : (List<InformationModel>)responseEntity.getBody()) {
                log.debug(informationModel.getId() + " " + informationModel.getOwner());
                if (informationModel.getId().equals(infoModelIdToDelete) &&
                        informationModel.getOwner().equals(user.getUsername())) {

                    // Ask Registry
                    try {
                        InformationModelRequest request = new InformationModelRequest();
                        request.setBody(informationModel);

                        InformationModelResponse response = rabbitManager.sendDeleteInfoModelRequest(request);
                        if (response != null) {
                            if (response.getStatus() != HttpStatus.OK.value()) {

                                return new ResponseEntity<>(response.getMessage(),
                                        new HttpHeaders(), HttpStatus.valueOf(response.getStatus()));
                            }
                        } else {
                            log.debug("Registry unreachable!");
                            return new ResponseEntity<>("Registry unreachable!",
                                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    } catch (CommunicationException e) {
                        e.printStackTrace();
                        String message = "Registry threw communication exception: " + e.getMessage();
                        log.debug(message);
                        return new ResponseEntity<>(message, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
                    }

                    return new ResponseEntity<>(new HttpHeaders(), HttpStatus.OK);
                }
            }

            return new ResponseEntity<>("You do not own the Information Model that you tried to delete",
                    new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }

    }

    @PostMapping("/user/cpanel/create_federation")
    public ResponseEntity<?> createFederation(@Valid @RequestBody CreateFederationRequest createFederationRequest,
                                              BindingResult bindingResult, Principal principal) {

        String username = principal.getName(); //get logged in username
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken)principal;
        CoreUser user = (CoreUser)token.getPrincipal();
        Map<String, Object> responseBody = new HashMap<>();

        if (bindingResult.hasErrors()) {

            List<FieldError> errors = bindingResult.getFieldErrors();
            for (FieldError fieldError : errors) {
                String errorMessage = fieldError.getDefaultMessage();

                String errorField = "federation_reg_error_" + fieldError.getField();
                responseBody.put(errorField, errorMessage);

                log.debug(errorField + ": " + errorMessage);

            }

            responseBody.put("error", "Invalid Arguments");
            return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }

        Set<String> platformIds = new HashSet<>();
        platformIds.add(createFederationRequest.getPlatform1Id());
        platformIds.add(createFederationRequest.getPlatform2Id());
        FederationRuleManagementRequest request = new FederationRuleManagementRequest(
                new Credentials(aaMOwnerUsername, aaMOwnerPassword),
                createFederationRequest.getId(),
                platformIds,
                FederationRuleManagementRequest.OperationType.CREATE
        );

        try {
            Map<String, FederationRule> aamResponse =
                    rabbitManager.sendFederationRuleManagementRequest(request);

            if (aamResponse != null) {
                if(aamResponse.size() == 1) {
                    Map.Entry<String, FederationRule> entry = aamResponse.entrySet().iterator().next();

                    if (entry.getValue().containPlatform(createFederationRequest.getPlatform1Id()) &&
                            entry.getValue().containPlatform(createFederationRequest.getPlatform2Id())) {
                        responseBody.put("message", "Federation Registration was successful1");
                        return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.CREATED);

                    } else {
                        responseBody.put("error", "Not both platforms ids present in AAM response");
                        return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST);
                    }
                } else {
                    String message = "Contains more than 1 Federation rule";
                    log.debug(message);
                    responseBody.put("error", message);
                    return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST);
                }
            } else {
                String message = "AAM unreachable";
                log.debug(message);
                responseBody.put("error", message);
                return new ResponseEntity<>(responseBody,
                        new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (CommunicationException e) {
            e.printStackTrace();
            String message = "AAM threw communication exception: " + e.getMessage();
            log.debug(message);
            responseBody.put("error", message);
            return new ResponseEntity<>(responseBody,
                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @PostMapping("/user/cpanel/list_federations")
    public ResponseEntity<?> listFederations(Principal principal) {

        log.debug("POST request on /user/cpanel/list_federations");

        String username = principal.getName(); //get logged in username
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken)principal;
        CoreUser user = (CoreUser)token.getPrincipal();
        Map<String, Object> responseBody = new HashMap<>();

        Set<String> platformIds = new HashSet<>();

        FederationRuleManagementRequest request = new FederationRuleManagementRequest(
                new Credentials(aaMOwnerUsername, aaMOwnerPassword),
                "",
                platformIds,
                FederationRuleManagementRequest.OperationType.READ
        );

        try {
            Map<String, FederationRule> aamResponse =
                    rabbitManager.sendFederationRuleManagementRequest(request);

            if (aamResponse != null) {
                return new ResponseEntity<>(aamResponse, new HttpHeaders(), HttpStatus.OK);

            } else {
                String message = "AAM unreachable during ListFederationRequest";
                log.debug(message);
                responseBody.put("error", message);
                return new ResponseEntity<>(responseBody,
                        new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (CommunicationException e) {
            e.printStackTrace();
            String message = "AAM threw communication exception during ListFederationRequest: " + e.getMessage();
            log.debug(message);
            responseBody.put("error", message);
            return new ResponseEntity<>(responseBody,
                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }


    @PostMapping("/user/cpanel/delete_federation")
    public ResponseEntity<?> deleteFederation(@RequestParam String federationIdToDelete,
                                              Principal principal) {

        log.debug("POST request on /user/cpanel/delete_federation for federation with id = " + federationIdToDelete);

        Map<String, Object> responseBody = new HashMap<>();

        FederationRuleManagementRequest request = new FederationRuleManagementRequest(
                new Credentials(aaMOwnerUsername, aaMOwnerPassword),
                federationIdToDelete,
                new HashSet<>(),
                FederationRuleManagementRequest.OperationType.DELETE
        );

        try {
            Map<String, FederationRule> aamResponse =
                    rabbitManager.sendFederationRuleManagementRequest(request);

            if (aamResponse != null) {
                return new ResponseEntity<>(aamResponse, new HttpHeaders(), HttpStatus.OK);

            } else {
                String message = "AAM unreachable during DeleteFederationRequest";
                log.debug(message);
                responseBody.put("error", message);
                return new ResponseEntity<>(responseBody,
                        new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (CommunicationException e) {
            e.printStackTrace();
            String message = "AAM threw communication exception during DeleteFederationRequest: " + e.getMessage();
            log.debug(message);
            responseBody.put("error", message);
            return new ResponseEntity<>(responseBody,
                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
                return new ResponseEntity<>(informationModelListResponse.getBody(),
                        new HttpHeaders(), HttpStatus.OK);

            } else {
                if (informationModelListResponse != null)
                    return new ResponseEntity<>(informationModelListResponse.getMessage(),
                            new HttpHeaders(), HttpStatus.valueOf(informationModelListResponse.getStatus()));
                else
                    return new ResponseEntity<>("Could not retrieve the information models from registry",
                            new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);

            }
        } catch (CommunicationException e) {
            e.printStackTrace();
            return new ResponseEntity<>("Communication exception while retrieving the information models: " +
                    e.getMessage(), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }

    private void sendPlatformDeleteMessageToAAM(PlatformManagementRequest aamRequest) {

        // Send deletion message to AAM
        aamRequest.setOperationType(OperationType.DELETE);
        try {
            PlatformManagementResponse aamResponse = rabbitManager.sendManagePlatformRequest(aamRequest);

            // Todo: Check what happens when platform deletion request is not successful at this stage
            if (aamResponse != null) {
                if (aamResponse.getRegistrationStatus() == ManagementStatus.OK) {
                    log.info("Platform was removed from AAM");
                } else {
                    log.info("Platform was NOT removed from AAM");
                }
            } else {
                log.info("AAM unreachable during platform deletion request");
            }
        } catch (CommunicationException e) {
            e.printStackTrace();
        }
    }
}