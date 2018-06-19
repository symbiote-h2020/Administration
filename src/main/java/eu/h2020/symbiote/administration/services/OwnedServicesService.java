package eu.h2020.symbiote.administration.services;

import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.administration.model.*;
import eu.h2020.symbiote.core.cci.PlatformRegistryResponse;
import eu.h2020.symbiote.core.cci.SspRegistryResponse;
import eu.h2020.symbiote.model.mim.Platform;
import eu.h2020.symbiote.model.mim.SmartSpace;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OwnedServicesService {

    private static Log log = LogFactory.getLog(OwnedServicesService.class);

    private final RabbitManager rabbitManager;
    private final PlatformService platformService;
    private final SSPService sspService;
    private final String aaMOwnerUsername;
    private final String aaMOwnerPassword;

    @Autowired
    public OwnedServicesService(RabbitManager rabbitManager,
                                PlatformService platformService,
                                SSPService sspService,
                                @Value("${aam.deployment.owner.username}") String aaMOwnerUsername,
                                @Value("${aam.deployment.owner.password}") String aaMOwnerPassword) {

        this.rabbitManager = rabbitManager;
        this.platformService = platformService;
        this.sspService = sspService;

        Assert.notNull(aaMOwnerUsername,"aaMOwnerUsername can not be null!");
        this.aaMOwnerUsername = aaMOwnerUsername;

        Assert.notNull(aaMOwnerPassword,"aaMOwnerPassword can not be null!");
        this.aaMOwnerPassword = aaMOwnerPassword;
    }

    public ResponseEntity<ListUserServicesResponse> listUserServices(Principal principal) {

        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();
        ArrayList<Platform> availablePlatforms = new ArrayList<>();
        ArrayList<SmartSpace> availableSSPs = new ArrayList<>();
        ArrayList<String> unavailablePlatforms = new ArrayList<>();
        ArrayList<String> unavailableSSPs = new ArrayList<>();
        Set<OwnedService> ownedPlatformDetailsSet = new HashSet<>();
        Set<OwnedService> ownedSSPDetailsSet = new HashSet<>();
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

                // Distinguish Platforms from SSPs
                divideServices(ownedServicesSet, ownedPlatformDetailsSet, ownedSSPDetailsSet);

                // Get Platform details from Registry
                getPlatformDetails(ownedPlatformDetailsSet, unavailablePlatforms, availablePlatforms);

                // Todo: Get Platform details from Registry
                getSSPDetails(ownedSSPDetailsSet, unavailableSSPs, availableSSPs);

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
                constructAvailableSSPDetails(ownedSSPDetailsSet, availableSSPs),
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

    private void getSSPDetails(Set<OwnedService> ownedSSPDetailsSet, ArrayList<String> unavailableSSPs,
                               ArrayList<SmartSpace> availableSmartSpaces) {
        for (OwnedService sspDetails : ownedSSPDetailsSet) {
            log.debug("ownedSSPDetailsSet: " + ReflectionToStringBuilder.toString(sspDetails));

            // Get SSP information from Registry
            ResponseEntity registryResponse = sspService.getSSPDetailsFromRegistry(sspDetails.getServiceInstanceId());
            log.debug("registryResponse = " + ReflectionToStringBuilder.toString(registryResponse));

            if (registryResponse.getStatusCode() != HttpStatus.OK)
                unavailableSSPs.add(sspDetails.getInstanceFriendlyName());
            else if (registryResponse.getStatusCode() == HttpStatus.OK)
                availableSmartSpaces.add(((SspRegistryResponse) registryResponse.getBody()).getBody());
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

    private ArrayList<SSPDetails> constructAvailableSSPDetails(Set<OwnedService> ssps, ArrayList<SmartSpace> availableSSPs) {
        ArrayList<SSPDetails> availableSSPDetails = new ArrayList<>();
        Map<String, SmartSpace> sspMap = availableSSPs.stream().collect(
                Collectors.toMap(SmartSpace::getId, x -> x));

        for (OwnedService ssp : ssps) {
            SmartSpace smartSpace = sspMap.get(ssp.getServiceInstanceId());

            if (smartSpace != null) {
                availableSSPDetails.add(
                        new SSPDetails(
                                ssp.getServiceInstanceId(),
                                ssp.getInstanceFriendlyName(),
                                smartSpace.getDescription().stream()
                                        .map(description -> new Description(description)).collect(Collectors.toList()),
                                ssp.getExternalAddress(),
                                ssp.getSiteLocalAddress(),
                                smartSpace.getInterworkingServices() != null && smartSpace.getInterworkingServices().get(0) != null
                                        ? smartSpace.getInterworkingServices().get(0).getInformationModelId() : null,
                                ssp.isExposingSiteLocalAddress()));
            }
        }
        return availableSSPDetails;
    }
}
