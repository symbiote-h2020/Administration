package eu.h2020.symbiote.administration.services;

import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.administration.model.*;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.communication.payloads.Credentials;
import eu.h2020.symbiote.security.communication.payloads.FederationRule;
import eu.h2020.symbiote.security.communication.payloads.FederationRuleManagementRequest;
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

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FederationService {
    private static Log log = LogFactory.getLog(FederationService.class);

    private RabbitManager rabbitManager;
    private PlatformService platformService;
    private ValidationService validationService;
    private String aaMOwnerUsername;
    private String aaMOwnerPassword;

    @Autowired
    public FederationService(RabbitManager rabbitManager, PlatformService platformService,
                             ValidationService validationService,
                             @Value("${aam.deployment.owner.username}") String aaMOwnerUsername,
                             @Value("${aam.deployment.owner.password}") String aaMOwnerPassword) {
        Assert.notNull(rabbitManager,"RabbitManager can not be null!");
        this.rabbitManager = rabbitManager;

        Assert.notNull(platformService,"PlatformService can not be null!");
        this.platformService = platformService;

        Assert.notNull(validationService,"ValidationService can not be null!");
        this.validationService = validationService;

        Assert.notNull(aaMOwnerUsername,"aaMOwnerUsername can not be null!");
        this.aaMOwnerUsername = aaMOwnerUsername;

        Assert.notNull(aaMOwnerPassword,"aaMOwnerPassword can not be null!");
        this.aaMOwnerPassword = aaMOwnerPassword;
    }

    public ResponseEntity<?> listFederations(Principal principal) {
        return getFederationDetails("");
    }

    public ResponseEntity<?> createFederation(CreateFederationRequest createFederationRequest,
                                              BindingResult bindingResult) {

        Map<String, Object> responseBody = new HashMap<>();

        if (bindingResult.hasErrors())
            return validationService.getRequestErrors(bindingResult);

        Set<String> platformIds = createFederationRequest.getPlatforms().stream()
                .map(PlatformId::getId).collect(Collectors.toSet());
        log.debug("Platform to form the Federation with id = " + createFederationRequest.getId() + " :\n"
                + platformIds.stream().collect(Collectors.joining(", ")));

        FederationRuleManagementRequest request = new FederationRuleManagementRequest(
                new Credentials(aaMOwnerUsername, aaMOwnerPassword),
                createFederationRequest.getId(),
                platformIds,
                FederationRuleManagementRequest.OperationType.CREATE
        );

        try {
            Map<String, FederationRule> aamResponse =
                    rabbitManager.sendCreateFederationRequest(request);

            if (aamResponse != null) {
                if (aamResponse.size() == 1) {
                    Map.Entry<String, FederationRule> entry = aamResponse.entrySet().iterator().next();

                    if (entry.getValue().getPlatformIds().equals(platformIds)) {
                        responseBody.put("message", "Federation Registration was successful!");
                        responseBody.put("federationRule", entry.getValue());
                        return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.CREATED);

                    } else {
                        String message = "Not all the platforms ids present in AAM response";
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

    public ResponseEntity<?> deleteFederation(String federationIdToDelete) {

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
                    rabbitManager.sendDeleteFederationRequest(request);

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

    public ResponseEntity<?> leaveFederation(String federationId, String platformId, Principal principal, boolean isAdmin) {
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();

        if (!isAdmin) {
            ResponseEntity<?> ownedPlatformDetailsResponse = platformService.checkIfUserOwnsPlatform(platformId, user);
            if (ownedPlatformDetailsResponse.getStatusCode() != HttpStatus.OK)
                return ownedPlatformDetailsResponse;
        }

        ResponseEntity<?> federationDetailsResponse = getFederationDetails(federationId);
        if (federationDetailsResponse.getStatusCode() != HttpStatus.OK)
            return federationDetailsResponse;

        FederationRule federationRule = ((Map<String, FederationRule>) federationDetailsResponse.getBody()).entrySet().iterator().next().getValue();

        ResponseEntity<?> isPlatformMember =  isPlatformMemberOfFederation(federationRule, platformId);
        if (isPlatformMember.getStatusCode() != HttpStatus.OK)
            return isPlatformMember;

        ResponseEntity<?> isPlatformTheOnlyMember =  isPlatformTheOnlyMemberOfFederation(federationRule, platformId);
        if (isPlatformTheOnlyMember.getStatusCode() != HttpStatus.OK)
            return isPlatformTheOnlyMember;

        federationRule.getPlatformIds().remove(platformId);

        return sendUpdateFederationRequest(federationRule);
    }


    private ResponseEntity<?> getFederationDetails(String ruleId) {

        Map<String, Object> responseBody = new HashMap<>();

        FederationRuleManagementRequest request = new FederationRuleManagementRequest(
                new Credentials(aaMOwnerUsername, aaMOwnerPassword),
                ruleId,
                new HashSet<>(),
                FederationRuleManagementRequest.OperationType.READ
        );

        Map<String, FederationRule> aamResponse;
        try {
            aamResponse = rabbitManager.sendReadFederationRequest(request);

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

    private ResponseEntity<?> isPlatformMemberOfFederation(FederationRule federationRule, String platformId) {
        if (federationRule.getPlatformIds().contains(platformId))
            return new ResponseEntity<>(new HttpHeaders(), HttpStatus.OK);

        Map<String, Object> responseBody = new HashMap<>();
        String message = "Platform " + platformId + " is not a member of federation " + federationRule.getFederationId();
        log.warn(message);
        responseBody.put("error", message);
        return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST);

    }

    private ResponseEntity<?> isPlatformTheOnlyMemberOfFederation(FederationRule federationRule, String platformId) {
        if (federationRule.getPlatformIds().size() > 1)
            return new ResponseEntity<>(new HttpHeaders(), HttpStatus.OK);

        Map<String, Object> responseBody = new HashMap<>();
        String message = "Platform " + platformId + " is the only a member of federation " + federationRule.getFederationId() +
                ". Please, delete the federation";
        log.warn(message);
        responseBody.put("error", message);
        return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST);

    }

    private ResponseEntity<?> sendUpdateFederationRequest(FederationRule federationRule) {
        Map<String, Object> responseBody = new HashMap<>();

        FederationRuleManagementRequest request = new FederationRuleManagementRequest(
                new Credentials(aaMOwnerUsername, aaMOwnerPassword),
                federationRule.getFederationId(),
                federationRule.getPlatformIds(),
                FederationRuleManagementRequest.OperationType.UPDATE
        );

        try {
            Map<String, FederationRule> aamResponse =
                    rabbitManager.sendUpdateFederationRequest(request);

            if (aamResponse != null) {
                if (aamResponse.size() == 1) {
                    Map.Entry<String, FederationRule> entry = aamResponse.entrySet().iterator().next();

                    if (entry.getValue().getPlatformIds().equals(federationRule.getPlatformIds())) {
                        responseBody.put("message", "Federation Registration was successful!");
                        responseBody.put("federationRule", entry.getValue());
                        return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.OK);

                    } else {
                        String message = "Not all the platforms ids present in AAM response";
                        responseBody.put("error", message);
                        return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST);
                    }
                } else {
                    String message = "Contains " + aamResponse.size() + " Federation rules";
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

    /**
     * Used for testing
     */
    public void setRabbitManager(RabbitManager rabbitManager){
        this.rabbitManager = rabbitManager;
    }
}
