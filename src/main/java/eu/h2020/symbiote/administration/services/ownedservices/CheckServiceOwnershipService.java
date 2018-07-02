package eu.h2020.symbiote.administration.services.ownedservices;

import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import eu.h2020.symbiote.administration.exceptions.rabbit.CommunicationException;
import eu.h2020.symbiote.administration.model.CoreUser;
import eu.h2020.symbiote.security.commons.enums.AccountStatus;
import eu.h2020.symbiote.security.commons.enums.OperationType;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.communication.payloads.Credentials;
import eu.h2020.symbiote.security.communication.payloads.OwnedService;
import eu.h2020.symbiote.security.communication.payloads.UserDetails;
import eu.h2020.symbiote.security.communication.payloads.UserManagementRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Set;

@Service
public class CheckServiceOwnershipService {
    private static Log log = LogFactory.getLog(CheckServiceOwnershipService.class);

    private RabbitManager rabbitManager;
    private String aaMOwnerUsername;
    private String aaMOwnerPassword;

    @Autowired
    public CheckServiceOwnershipService(RabbitManager rabbitManager,
                                        @Value("${aam.deployment.owner.username}") String aaMOwnerUsername,
                                        @Value("${aam.deployment.owner.password}") String aaMOwnerPassword) {
        this.rabbitManager = rabbitManager;

        Assert.notNull(aaMOwnerUsername,"aaMOwnerUsername can not be null!");
        this.aaMOwnerUsername = aaMOwnerUsername;

        Assert.notNull(aaMOwnerPassword,"aaMOwnerPassword can not be null!");
        this.aaMOwnerPassword = aaMOwnerPassword;
    }

    public ResponseEntity<?> checkIfUserOwnsService(String serviceId, CoreUser user, OwnedService.ServiceType serviceType) {

        String serviceTypeName = serviceType.toString().toLowerCase().replace("_", " ");

        UserManagementRequest ownedPlatformDetailsRequest = new UserManagementRequest(
                new Credentials(aaMOwnerUsername, aaMOwnerPassword),
                new Credentials(user.getUsername(), ""),
                new UserDetails(
                        new Credentials(user.getUsername(), ""),
                        "",
                        UserRole.NULL,
                        AccountStatus.ACTIVE,
                        new HashMap<>(),
                        new HashMap<>(),
                        true,
                        false
                ),
                OperationType.CREATE
        );

        try {
            Set<OwnedService> ownedServiceSet =
                    rabbitManager.sendOwnedServiceDetailsRequest(ownedPlatformDetailsRequest);
            OwnedService ownedService = null;

            if (ownedServiceSet != null) {
                boolean ownsService = false;
                for (OwnedService ownedServiceDetails : ownedServiceSet) {
                    log.debug(ownedServiceDetails);
                    if (ownedServiceDetails.getServiceInstanceId().equals(serviceId)) {
                        String message = "The user owns the " + serviceTypeName + " with id " + serviceId;
                        log.info(message);
                        ownsService = true;
                        ownedService = ownedServiceDetails;
                        break;
                    }
                }

                if (!ownsService) {
                    String message = "You do not own the " + serviceTypeName + " with id " + serviceId;
                    log.info(message);
                    return new ResponseEntity<>("You do not own the " + serviceTypeName + " with id " + serviceId,
                            new HttpHeaders(), HttpStatus.BAD_REQUEST);
                } else {
                    return new ResponseEntity<>(ownedService,
                            new HttpHeaders(), HttpStatus.OK);
                }
            } else {
                String message = "AAM unreachable";
                log.warn(message);
                return new ResponseEntity<>("AAM unreachable",
                        new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (CommunicationException e) {
            String message = "AAM threw Communication Exception";
            log.warn(message, e);
            return new ResponseEntity<>(message + ": " + e.getMessage(),
                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
