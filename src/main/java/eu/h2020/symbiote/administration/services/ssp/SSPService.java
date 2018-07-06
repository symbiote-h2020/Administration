package eu.h2020.symbiote.administration.services.ssp;

import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import eu.h2020.symbiote.administration.exceptions.rabbit.CommunicationException;
import eu.h2020.symbiote.administration.model.CoreUser;
import eu.h2020.symbiote.administration.model.Description;
import eu.h2020.symbiote.administration.model.SSPDetails;
import eu.h2020.symbiote.administration.services.infomodel.InformationModelService;
import eu.h2020.symbiote.administration.services.ownedservices.CheckServiceOwnershipService;
import eu.h2020.symbiote.administration.services.validation.ValidationService;
import eu.h2020.symbiote.core.cci.SspRegistryResponse;
import eu.h2020.symbiote.model.mim.InformationModel;
import eu.h2020.symbiote.model.mim.InterworkingService;
import eu.h2020.symbiote.model.mim.SmartSpace;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import eu.h2020.symbiote.security.commons.enums.OperationType;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.security.communication.payloads.Credentials;
import eu.h2020.symbiote.security.communication.payloads.OwnedService;
import eu.h2020.symbiote.security.communication.payloads.SmartSpaceManagementRequest;
import eu.h2020.symbiote.security.communication.payloads.SmartSpaceManagementResponse;
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

import java.security.Principal;
import java.util.*;

@Service
public class SSPService {
    private static Log log = LogFactory.getLog(SSPService.class);

    private final RabbitManager rabbitManager;
    private final CheckServiceOwnershipService checkServiceOwnershipService;
    private final ValidationService validationService;
    private final InformationModelService informationModelService;
    private String aaMOwnerUsername;
    private String aaMOwnerPassword;


    @Autowired
    public SSPService(RabbitManager rabbitManager,
                      ValidationService validationService,
                      CheckServiceOwnershipService checkServiceOwnershipService,
                      InformationModelService informationModelService,
                      @Value("${aam.deployment.owner.username}") String aaMOwnerUsername,
                      @Value("${aam.deployment.owner.password}") String aaMOwnerPassword) {

        Assert.notNull(rabbitManager,"RabbitManager can not be null!");
        this.rabbitManager = rabbitManager;

        Assert.notNull(checkServiceOwnershipService,"CheckServiceOwnershipService can not be null!");
        this.checkServiceOwnershipService = checkServiceOwnershipService;

        Assert.notNull(validationService,"ValidationService can not be null!");
        this.validationService = validationService;

        Assert.notNull(informationModelService,"InformationModelService can not be null!");
        this.informationModelService = informationModelService;

        Assert.notNull(aaMOwnerUsername,"aaMOwnerUsername can not be null!");
        this.aaMOwnerUsername = aaMOwnerUsername;

        Assert.notNull(aaMOwnerPassword,"aaMOwnerPassword can not be null!");
        this.aaMOwnerPassword = aaMOwnerPassword;
    }


    public ResponseEntity<?> registerSSP(SSPDetails sspDetails, BindingResult bindingResult,
                                         Principal principal) {

        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();
        String password = (String) token.getCredentials();

        log.debug("User state is: " + ReflectionToStringBuilder.toString(user));
        log.debug(sspDetails.toString());

        if (bindingResult.hasErrors())
            return validationService.getRequestErrors(bindingResult);

        Map<String, Object> responseBody = new HashMap<>();

        try {
            // If form is valid, construct the SmartSpaceManagementRequest to the AAM
            SmartSpaceManagementRequest aamRequest = new SmartSpaceManagementRequest(
                    new Credentials(aaMOwnerUsername, aaMOwnerPassword),
                    new Credentials(user.getUsername(), password),
                    sspDetails.getExternalAddress(),
                    sspDetails.getSiteLocalAddress(),
                    sspDetails.getName(),
                    OperationType.CREATE,
                    sspDetails.getId(),
                    sspDetails.getExposingSiteLocalAddress());

            SmartSpaceManagementResponse aamResponse = rabbitManager.sendManageSSPRequest(aamRequest);

            if(aamResponse != null) {
                log.debug("AAM responded with: " + aamResponse.getManagementStatus());

                if (aamResponse.getManagementStatus() == ManagementStatus.OK) {
                    // If AAM responds with OK construct the Platform registration request and send it to registry
                    SmartSpace smartSpaceRequest = new SmartSpace();
                    smartSpaceRequest.setId(aamResponse.getSmartSpaceId());
                    smartSpaceRequest.setName(sspDetails.getName());

                    InterworkingService interworkingService = new InterworkingService();
                    interworkingService.setUrl(sspDetails.getExternalAddress());
                    interworkingService.setInformationModelId(sspDetails.getInformationModelId());

                    smartSpaceRequest.setInterworkingServices(new ArrayList<>(Collections.singletonList(interworkingService)));

                    // FIll in the descriptions. The first comment is the platform description
                    ArrayList<String> descriptions = new ArrayList<>();
                    for (Description description : sspDetails.getDescription())
                        descriptions.add(description.getDescription());
                    smartSpaceRequest.setDescription(descriptions);

                    try {
                        SspRegistryResponse registryResponse = rabbitManager.sendSmartSpaceCreationRequest(smartSpaceRequest);
                        if (registryResponse != null) {
                            if (registryResponse.getStatus() == HttpStatus.OK.value()) {
                                // SSP registered successfully
                                log.info("SSP " + smartSpaceRequest.getId() + " registered successfully!");
                                // Override with the received instanceId from the core
                                return new ResponseEntity<>(
                                        new SSPDetails(
                                                aamResponse.getSmartSpaceId(),
                                                sspDetails.getName(),
                                                sspDetails.getDescription(),
                                                sspDetails.getExternalAddress(),
                                                sspDetails.getSiteLocalAddress(),
                                                sspDetails.getInformationModelId(),
                                                sspDetails.getExposingSiteLocalAddress()
                                        ),
                                        new HttpHeaders(),
                                        HttpStatus.CREATED
                                );

                            } else {
                                log.warn("Registration Failed: " + registryResponse.getMessage());

                                sendSSPDeleteMessageToAAM(aamRequest);

                                responseBody.put("platformRegistrationError", registryResponse.getMessage());
                                return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.valueOf(registryResponse.getStatus()));
                            }
                        } else {
                            log.warn("Registry unreachable!");

                            sendSSPDeleteMessageToAAM(aamRequest);

                            responseBody.put("sspRegistrationError", "Registry unreachable!");
                            return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    } catch (CommunicationException e) {
                        log.info("", e);
                        log.warn("Registry threw communication exception: " + e.getMessage());

                        sendSSPDeleteMessageToAAM(aamRequest);

                        responseBody.put("sspRegistrationError", "Registry threw CommunicationException");
                        return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
                    }

                } else if (aamResponse.getManagementStatus() == ManagementStatus.PLATFORM_EXISTS) {
                    log.info("AAM says that the SSP exists!");
                    responseBody.put("sspRegistrationError", "AAM says that the SSP exists!");
                    return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST);
                } else {
                    log.info("AAM says that there was an ERROR");
                    responseBody.put("sspRegistrationError", "AAM says that there was an ERROR");
                    return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST);
                }
            } else {
                log.warn("AAM unreachable!");
                responseBody.put("sspRegistrationError", "AAM unreachable!");
                return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (InvalidArgumentsException | CommunicationException e) {
            log.info("", e);
            String message = "AAM threw " + e.getClass().getSimpleName() + ": "+ e.getMessage();
            log.warn(message);
            responseBody.put("sspRegistrationError", message);
            return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<?> updateSSP(SSPDetails sspDetails, BindingResult bindingResult,
                                       Principal principal) {

        // Checking if the user owns the ssp
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();
        String password = (String) token.getCredentials();

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

        if (isSSPRequestInvalid(bindingResult, sspDetails, validInfoModelIds))
            return validationService.getRequestErrors(bindingResult);

        ResponseEntity<?> ownedPlatformDetailsResponse = checkServiceOwnershipService.checkIfUserOwnsService(
                sspDetails.getId(), user, OwnedService.ServiceType.SMART_SPACE);
        if (ownedPlatformDetailsResponse.getStatusCode() != HttpStatus.OK)
            return ownedPlatformDetailsResponse;


        Map<String, Object> responseBody = new HashMap<>();

        // Remove ending slashed from sspDetails external and local addresses
        sspDetails = new SSPDetails(
                sspDetails.getId(),
                sspDetails.getName(),
                sspDetails.getDescription(),
                sspDetails.getExternalAddress().replaceFirst("\\/+$", ""),
                sspDetails.getSiteLocalAddress().replaceFirst("\\/+$", ""),
                sspDetails.getInformationModelId(),
                sspDetails.getExposingSiteLocalAddress()
        );


        try {
            // If form is valid, construct the SmartSpaceManagementRequest to the AAM
            SmartSpaceManagementRequest aamRequest = new SmartSpaceManagementRequest(
                    new Credentials(aaMOwnerUsername, aaMOwnerPassword),
                    new Credentials(user.getUsername(), password),
                    sspDetails.getExternalAddress(),
                    sspDetails.getSiteLocalAddress(),
                    sspDetails.getName(),
                    OperationType.UPDATE,
                    sspDetails.getId(),
                    sspDetails.getExposingSiteLocalAddress()
            );

            SmartSpaceManagementResponse aamResponse = rabbitManager.sendManageSSPRequest(aamRequest);
            if(aamResponse != null) {
                log.debug("AAM responded with: " + aamResponse.getManagementStatus());

                if (aamResponse.getManagementStatus() == ManagementStatus.OK) {
                    // If AAM responds with OK construct the SSP update request and send it to registry
                    SmartSpace registryRequest = new SmartSpace();
                    registryRequest.setId(sspDetails.getId()); // To take into account the empty id
                    registryRequest.setName(sspDetails.getName());

                    InterworkingService interworkingService = new InterworkingService();
                    interworkingService.setUrl(sspDetails.getExternalAddress());
                    interworkingService.setInformationModelId(sspDetails.getInformationModelId());

                    registryRequest.setInterworkingServices(new ArrayList<>(Collections.singletonList(interworkingService)));

                    // FIll in the descriptions. The first comment is the platform description
                    ArrayList<String> descriptions = new ArrayList<>();
                    for (Description description : sspDetails.getDescription())
                        descriptions.add(description.getDescription());
                    registryRequest.setDescription(descriptions);

                    try {
                        SspRegistryResponse registryResponse = rabbitManager.sendSmartSpaceModificationRequest(registryRequest);
                        if (registryResponse != null) {
                            if (registryResponse.getStatus() == HttpStatus.OK.value()) {
                                // SSP updated successfully
                                log.info("SSP " + registryRequest.getId() + " updated successfully!");
                                return new ResponseEntity<>(sspDetails,
                                        new HttpHeaders(), HttpStatus.OK);

                            } else {
                                log.warn("Update Failed: " + registryResponse.getMessage());

                                sendSSPUndoMessageToAAM((OwnedService) ownedPlatformDetailsResponse.getBody(), user, password);

                                responseBody.put("sspUpdateError", registryResponse.getMessage());
                                return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.valueOf(registryResponse.getStatus()));
                            }
                        } else {
                            log.warn("Registry unreachable!");

                            sendSSPUndoMessageToAAM((OwnedService) ownedPlatformDetailsResponse.getBody(), user, password);

                            responseBody.put("sspUpdateError", "Registry unreachable!");
                            return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    } catch (CommunicationException e) {
                        log.info("", e);
                        log.warn("Registry threw communication exception: " + e.getMessage());

                        sendSSPUndoMessageToAAM((OwnedService) ownedPlatformDetailsResponse.getBody(), user, password);

                        responseBody.put("sspUpdateError", "Registry threw CommunicationException");
                        return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
                    }

                } else if (aamResponse.getManagementStatus() == ManagementStatus.PLATFORM_EXISTS) {
                    log.info("AAM says that the Platform exists!");
                    responseBody.put("sspUpdateError", "AAM says that the Platform exists!");
                    return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST);
                } else {
                    log.info("AAM says that there was an ERROR");
                    responseBody.put("sspUpdateError", "AAM says that there was an ERROR");
                    return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST);
                }
            } else {
                log.warn("AAM unreachable!");
                responseBody.put("sspUpdateError", "AAM unreachable!");
                return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (CommunicationException | InvalidArgumentsException e) {
            log.info("", e);
            String message = "AAM threw CommunicationException: " + e.getMessage();
            log.warn(message);
            responseBody.put("sspUpdateError", message);
            return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    public ResponseEntity<?> deleteSSP(String sspIdToDelete, Principal principal) {

        // Checking if the user owns the ssp
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();
        String password = (String) token.getCredentials();

        ResponseEntity<?> ownedPlatformDetailsResponse = checkServiceOwnershipService.checkIfUserOwnsService(
                sspIdToDelete,
                user,
                OwnedService.ServiceType.SMART_SPACE);
        if (ownedPlatformDetailsResponse.getStatusCode() != HttpStatus.OK)
            return ownedPlatformDetailsResponse;

        // Check with Registry
        try {
            SmartSpace registryRequest = new SmartSpace();
            registryRequest.setId(sspIdToDelete);

            SspRegistryResponse registryResponse = rabbitManager.sendSmartSpaceRemovalRequest(registryRequest);
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
        try {
            SmartSpaceManagementRequest aamRequest = new SmartSpaceManagementRequest(
                    new Credentials(aaMOwnerUsername, aaMOwnerPassword),
                    new Credentials(user.getUsername(), password),
                    "",
                    "",
                    "",
                    OperationType.DELETE,
                    sspIdToDelete,
                    true);

            SmartSpaceManagementResponse aamResponse = rabbitManager.sendManageSSPRequest(aamRequest);

            if(aamResponse != null) {
                log.debug("AAM responded with: " + aamResponse.getManagementStatus());

                if (aamResponse.getManagementStatus() != ManagementStatus.OK) {
                    String message = "AAM says that the SSP does not exist!";
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
        } catch (CommunicationException | InvalidArgumentsException e) {
            String message = "AAM threw " + e.getClass().getSimpleName() + ": "+ e.getMessage();
            log.warn(message, e);
            return new ResponseEntity<>(message,
                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(new HttpHeaders(), HttpStatus.OK);
    }

    private void sendSSPDeleteMessageToAAM(SmartSpaceManagementRequest initialRequest) {

        // Send deletion message to AAM
        try {
            SmartSpaceManagementRequest aamRequest = new SmartSpaceManagementRequest(
                    initialRequest.getAamOwnerCredentials(),
                    initialRequest.getServiceOwnerCredentials(),
                    initialRequest.getExternalAddress(),
                    initialRequest.getSiteLocalAddress(),
                    initialRequest.getInstanceFriendlyName(),
                    OperationType.DELETE,
                    initialRequest.getInstanceId(),
                    initialRequest.isExposingSiteLocalAddress());

            SmartSpaceManagementResponse aamResponse = rabbitManager.sendManageSSPRequest(aamRequest);

            // Todo: Check what happens when ssp deletion request is not successful at this stage
            if (aamResponse != null) {
                if (aamResponse.getManagementStatus() == ManagementStatus.OK) {
                    log.info("SSP" + aamRequest.getInstanceId() + " was removed from AAM");
                } else {
                    log.info("SSP" + aamRequest.getInstanceId() + "  was NOT removed from AAM");
                }
            } else {
                log.warn("AAM unreachable during platform deletion request");
            }
        } catch (CommunicationException | InvalidArgumentsException e) {
            log.info("", e);
        }
    }

    public ResponseEntity getSSPDetailsFromRegistry(String sspId) {

        try {
            SspRegistryResponse registryResponse = rabbitManager.sendGetSSPDetailsMessage(sspId);
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

    private boolean isSSPRequestInvalid(BindingResult bindingResult, SSPDetails sspDetails,
                                        List<String> validInfoModelIds) {
        boolean invalidInfoModel = false;

        if (!validInfoModelIds.contains(sspDetails.getInformationModelId())) {
            log.debug("The information model id is not valid");
            invalidInfoModel = true;
        }

        return bindingResult.hasErrors() || invalidInfoModel;
    }

    private void sendSSPUndoMessageToAAM(OwnedService sspDetails, CoreUser user, String password) {

        // Send deletion message to AAM
        try {
            SmartSpaceManagementRequest aamRequest = new SmartSpaceManagementRequest(
                    new Credentials(aaMOwnerUsername, aaMOwnerPassword),
                    new Credentials(user.getUsername(), password),
                    sspDetails.getExternalAddress(),
                    sspDetails.getSiteLocalAddress(),
                    sspDetails.getInstanceFriendlyName(),
                    OperationType.CREATE,
                    sspDetails.getServiceInstanceId(),
                    sspDetails.isExposingSiteLocalAddress());

            SmartSpaceManagementResponse aamResponse = rabbitManager.sendManageSSPRequest(aamRequest);

            // Todo: Check what happens when ssp deletion request is not successful at this stage
            if (aamResponse != null) {
                if (aamResponse.getManagementStatus() == ManagementStatus.OK) {
                    log.info("Changes in Platform" + aamRequest.getInstanceId() + " were reverted in AAM");
                } else {
                    log.info("Changes in Platform" + aamRequest.getInstanceId() + " were NOT reverted in AAM");
                }
            } else {
                log.warn("AAM unreachable during platform undo request");
            }
        } catch (CommunicationException | InvalidArgumentsException e) {
            log.info("", e);
        }
    }
}
