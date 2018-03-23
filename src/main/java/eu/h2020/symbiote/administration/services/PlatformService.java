package eu.h2020.symbiote.administration.services;

import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.administration.model.CoreUser;
import eu.h2020.symbiote.administration.model.Description;
import eu.h2020.symbiote.administration.model.PlatformConfigurationMessage;
import eu.h2020.symbiote.administration.model.PlatformDetails;
import eu.h2020.symbiote.core.cci.PlatformRegistryResponse;
import eu.h2020.symbiote.model.mim.InformationModel;
import eu.h2020.symbiote.model.mim.InterworkingService;
import eu.h2020.symbiote.model.mim.Platform;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import eu.h2020.symbiote.security.commons.enums.OperationType;
import eu.h2020.symbiote.security.communication.payloads.Credentials;
import eu.h2020.symbiote.security.communication.payloads.OwnedService;
import eu.h2020.symbiote.security.communication.payloads.PlatformManagementRequest;
import eu.h2020.symbiote.security.communication.payloads.PlatformManagementResponse;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlatformService {
    private static Log log = LogFactory.getLog(PlatformService.class);

    private RabbitManager rabbitManager;
    private PlatformConfigurer platformConfigurer;
    private ValidationService validationService;
    private InformationModelService informationModelService;
    private CheckServiceOwnershipService checkServiceOwnershipService;
    private String aaMOwnerUsername;
    private String aaMOwnerPassword;


    @Autowired
    public PlatformService(RabbitManager rabbitManager,
                           PlatformConfigurer platformConfigurer,
                           ValidationService validationService,
                           InformationModelService informationModelService,
                           CheckServiceOwnershipService checkServiceOwnershipService,
                           @Value("${aam.deployment.owner.username}") String aaMOwnerUsername,
                           @Value("${aam.deployment.owner.password}") String aaMOwnerPassword) {

        Assert.notNull(rabbitManager,"RabbitManager can not be null!");
        this.rabbitManager = rabbitManager;

        Assert.notNull(platformConfigurer,"PlatformConfigurer can not be null!");
        this.platformConfigurer = platformConfigurer;

        Assert.notNull(validationService,"ValidationService can not be null!");
        this.validationService = validationService;

        Assert.notNull(informationModelService,"InformationModelService can not be null!");
        this.informationModelService = informationModelService;

        Assert.notNull(checkServiceOwnershipService,"CheckServiceOwnershipService can not be null!");
        this.checkServiceOwnershipService = checkServiceOwnershipService;
        
        Assert.notNull(aaMOwnerUsername,"aaMOwnerUsername can not be null!");
        this.aaMOwnerUsername = aaMOwnerUsername;

        Assert.notNull(aaMOwnerPassword,"aaMOwnerPassword can not be null!");
        this.aaMOwnerPassword = aaMOwnerPassword;
    }


    public ResponseEntity<?> registerPlatform(PlatformDetails platformDetails, BindingResult bindingResult,
                                              Principal principal) {

        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();
        String password = (String) token.getCredentials();

        log.debug("User state is: " + ReflectionToStringBuilder.toString(user));
        log.debug(platformDetails.toString());

        List<String> validInfoModelIds = new ArrayList<>();
        ResponseEntity<?> listOfInformationModels = informationModelService.getInformationModels();

        if (listOfInformationModels.getStatusCode() != HttpStatus.OK) {
            log.debug("Could not get information models from Registry");
            return new ResponseEntity<>(listOfInformationModels.getBody(), new HttpHeaders(), listOfInformationModels.getStatusCode());
        } else {
            for (InformationModel informationModel: (List<InformationModel>) listOfInformationModels.getBody()) {
                validInfoModelIds.add(informationModel.getId());
            }
        }

        if (isPlatformRequestInvalid(bindingResult, platformDetails, validInfoModelIds))
            return validationService.getRequestErrors(bindingResult);

        Map<String, Object> responseBody = new HashMap<>();

        // Remove ending slashed from platformDetails inter-working services
        platformDetails.getInterworkingServices().get(0)
                .setUrl(platformDetails.getInterworkingServices().get(0).getUrl().replaceFirst("\\/+$", ""));

        // If form is valid, construct the PlatformManagementResponse to the AAM
        PlatformManagementRequest aamRequest = new PlatformManagementRequest(
                new Credentials(aaMOwnerUsername, aaMOwnerPassword), new Credentials(user.getUsername(), password),
                platformDetails.getInterworkingServices().get(0).getUrl(),
                platformDetails.getName(), platformDetails.getId(), OperationType.CREATE);
        try {
            PlatformManagementResponse aamResponse = rabbitManager.sendManagePlatformRequest(aamRequest);
            if(aamResponse != null) {
                log.debug("AAM responded with: " + aamResponse.getRegistrationStatus());

                if (aamResponse.getRegistrationStatus() == ManagementStatus.OK) {
                    // If AAM responds with OK construct the Platform registration request and send it to registry
                    Platform registryRequest = new Platform();
                    registryRequest.setId(aamResponse.getPlatformId()); // To take into account the empty id
                    registryRequest.setName(platformDetails.getName());
                    registryRequest.setInterworkingServices(platformDetails.getInterworkingServices());
                    registryRequest.setEnabler(platformDetails.getIsEnabler());

                    // FIll in the descriptions. The first comment is the platform description
                    ArrayList<String> descriptions = new ArrayList<>();
                    for (Description description : platformDetails.getDescription())
                        descriptions.add(description.getDescription());
                    registryRequest.setDescription(descriptions);

                    try {
                        PlatformRegistryResponse registryResponse = rabbitManager.sendPlatformCreationRequest(registryRequest);
                        if (registryResponse != null) {
                            if (registryResponse.getStatus() == HttpStatus.OK.value()) {
                                // Platform registered successfully
                                log.info("Platform " + registryRequest.getId() + " registered successfully!");
                                return new ResponseEntity<>(new PlatformDetails(registryRequest), new HttpHeaders(), HttpStatus.CREATED);

                            } else {
                                log.warn("Registration Failed: " + registryResponse.getMessage());

                                sendPlatformDeleteMessageToAAM(new PlatformManagementRequest(
                                        aamRequest.getAamOwnerCredentials(),
                                        aamRequest.getPlatformOwnerCredentials(),
                                        aamRequest.getPlatformInterworkingInterfaceAddress(),
                                        aamRequest.getPlatformInstanceFriendlyName(),
                                        aamRequest.getPlatformInstanceId(),
                                        OperationType.DELETE));

                                responseBody.put("platformRegistrationError", registryResponse.getMessage());
                                return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.valueOf(registryResponse.getStatus()));
                            }
                        } else {
                            log.warn("Registry unreachable!");

                            sendPlatformDeleteMessageToAAM(aamRequest);

                            responseBody.put("platformRegistrationError", "Registry unreachable!");
                            return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    } catch (CommunicationException e) {
                        log.info("", e);
                        log.warn("Registry threw communication exception: " + e.getMessage());

                        sendPlatformDeleteMessageToAAM(aamRequest);

                        responseBody.put("platformRegistrationError", "Registry threw CommunicationException");
                        return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
                    }

                } else if (aamResponse.getRegistrationStatus() == ManagementStatus.PLATFORM_EXISTS) {
                    log.info("AAM says that the Platform exists!");
                    responseBody.put("platformRegistrationError", "AAM says that the Platform exists!");
                    return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST);
                } else {
                    log.info("AAM says that there was an ERROR");
                    responseBody.put("platformRegistrationError", "AAM says that there was an ERROR");
                    return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST);
                }
            } else {
                log.warn("AAM unreachable!");
                responseBody.put("platformRegistrationError", "AAM unreachable!");
                return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (CommunicationException e) {
            log.info("", e);
            String message = "AAM threw CommunicationException: " + e.getMessage();
            log.warn(message);
            responseBody.put("platformRegistrationError", message);
            return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<?> updatePlatform(PlatformDetails platformDetails, BindingResult bindingResult,
                                            Principal principal) {
        // Checking if the user owns the platform
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();
        String password = (String) token.getCredentials();

        List<String> validInfoModelIds = new ArrayList<>();
        ResponseEntity<?> listOfInformationModels = informationModelService.getInformationModels();

        ResponseEntity<?> ownedPlatformDetailsResponse = checkServiceOwnershipService.checkIfUserOwnsService(
                platformDetails.getId(), user, OwnedService.ServiceType.PLATFORM);
        if (ownedPlatformDetailsResponse.getStatusCode() != HttpStatus.OK)
            return ownedPlatformDetailsResponse;

        if (listOfInformationModels.getStatusCode() != HttpStatus.OK) {
            log.debug("Could not get information models from Registry");
            return new ResponseEntity<>(listOfInformationModels.getBody(), new HttpHeaders(), listOfInformationModels.getStatusCode());
        } else {
            for (InformationModel informationModel: (List<InformationModel>) listOfInformationModels.getBody()) {
                validInfoModelIds.add(informationModel.getId());
            }
        }

        if (isPlatformRequestInvalid(bindingResult, platformDetails, validInfoModelIds))
            return validationService.getRequestErrors(bindingResult);


        Map<String, Object> responseBody = new HashMap<>();

        // Remove ending slashed from platformDetails interworking services
        platformDetails.getInterworkingServices().get(0)
                .setUrl(platformDetails.getInterworkingServices().get(0).getUrl().replaceFirst("\\/+$", ""));

        // If form is valid, construct the PlatformManagementResponse to the AAM
        PlatformManagementRequest aamRequest = new PlatformManagementRequest(
                new Credentials(aaMOwnerUsername, aaMOwnerPassword), new Credentials(user.getUsername(), password),
                platformDetails.getInterworkingServices().get(0).getUrl(),
                platformDetails.getName(), platformDetails.getId(), OperationType.UPDATE);

        try {
            PlatformManagementResponse aamResponse = rabbitManager.sendManagePlatformRequest(aamRequest);
            if(aamResponse != null) {
                log.debug("AAM responded with: " + aamResponse.getRegistrationStatus());

                if (aamResponse.getRegistrationStatus() == ManagementStatus.OK) {
                    // If AAM responds with OK construct the Platform update request and send it to registry
                    Platform registryRequest = new Platform();
                    registryRequest.setId(platformDetails.getId()); // To take into account the empty id
                    registryRequest.setName(platformDetails.getName());
                    registryRequest.setInterworkingServices(platformDetails.getInterworkingServices());
                    registryRequest.setEnabler(platformDetails.getIsEnabler());

                    // FIll in the descriptions. The first comment is the platform description
                    ArrayList<String> descriptions = new ArrayList<>();
                    for (Description description : platformDetails.getDescription())
                        descriptions.add(description.getDescription());
                    registryRequest.setDescription(descriptions);

                    try {
                        PlatformRegistryResponse registryResponse = rabbitManager.sendPlatformModificationRequest(registryRequest);
                        if (registryResponse != null) {
                            if (registryResponse.getStatus() == HttpStatus.OK.value()) {
                                // Platform updated successfully
                                log.info("Platform " + registryRequest.getId() + " updated successfully!");
                                return new ResponseEntity<>(new PlatformDetails(registryRequest), new HttpHeaders(), HttpStatus.OK);

                            } else {
                                log.warn("Update Failed: " + registryResponse.getMessage());

                                sendPlatformUndoMessageToAAM((OwnedService) ownedPlatformDetailsResponse.getBody(), user, password);

                                responseBody.put("platformUpdateError", registryResponse.getMessage());
                                return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.valueOf(registryResponse.getStatus()));
                            }
                        } else {
                            log.warn("Registry unreachable!");

                            sendPlatformUndoMessageToAAM((OwnedService) ownedPlatformDetailsResponse.getBody(), user, password);

                            responseBody.put("platformUpdateError", "Registry unreachable!");
                            return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    } catch (CommunicationException e) {
                        log.info("", e);
                        log.warn("Registry threw communication exception: " + e.getMessage());

                        sendPlatformUndoMessageToAAM((OwnedService) ownedPlatformDetailsResponse.getBody(), user, password);

                        responseBody.put("platformUpdateError", "Registry threw CommunicationException");
                        return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
                    }

                } else if (aamResponse.getRegistrationStatus() == ManagementStatus.PLATFORM_EXISTS) {
                    log.info("AAM says that the Platform exists!");
                    responseBody.put("platformUpdateError", "AAM says that the Platform exists!");
                    return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST);
                } else {
                    log.info("AAM says that there was an ERROR");
                    responseBody.put("platformUpdateError", "AAM says that there was an ERROR");
                    return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST);
                }
            } else {
                log.warn("AAM unreachable!");
                responseBody.put("platformUpdateError", "AAM unreachable!");
                return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (CommunicationException e) {
            log.info("", e);
            String message = "AAM threw CommunicationException: " + e.getMessage();
            log.warn(message);
            responseBody.put("platformUpdateError", message);
            return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    public ResponseEntity<?> deletePlatform(String platformIdToDelete, Principal principal) {

        // Checking if the user owns the platform
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();
        String password = (String) token.getCredentials();

        ResponseEntity<?> ownedPlatformDetailsResponse = checkServiceOwnershipService.checkIfUserOwnsService(
                platformIdToDelete, user, OwnedService.ServiceType.PLATFORM);
        if (ownedPlatformDetailsResponse.getStatusCode() != HttpStatus.OK)
            return ownedPlatformDetailsResponse;

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
                log.warn(message);
                // Send deletion message to AAM
                return new ResponseEntity<>(message,
                        new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (CommunicationException e) {
            String message = "Registry threw communication exception";
            log.warn(message, e);
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
                log.warn(message);
                return new ResponseEntity<>(message,
                        new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (CommunicationException e) {
            String message = "AAM threw communication exception: " + e.getMessage();
            log.warn(message, e);
            return new ResponseEntity<>(message,
                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(new HttpHeaders(), HttpStatus.OK);
    }


    public void getPlatformConfig(PlatformConfigurationMessage configurationMessage,
                                  BindingResult bindingResult, Principal principal,
                                  HttpServletResponse response) throws Exception {

        if (bindingResult.hasErrors()) {

            List<FieldError> errors = bindingResult.getFieldErrors();
            for (FieldError fieldError : errors) {
                String errorMessage = fieldError.getDefaultMessage();
                String errorField = "platform_get_config_error_" + fieldError.getField();
                log.debug(errorField + ": " + errorMessage);
            }

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.addHeader("Content-Type", "text/html");

            response.getWriter().write("Invalid Arguments");
            response.getWriter().flush();
            response.getWriter().close();
        } else {
            // Checking if the user owns the platform
            UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
            CoreUser user = (CoreUser) token.getPrincipal();

            String platformId = configurationMessage.getPlatformId();

            ResponseEntity<?> aamResponse = checkServiceOwnershipService.checkIfUserOwnsService(
                    platformId, user, OwnedService.ServiceType.PLATFORM);
            if (aamResponse.getStatusCode() != HttpStatus.OK) {
                response.setStatus(aamResponse.getStatusCodeValue());
                response.getWriter().write("You do not own the platform with id " + platformId);
                response.getWriter().flush();
                response.getWriter().close();
            } else {
                platformConfigurer.returnPlatformConfiguration(response, user,
                        (OwnedService) aamResponse.getBody(), configurationMessage);

            }
        }
    }


    private void sendPlatformDeleteMessageToAAM(PlatformManagementRequest initialRequest) {

        PlatformManagementRequest aamRequest = new PlatformManagementRequest(
                initialRequest.getAamOwnerCredentials(),
                initialRequest.getPlatformOwnerCredentials(),
                initialRequest.getPlatformInterworkingInterfaceAddress(),
                initialRequest.getPlatformInstanceFriendlyName(),
                initialRequest.getPlatformInstanceId(),
                OperationType.DELETE);

        // Send deletion message to AAM
        try {
            PlatformManagementResponse aamResponse = rabbitManager.sendManagePlatformRequest(aamRequest);

            // Todo: Check what happens when platform deletion request is not successful at this stage
            if (aamResponse != null) {
                if (aamResponse.getRegistrationStatus() == ManagementStatus.OK) {
                    log.info("Platform" + aamRequest.getPlatformInstanceId() + " was removed from AAM");
                } else {
                    log.info("Platform" + aamRequest.getPlatformInstanceId() + "  was NOT removed from AAM");
                }
            } else {
                log.warn("AAM unreachable during platform deletion request");
            }
        } catch (CommunicationException e) {
            log.info("", e);
        }
    }

    private void sendPlatformUndoMessageToAAM(OwnedService platformDetails, CoreUser user, String password) {

        PlatformManagementRequest aamRequest = new PlatformManagementRequest(
                new Credentials(aaMOwnerUsername, aaMOwnerPassword),
                new Credentials(user.getUsername(), password),
                platformDetails.getPlatformInterworkingInterfaceAddress(),
                platformDetails.getInstanceFriendlyName(),
                platformDetails.getServiceInstanceId(),
                OperationType.UPDATE);

        // Send deletion message to AAM
        try {
            PlatformManagementResponse aamResponse = rabbitManager.sendManagePlatformRequest(aamRequest);

            // Todo: Check what happens when platform deletion request is not successful at this stage
            if (aamResponse != null) {
                if (aamResponse.getRegistrationStatus() == ManagementStatus.OK) {
                    log.info("Changes in Platform" + aamRequest.getPlatformInstanceId() + " were reverted in AAM");
                } else {
                    log.info("Changes in Platform" + aamRequest.getPlatformInstanceId() + " were NOT reverted in AAM");
                }
            } else {
                log.warn("AAM unreachable during platform undo request");
            }
        } catch (CommunicationException e) {
            log.info("", e);
        }
    }

    private boolean isPlatformRequestInvalid(BindingResult bindingResult, PlatformDetails platformDetails,
                                             List<String> validInfoModelIds) {
        boolean invalidInfoModel = false;

        for (InterworkingService service : platformDetails.getInterworkingServices()) {
            if (!validInfoModelIds.contains(service.getInformationModelId())) {
                log.debug("The information model id is not valid");
                invalidInfoModel = true;
            }
        }

        return bindingResult.hasErrors() || invalidInfoModel;
    }

    public ResponseEntity getPlatformDetailsFromRegistry(String platformId) {

        try {
            PlatformRegistryResponse registryResponse = rabbitManager.sendGetPlatformDetailsMessage(platformId);
            if (registryResponse != null) {
                if (registryResponse.getStatus() != HttpStatus.OK.value()) {
                    log.debug(registryResponse.getMessage());
                    return new ResponseEntity<>(new HttpHeaders(), HttpStatus.NOT_FOUND);
                } else {
                    return new ResponseEntity<>(registryResponse, new HttpHeaders(), HttpStatus.OK);
                }
            } else {
                String message = "Registry unreachable!";
                log.warn(message);
                return new ResponseEntity<>(message,
                        new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (CommunicationException e) {
            String message = "Registry threw CommunicationException";

            log.warn(message, e);
            return new ResponseEntity<>("Registry threw CommunicationException: " + e.getMessage(),
                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
