package eu.h2020.symbiote.administration.services;

import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.administration.model.CoreUser;
import eu.h2020.symbiote.administration.model.ListUserServicesResponse;
import eu.h2020.symbiote.administration.model.PlatformDetails;
import eu.h2020.symbiote.administration.model.SSPDetails;
import eu.h2020.symbiote.administration.services.platform.PlatformService;
import eu.h2020.symbiote.core.cci.PlatformRegistryResponse;
import eu.h2020.symbiote.model.mim.Platform;
import eu.h2020.symbiote.security.commons.enums.OperationType;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.communication.payloads.Credentials;
import eu.h2020.symbiote.security.communication.payloads.OwnedService;
import eu.h2020.symbiote.security.communication.payloads.UserDetails;
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
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@Service
public class OwnedServicesService {

    private static Log log = LogFactory.getLog(OwnedServicesService.class);

    private RabbitManager rabbitManager;
    private PlatformService platformService;
    private String aaMOwnerUsername;
    private String aaMOwnerPassword;

    @Autowired
    public OwnedServicesService(RabbitManager rabbitManager,
                                PlatformService platformService,
                                @Value("${aam.deployment.owner.username}") String aaMOwnerUsername,
                                @Value("${aam.deployment.owner.password}") String aaMOwnerPassword) {

        this.rabbitManager = rabbitManager;
        this.platformService = platformService;

        Assert.notNull(aaMOwnerUsername,"aaMOwnerUsername can not be null!");
        this.aaMOwnerUsername = aaMOwnerUsername;

        Assert.notNull(aaMOwnerPassword,"aaMOwnerPassword can not be null!");
        this.aaMOwnerPassword = aaMOwnerPassword;
    }

    public ResponseEntity<ListUserServicesResponse> listUserServices(Principal principal) {

        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();
        ArrayList<Platform> availablePlatforms = new ArrayList<>();
        ArrayList<OwnedService> availableSSPs = new ArrayList<>();
        ArrayList<String> unavailablePlatforms = new ArrayList<>();
        ArrayList<String> unavailableSSPs = new ArrayList<>();
        String responseMessage;
        HttpStatus httpStatus;

        UserManagementRequest ownedPlatformDetailsRequest = new UserManagementRequest(
                new Credentials(aaMOwnerUsername, aaMOwnerPassword),
                new Credentials(user.getUsername(), ""),
                new UserDetails(
                        new Credentials(user.getUsername(), ""),
                        "",
                        UserRole.NULL,
                        new HashMap<>(),
                        new HashMap<>()
                ),
                OperationType.CREATE
        );

        // Get OwnedPlatformDetails from AAM
        try {
            Set<OwnedService> ownedServicesSet =
                    rabbitManager.sendOwnedServiceDetailsRequest(ownedPlatformDetailsRequest);
            if (ownedServicesSet != null) {

                // Distinguish the platforms from SSPs.
                Set<OwnedService> ownedPlatformDetailsSet = new HashSet<>();
                Set<OwnedService> ownedSSPDetailsSet = new HashSet<>();

                // Distinguish Platforms from SSPs
                divideServices(ownedServicesSet, ownedPlatformDetailsSet, ownedSSPDetailsSet);

                // Todo: Get information from Registry regarding SSPs
                availableSSPs.addAll(ownedSSPDetailsSet);

                // Get Platform details from Registry
                getPlatformDetails(ownedPlatformDetailsSet, unavailablePlatforms, availablePlatforms);

                if (unavailablePlatforms.size() == 0 && unavailableSSPs.size() == 0) {
                    responseMessage = "All the owned service details were successfully received";
                    httpStatus = HttpStatus.OK;
                } else {
                    responseMessage = "Could NOT all the service information";
                    httpStatus = HttpStatus.PARTIAL_CONTENT;
                }
            } else {
                responseMessage = "AAM responded with null";
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
                log.warn(responseMessage);
            }
        } catch (CommunicationException e) {
            responseMessage = "AAM threw CommunicationException: " + e.getMessage();
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            log.warn(responseMessage, e);
        }

        ListUserServicesResponse response = new ListUserServicesResponse(responseMessage,
                constructAvailablePlatformDetails(availablePlatforms),
                constructAvailableSSPDetails(availableSSPs),
                unavailablePlatforms,
                unavailableSSPs);
        return new ResponseEntity<>(response, new HttpHeaders(), httpStatus);
    }

    private void divideServices(Set<OwnedService> ownedServicesSet, Set<OwnedService> ownedPlatformDetailsSet,
                                Set<OwnedService> ownedSSPDetailsSet) {
        for (OwnedService ownedService : ownedServicesSet) {
            if (ownedService.getServiceType() == OwnedService.ServiceType.PLATFORM)
                ownedPlatformDetailsSet.add(ownedService);
            else if (ownedService.getServiceType() == OwnedService.ServiceType.SMART_SPACE)
                ownedSSPDetailsSet.add(ownedService);
        }

    }

    private void getPlatformDetails(Set<OwnedService> ownedPlatformDetailsSet, ArrayList<String> unavailablePlatforms,
                                    ArrayList<Platform> availablePlatforms) {
        for (OwnedService platformDetails : ownedPlatformDetailsSet) {
            log.debug("OwnedPlatformDetails: " + ReflectionToStringBuilder.toString(platformDetails));

            // Get Platform information from Registry
            ResponseEntity registryResponse = platformService.getPlatformDetailsFromRegistry(platformDetails.getServiceInstanceId());
            log.debug("registryResponse = " + ReflectionToStringBuilder.toString(registryResponse));

            if (registryResponse.getStatusCode() != HttpStatus.OK)
                unavailablePlatforms.add(platformDetails.getInstanceFriendlyName());
            else if (registryResponse.getStatusCode() == HttpStatus.OK)
                availablePlatforms.add(((PlatformRegistryResponse) registryResponse.getBody()).getBody());
        }
    }

    private ArrayList<PlatformDetails> constructAvailablePlatformDetails(ArrayList<Platform> availablePlatforms) {
        ArrayList<PlatformDetails> availablePlatformDetails = new ArrayList<>();
        for (Platform platform : availablePlatforms) {
            PlatformDetails platformDetails = new PlatformDetails(platform);
            availablePlatformDetails.add(platformDetails);
        }
        return availablePlatformDetails;
    }

    private ArrayList<SSPDetails> constructAvailableSSPDetails(ArrayList<OwnedService> ssps) {
        ArrayList<SSPDetails> availableSSPDetails = new ArrayList<>();
        for (OwnedService ssp : ssps) {
            availableSSPDetails.add(
                    new SSPDetails(
                            ssp.getServiceInstanceId(),
                            ssp.getInstanceFriendlyName(),
                            ssp.getExternalAddress(),
                            ssp.getSiteLocalAddress(),
                            ssp.isExposingSiteLocalAddress()));
        }
        return availableSSPDetails;
    }
}
