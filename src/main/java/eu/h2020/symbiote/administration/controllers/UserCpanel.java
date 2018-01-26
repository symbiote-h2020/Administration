package eu.h2020.symbiote.administration.controllers;

import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.administration.model.*;
import eu.h2020.symbiote.administration.services.PlatformService;
import eu.h2020.symbiote.core.cci.InformationModelRequest;
import eu.h2020.symbiote.core.cci.InformationModelResponse;
import eu.h2020.symbiote.core.cci.PlatformRegistryResponse;
import eu.h2020.symbiote.core.internal.InformationModelListResponse;
import eu.h2020.symbiote.core.internal.RDFFormat;
import eu.h2020.symbiote.model.mim.InformationModel;
import eu.h2020.symbiote.model.mim.Platform;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.util.*;

/**
 * Spring controller for the User control panel, handles management views and form validation.
 *
 * @author Vasileios Glykantzis (ICOM)
 * @author Tilemachos Pechlivanoglou (ICOM)
 */
@Controller
@CrossOrigin
public class UserCpanel {
    private static Log log = LogFactory.getLog(UserCpanel.class);

    private RabbitManager rabbitManager;
    private PlatformService platformService;
    private String aaMOwnerUsername;
    private String aaMOwnerPassword;

    @Autowired
    public UserCpanel(RabbitManager rabbitManager, PlatformService platformService,
                      @Value("${aam.deployment.owner.username}") String aaMOwnerUsername,
                      @Value("${aam.deployment.owner.password}") String aaMOwnerPassword) {

        Assert.notNull(rabbitManager,"RabbitManager can not be null!");
        this.rabbitManager = rabbitManager;

        Assert.notNull(platformService,"PlatformService can not be null!");
        this.platformService = platformService;

        Assert.notNull(aaMOwnerUsername,"aaMOwnerUsername can not be null!");
        this.aaMOwnerUsername = aaMOwnerUsername;

        Assert.notNull(aaMOwnerPassword,"aaMOwnerPassword can not be null!");
        this.aaMOwnerPassword = aaMOwnerPassword;
    }


    /**
     * Gets the default view. If the user is a platform owner, tries to fetch their details.
     * Registry is first polled and, if the platform isn't activated there, AAM is polled for them.
     */
    @GetMapping("/administration/user/cpanel")
    public String userCPanel(Model model, Principal principal) {

        log.debug("GET request on /administration/user/cpanel");

        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();

        log.debug("User state is: " + ReflectionToStringBuilder.toString(user));
        model.addAttribute("user", user);

        return "index";
    }

    @GetMapping("/administration/user/information")
    public ResponseEntity<?> getUserInformation(Principal principal) {
        log.debug("GET request on /administration/user/information");

        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();

        UserDetailsDDO userDetails = new UserDetailsDDO(user.getUsername(), user.getRecoveryMail(), user.getRole().toString());
        return new ResponseEntity<>(userDetails, new HttpHeaders(), HttpStatus.OK);
    }

    @PostMapping("/administration/user/change_email")
    public ResponseEntity<?> changeEmail(@Valid @RequestBody ChangeEmailRequest message,
                                         BindingResult bindingResult,
                                         Principal principal) {
        log.debug("POST request on /administration/user/change_email");

        Map<String, String> errorsResponse = new HashMap<>();
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();
        String password = (String) token.getCredentials();

        if (bindingResult.hasErrors()) {

            List<FieldError> errors = bindingResult.getFieldErrors();
            for (FieldError fieldError : errors) {
                String errorMessage = "Enter a valid email";
                String errorField = "error_" + fieldError.getField();
                log.debug(errorField + ": " + errorMessage);
                errorsResponse.put(errorField, errorMessage);
            }
        }

        if (errorsResponse.get("error_newEmailRetyped") == null &&
                !message.getNewEmail().equals(message.getNewEmailRetyped())) {
            String errorField = "error_newEmailRetyped";
            String errorMessage = "The provided emails do not match";
            log.debug(errorField + ": " + errorMessage);
            errorsResponse.put(errorField, errorMessage);

        }

        if (errorsResponse.size() > 0) {
            errorsResponse.put("changeEmailError", "Invalid Arguments");
            return new ResponseEntity<>(errorsResponse, new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }

        // Construct the UserManagementRequest
        // Todo: Change the federatedId in R4

        UserManagementRequest userUpdateRequest = new UserManagementRequest(
                new Credentials(aaMOwnerUsername, aaMOwnerPassword),
                new Credentials(user.getUsername(), password),
                new UserDetails(
                        new Credentials(user.getUsername(), password),
                        "",
                        message.getNewEmail(),
                        user.getRole(),
                        new HashMap<>(),
                        new HashMap<>()
                ),
                OperationType.UPDATE
        );

        Map<String, Object> response = new HashMap<>();

        try {
            ManagementStatus managementStatus = rabbitManager.sendUserManagementRequest(userUpdateRequest);

            if (managementStatus == null) {
                response.put("changeEmailError","Authorization Manager is unreachable!");
                return new ResponseEntity<>(response, new HttpHeaders(),
                        HttpStatus.INTERNAL_SERVER_ERROR);

            } else if(managementStatus == ManagementStatus.OK ){
                return new ResponseEntity<>(response, new HttpHeaders(),
                        HttpStatus.OK);
            }
        } catch (CommunicationException e) {
            response.put("changeEmailError",e.getMessage());
            return  new ResponseEntity<>(response, new HttpHeaders(),
                    HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    @PostMapping("/administration/user/cpanel/list_user_platforms")
    public ResponseEntity<ListUserPlatformsResponse> listUserPlatforms(Principal principal) {

        log.debug("POST request on /administration/user/cpanel/list_user_platforms");

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
                            log.warn(message);
                            response.setMessage(message);
                            return new ResponseEntity<>(response,
                                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    } catch (CommunicationException e) {
                        String message = "Registry threw CommunicationException";

                        log.warn(message, e);
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
                log.warn(message);
                response.setMessage(message);
                return new ResponseEntity<>(response,
                        new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (CommunicationException e) {
            String message = "AAM threw CommunicationException";
            log.warn(message, e);
            response.setMessage(message + ": " + e.getMessage());
            return new ResponseEntity<>(response,
                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }

    @PostMapping("/administration/user/cpanel/register_platform")
    public ResponseEntity<?> registerPlatform(@Valid @RequestBody PlatformDetails platformDetails,
                                              BindingResult bindingResult, Principal principal) {

        log.debug("POST request on /administration/user/cpanel/register_platform");

        return platformService.registerPlatform(platformDetails, bindingResult, principal);
    }


    @PostMapping("/administration/user/cpanel/update_platform")
    public ResponseEntity<?> updatePlatform(@Valid @RequestBody PlatformDetails platformDetails,
                                            BindingResult bindingResult, Principal principal) {

        log.debug("POST request on /administration/user/cpanel/update_platform");
        return platformService.updatePlatform(platformDetails, bindingResult, principal);

    }

    @PostMapping("/administration/user/cpanel/delete_platform")
    public ResponseEntity<?> deletePlatform(@RequestParam String platformIdToDelete, Principal principal) {

        log.debug("POST request on /administration/user/cpanel/delete_platform for platform with id: " +
                platformIdToDelete);
        return platformService.deletePlatform(platformIdToDelete, principal);
    }


    @PostMapping(value = "/administration/user/cpanel/get_platform_config", produces="application/zip")
    public void getPlatformConfig(@Valid @RequestBody PlatformConfigurationMessage configurationMessage,
                                  BindingResult bindingResult, Principal principal,
                                  HttpServletResponse response) throws Exception {

        log.debug("POST request on /administration/user/cpanel/get_platform_config: " + configurationMessage);
        platformService.getPlatformConfig(configurationMessage, bindingResult, principal, response);

    }


    @PostMapping("/administration/user/cpanel/list_all_info_models")
    public ResponseEntity<?> listAllInformationModels() {

        log.debug("POST request on /administration/user/cpanel/list_all_info_models");

        // Get InformationModelList from Registry
        return getInformationModels();

    }

    @PostMapping("/administration/user/cpanel/list_user_info_models")
    public ResponseEntity<?> listUserInformationModels(Principal principal) {

        log.debug("POST request on /administration/user/cpanel/list_user_info_models");

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

    @PostMapping("/administration/user/cpanel/register_information_model")
    public ResponseEntity<?> registerInformationModel(@RequestParam("info-model-name") String name,
                                                      @RequestParam("info-model-uri") String uri,
                                                      @RequestParam("info-model-rdf") MultipartFile rdfFile,
                                                      Principal principal) {

        log.debug("POST request on /administration/user/cpanel/register_information_model");

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

        try {
            log.debug("The size of the file is " + rdfFile.getBytes().length + "bytes");
        } catch (IOException e) {
            log.info("", e);
        }

        if (response.size() > 0) {
            response.put("error", "Invalid Arguments");
            return new ResponseEntity<>(response, new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }

        InformationModelResponse registryResponse;
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

            registryResponse = rabbitManager.sendRegisterInfoModelRequest(request);
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
                log.warn(message);
                response.put("error", message);
                return new ResponseEntity<>(response,
                        new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (CommunicationException e) {
            String message = "Registry threw communication exception: " + e.getMessage();
            log.warn(message);
            response.put("error", message);
            return new ResponseEntity<>(response,
                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IOException e) {
            String message = "Could not read the rdfFile";
            log.warn(message);
            response.put("error", message);
            return new ResponseEntity<>(response,
                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(registryResponse.getBody(), new HttpHeaders(), HttpStatus.CREATED);
    }


    @PostMapping("/administration/user/cpanel/delete_information_model")
    public ResponseEntity<?> deleteInformationModel(@RequestParam String infoModelIdToDelete,
                                                    Principal principal) {

        log.debug("POST request on /administration/user/cpanel/delete_information_model for info model with id = " + infoModelIdToDelete);

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
                            log.warn("Registry unreachable!");
                            return new ResponseEntity<>("Registry unreachable!",
                                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    } catch (CommunicationException e) {
                        log.info("", e);
                        String message = "Registry threw communication exception: " + e.getMessage();
                        log.warn(message);
                        return new ResponseEntity<>(message, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
                    }

                    return new ResponseEntity<>(new HttpHeaders(), HttpStatus.OK);
                }
            }

            return new ResponseEntity<>("You do not own the Information Model that you tried to delete",
                    new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }

    }

    /**
     * Used for testing
     */
    public void setRabbitManager(RabbitManager rabbitManager){
        this.rabbitManager = rabbitManager;
    }


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
            log.info("", e);
            return new ResponseEntity<>("Communication exception while retrieving the information models: " +
                    e.getMessage(), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }
}