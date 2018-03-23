package eu.h2020.symbiote.administration.controllers;

import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.administration.model.*;
import eu.h2020.symbiote.administration.services.*;
import eu.h2020.symbiote.model.mim.Federation;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import eu.h2020.symbiote.security.commons.enums.OperationType;
import eu.h2020.symbiote.security.communication.payloads.Credentials;
import eu.h2020.symbiote.security.communication.payloads.UserDetails;
import eu.h2020.symbiote.security.communication.payloads.UserDetailsResponse;
import eu.h2020.symbiote.security.communication.payloads.UserManagementRequest;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring controller for the User control panel, handles management views and form validation.
 *
 * @author Vasileios Glykantzis (ICOM)
 * @author Tilemachos Pechlivanoglou (ICOM)
 */
@Controller
@CrossOrigin
public class UserCpanelController {
    private static Log log = LogFactory.getLog(UserCpanelController.class);

    private RabbitManager rabbitManager;
    private OwnedServicesService ownedServicesService;
    private PlatformService platformService;
    private SSPService sspService;
    private InformationModelService informationModelService;
    private FederationService federationService;
    private String aaMOwnerUsername;
    private String aaMOwnerPassword;

    @Autowired
    public UserCpanelController(RabbitManager rabbitManager,
                                OwnedServicesService ownedServicesService,
                                PlatformService platformService,
                                SSPService sspService,
                                InformationModelService informationModelService,
                                FederationService federationService,
                                @Value("${aam.deployment.owner.username}") String aaMOwnerUsername,
                                @Value("${aam.deployment.owner.password}") String aaMOwnerPassword) {

        Assert.notNull(rabbitManager,"RabbitManager can not be null!");
        this.rabbitManager = rabbitManager;

        Assert.notNull(ownedServicesService,"OwnedServicesService can not be null!");
        this.ownedServicesService = ownedServicesService;

        Assert.notNull(platformService,"PlatformService can not be null!");
        this.platformService = platformService;

        Assert.notNull(sspService,"SSPService can not be null!");
        this.sspService = sspService;

        Assert.notNull(informationModelService,"InformationModelService can not be null!");
        this.informationModelService = informationModelService;

        Assert.notNull(federationService,"FederationService can not be null!");
        this.federationService = federationService;

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
        String password = (String) token.getCredentials();

        UserDetailsResponse response;
        try {
            response = rabbitManager.sendLoginRequest(new Credentials(user.getUsername(), password));

            if(response != null) {
                if (response.getHttpStatus() != HttpStatus.OK) {
                    log.debug("Could not get the userDetails");
                    return new ResponseEntity<>(new HttpHeaders(), response.getHttpStatus());
                }
            } else {
                return new ResponseEntity<>(new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch(CommunicationException e){
            log.info(e.getMessage());
            return new ResponseEntity<>(e.getErrorMessage(), new HttpHeaders(), e.getStatusCode());

        }

        String recoveryMail = response.getUserDetails().getRecoveryMail();
        String role = response.getUserDetails().getRole().toString();
        UserDetailsDDO userDetails = new UserDetailsDDO(user.getUsername(), recoveryMail, role);
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

    @PostMapping("/administration/user/change_password")
    public ResponseEntity<?> changeEmail(@Valid @RequestBody ChangePasswordRequest message,
                                         BindingResult bindingResult,
                                         Principal principal) {
        log.debug("POST request on /administration/user/change_password");

        Map<String, String> errorsResponse = new HashMap<>();
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();
        String password = (String) token.getCredentials();

        if (bindingResult.hasErrors()) {

            List<FieldError> errors = bindingResult.getFieldErrors();
            for (FieldError fieldError : errors) {
                String errorMessage = "Enter a valid password";
                String errorField = "error_" + fieldError.getField();
                log.debug(errorField + ": " + errorMessage);
                errorsResponse.put(errorField, errorMessage);
            }
        }

        if (errorsResponse.get("error_newPasswordRetyped") == null &&
                !message.getNewPassword().equals(message.getNewPasswordRetyped())) {
            String errorField = "error_newPasswordRetyped";
            String errorMessage = "The provided passwords do not match";
            log.debug(errorField + ": " + errorMessage);
            errorsResponse.put(errorField, errorMessage);

        }

        if (!password.equals(message.getOldPassword())) {
            String errorMessage = "Your old password is not correct";
            String errorField = "error_oldPassword";
            log.debug(errorField + ": " + errorMessage);
            errorsResponse.put(errorField, errorMessage);
        }

        if (errorsResponse.size() > 0) {
            errorsResponse.put("changePasswordError", "Invalid Arguments");
            return new ResponseEntity<>(errorsResponse, new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }

        // Construct the UserManagementRequest
        // Todo: Change the federatedId in R4

        UserManagementRequest userUpdateRequest = new UserManagementRequest(
                new Credentials(aaMOwnerUsername, aaMOwnerPassword),
                new Credentials(user.getUsername(), password),
                new UserDetails(
                        new Credentials(user.getUsername(), message.getNewPassword()),
                        user.getRecoveryMail(),
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
                response.put("changePasswordError","Authorization Manager is unreachable!");
                return new ResponseEntity<>(response, new HttpHeaders(),
                        HttpStatus.INTERNAL_SERVER_ERROR);

            } else if(managementStatus == ManagementStatus.OK ){
                return new ResponseEntity<>(response, new HttpHeaders(),
                        HttpStatus.OK);
            }
        } catch (CommunicationException e) {
            response.put("changePasswordError",e.getMessage());
            return  new ResponseEntity<>(response, new HttpHeaders(),
                    HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    @PostMapping("/administration/user/cpanel/list_user_services")
    public ResponseEntity<ListUserServicesResponse> listUserPlatforms(Principal principal) {

        log.debug("POST request on /administration/user/cpanel/list_user_services");
        return ownedServicesService.listUserServices(principal);
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

    @PostMapping("/administration/user/cpanel/register_ssp")
    public ResponseEntity<?> registerSSP(@Valid @RequestBody SSPDetails sspDetails,
                                              BindingResult bindingResult, Principal principal) {

        log.debug("POST request on /administration/user/cpanel/register_ssp");

        return sspService.registerSSP(sspDetails, bindingResult, principal);
    }

    @PostMapping("/administration/user/cpanel/delete_ssp")
    public ResponseEntity<?> deleteSSP(@RequestParam String sspIdToDelete, Principal principal) {

        log.debug("POST request on /administration/user/cpanel/delete_ssp for ssp with id: " +
                sspIdToDelete);
        return sspService.deleteSSP(sspIdToDelete, principal);
    }


    @PostMapping("/administration/user/cpanel/list_all_info_models")
    public ResponseEntity<?> listAllInformationModels() {

        log.debug("POST request on /administration/user/cpanel/list_all_info_models");

        // Get InformationModelList from Registry
        return informationModelService.getInformationModels();

    }

    @PostMapping("/administration/user/cpanel/list_user_info_models")
    public ResponseEntity<?> listUserInformationModels(Principal principal) {

        log.debug("POST request on /administration/user/cpanel/list_user_info_models");

        return informationModelService.listUserInformationModels(principal);
    }

    @PostMapping("/administration/user/cpanel/register_information_model")
    public ResponseEntity<?> registerInformationModel(@RequestParam("info-model-name") String name,
                                                      @RequestParam("info-model-uri") String uri,
                                                      @RequestParam("info-model-rdf") MultipartFile rdfFile,
                                                      Principal principal) {

        log.debug("POST request on /administration/user/cpanel/register_information_model");
        return informationModelService.registerInformationModel(name, uri, rdfFile, principal);
    }


    @PostMapping("/administration/user/cpanel/delete_information_model")
    public ResponseEntity<?> deleteInformationModel(@RequestParam String infoModelIdToDelete,
                                                    Principal principal) {

        log.debug("POST request on /administration/user/cpanel/delete_information_model for info model with id = " + infoModelIdToDelete);
        return informationModelService.deleteInformationModel(infoModelIdToDelete, principal);
    }

    @PostMapping("/administration/user/cpanel/list_federations")
    public ResponseEntity<?> listFederations() {

        log.debug("POST request on /administration/user/cpanel/list_federations");
        return federationService.listFederations();
    }

    @PostMapping("/administration/user/cpanel/create_federation")
    public ResponseEntity<?> createFederation(@Valid @RequestBody Federation federation,
                                              BindingResult bindingResult) {

        log.debug("POST request on /administration/user/cpanel/create_federation with RequestBody: "
                + ReflectionToStringBuilder.toString(federation));
        return federationService.createFederation(federation, bindingResult);
    }

    @PostMapping("/administration/user/cpanel/leave_federation")
    public ResponseEntity<?> createFederation(@RequestParam String federationId, @RequestParam String platformId,
                                              Principal principal) {

        log.debug("POST request on /administration/user/cpanel/leave_federation for federationId = "
                + federationId + " platformId = " + platformId);
        return federationService.leaveFederation(federationId, platformId, principal, false);
    }

    /**
     * Used for testing
     */
    public void setRabbitManager(RabbitManager rabbitManager){
        this.rabbitManager = rabbitManager;
    }

}