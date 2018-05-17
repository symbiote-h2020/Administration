package eu.h2020.symbiote.administration.services.ssp;

import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.administration.model.CoreUser;
import eu.h2020.symbiote.administration.model.SSPDetails;
import eu.h2020.symbiote.administration.services.CheckServiceOwnershipService;
import eu.h2020.symbiote.administration.services.validation.ValidationService;
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
import java.util.HashMap;
import java.util.Map;

@Service
public class SSPService {
    private static Log log = LogFactory.getLog(SSPService.class);

    private RabbitManager rabbitManager;
    private CheckServiceOwnershipService checkServiceOwnershipService;
    private ValidationService validationService;
    private String aaMOwnerUsername;
    private String aaMOwnerPassword;


    @Autowired
    public SSPService(RabbitManager rabbitManager,
                      ValidationService validationService,
                      CheckServiceOwnershipService checkServiceOwnershipService,
                      @Value("${aam.deployment.owner.username}") String aaMOwnerUsername,
                      @Value("${aam.deployment.owner.password}") String aaMOwnerPassword) {

        Assert.notNull(rabbitManager,"RabbitManager can not be null!");
        this.rabbitManager = rabbitManager;

        Assert.notNull(checkServiceOwnershipService,"CheckServiceOwnershipService can not be null!");
        this.checkServiceOwnershipService = checkServiceOwnershipService;

        Assert.notNull(validationService,"ValidationService can not be null!");
        this.validationService = validationService;

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
                    // Override with the received instanceId from the core
                    return new ResponseEntity<>(
                            new SSPDetails(
                                    aamResponse.getSmartSpaceId(),
                                    sspDetails.getName(),
                                    sspDetails.getExternalAddress(),
                                    sspDetails.getSiteLocalAddress(),
                                    sspDetails.getExposingSiteLocalAddress()
                            ),
                            new HttpHeaders(),
                            HttpStatus.CREATED
                    );

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
}
