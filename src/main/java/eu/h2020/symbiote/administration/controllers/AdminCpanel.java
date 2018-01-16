package eu.h2020.symbiote.administration.controllers;

import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.administration.model.CoreUser;

import eu.h2020.symbiote.administration.model.CreateFederationRequest;
import eu.h2020.symbiote.core.cci.InformationModelRequest;
import eu.h2020.symbiote.core.cci.InformationModelResponse;
import eu.h2020.symbiote.core.internal.ClearDataRequest;
import eu.h2020.symbiote.core.internal.ClearDataResponse;
import eu.h2020.symbiote.core.internal.InformationModelListResponse;
import eu.h2020.symbiote.model.mim.InformationModel;
import eu.h2020.symbiote.security.communication.payloads.Credentials;
import eu.h2020.symbiote.security.communication.payloads.FederationRule;
import eu.h2020.symbiote.security.communication.payloads.FederationRuleManagementRequest;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
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

import javax.validation.Valid;
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
public class AdminCpanel {
    private static Log log = LogFactory.getLog(AdminCpanel.class);

    private RabbitManager rabbitManager;
    private ResourceLoader resourceLoader;
    private String aaMOwnerUsername;
    private String aaMOwnerPassword;
    private String coreInterfaceAddress;
    private String cloudCoreInterfaceAddress;
    private String paamValidityMillis;



    @Autowired
    public AdminCpanel(RabbitManager rabbitManager, ResourceLoader resourceLoader,
                       @Value("${aam.deployment.owner.username}") String aaMOwnerUsername,
                       @Value("${aam.deployment.owner.password}") String aaMOwnerPassword,
                       @Value("${aam.environment.coreInterfaceAddress}") String coreInterfaceAddress,
                       @Value("${paam.deployment.token.validityMillis}") String paamValidityMillis) {
        Assert.notNull(rabbitManager,"RabbitManager can not be null!");
        this.rabbitManager = rabbitManager;

        Assert.notNull(resourceLoader,"ResourceLoader can not be null!");
        this.resourceLoader = resourceLoader;

        Assert.notNull(aaMOwnerUsername,"aaMOwnerUsername can not be null!");
        this.aaMOwnerUsername = aaMOwnerUsername;

        Assert.notNull(aaMOwnerPassword,"aaMOwnerPassword can not be null!");
        this.aaMOwnerPassword = aaMOwnerPassword;

        Assert.notNull(coreInterfaceAddress,"coreInterfaceAddress can not be null!");
        this.coreInterfaceAddress = coreInterfaceAddress;

        Assert.notNull(paamValidityMillis,"paamValidityMillis can not be null!");
        this.paamValidityMillis = paamValidityMillis;

        this.cloudCoreInterfaceAddress = this.coreInterfaceAddress.replace("8100/coreInterface", "8101/cloudCoreInterface");
    }

    /**
     * Gets the default view. If the user is a platform owner, tries to fetch their details.
     * Registry is first polled and, if the platform isn't activated there, AAM is polled for them.
     */
    @GetMapping("/administration/admin/cpanel")
    public String userCPanel(Model model, Principal principal) {

        log.debug("GET request on /administration/admin/cpanel");

        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();

        log.debug("User state is: " + ReflectionToStringBuilder.toString(user));

        model.addAttribute("user", user);

        return "index";
    }

    @PostMapping("/administration/admin/cpanel/delete_platform_resources")
    public ResponseEntity<?> deletePlatformResources(@RequestParam String platformId) {

        log.debug("POST request on /administration/admin/cpanel/delete_platform_resources for info model with id = " + platformId);

        // Ask Registry
        try {
            ClearDataRequest request = new ClearDataRequest(null, platformId);

            ClearDataResponse response = rabbitManager.sendClearDataRequest(request);
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
            String message = "Registry threw communication exception: " + e.getMessage();
            log.warn(message, e);
            return new ResponseEntity<>(message, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(new HttpHeaders(), HttpStatus.OK);
    }

    @PostMapping("/administration/admin/cpanel/delete_information_model")
    public ResponseEntity<?> deleteInformationModel(@RequestParam String infoModelIdToDelete) {

        log.debug("POST request on /administration/admin/cpanel/delete_information_model for info model with id = " + infoModelIdToDelete);

        // Get InformationModelList from Registry
        ResponseEntity<?> responseEntity = getInformationModels();
        if (responseEntity.getStatusCode() != HttpStatus.OK)
            return responseEntity;
        else {

            for (InformationModel informationModel : (List<InformationModel>)responseEntity.getBody()) {
                log.debug(informationModel.getId() + " " + informationModel.getOwner());
                if (informationModel.getId().equals(infoModelIdToDelete)) {

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
                        String message = "Registry threw communication exception: " + e.getMessage();
                        log.warn(message, e);
                        return new ResponseEntity<>(message, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
                    }

                    return new ResponseEntity<>(new HttpHeaders(), HttpStatus.OK);
                }
            }

            return new ResponseEntity<>("Information Model NOT FOUND",
                    new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }

    }

    @PostMapping("/administration/admin/cpanel/create_federation")
    public ResponseEntity<?> createFederation(@Valid @RequestBody CreateFederationRequest createFederationRequest,
                                              BindingResult bindingResult) {

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
                        responseBody.put("message", "Federation Registration was successful!");
                        responseBody.put("federationRule", entry.getValue());
                        return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.CREATED);

                    } else {
                        String message = "Not both platforms ids present in AAM response";
                        responseBody.put("error", message);
                        return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST);
                    }
                } else {
                    String message = "Contains more than 1 Federation rule";
                    log.warn(message);
                    responseBody.put("error", message);
                    return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST);
                }
            } else {
                String message = "AAM unreachable";
                log.warn(message);
                responseBody.put("error", message);
                return new ResponseEntity<>(responseBody,
                        new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (CommunicationException e) {
            String message = "AAM threw communication exception";
            log.warn(message, e);
            responseBody.put("error", message + ": " + e.getMessage());
            return new ResponseEntity<>(responseBody,
                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @PostMapping("/administration/admin/cpanel/list_federations")
    public ResponseEntity<?> listFederations() {

        log.debug("POST request on /administration/user/cpanel/list_federations");

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
                log.warn(message);
                responseBody.put("error", message);
                return new ResponseEntity<>(responseBody,
                        new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (CommunicationException e) {
            String message = "AAM threw communication exception during ListFederationRequest: " + e.getMessage();
            log.warn(message, e);
            responseBody.put("error", message);
            return new ResponseEntity<>(responseBody,
                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }


    @PostMapping("/administration/admin/cpanel/delete_federation")
    public ResponseEntity<?> deleteFederation(@RequestParam String federationIdToDelete) {

        log.debug("POST request on /administration/user/cpanel/delete_federation for federation with id = " + federationIdToDelete);

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
                log.warn(message);
                responseBody.put("error", message);
                return new ResponseEntity<>(responseBody,
                        new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (CommunicationException e) {
            String message = "AAM threw communication exception during DeleteFederationRequest: " + e.getMessage();
            log.warn(message, e);
            responseBody.put("error", message);
            return new ResponseEntity<>(responseBody,
                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
            log.warn("Communication exception while retrieving the information models", e);
            return new ResponseEntity<>("Communication exception while retrieving the information models: " +
                    e.getMessage(), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }

    /**
     * Used for testing
     */
    public void setRabbitManager(RabbitManager rabbitManager){
        this.rabbitManager = rabbitManager;
    }
}