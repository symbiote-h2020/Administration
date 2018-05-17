package eu.h2020.symbiote.administration.services.authorization;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.h2020.symbiote.administration.helpers.AuthorizationServiceHelper;
import eu.h2020.symbiote.security.ComponentSecurityHandlerFactory;
import eu.h2020.symbiote.security.accesspolicies.IAccessPolicy;
import eu.h2020.symbiote.security.accesspolicies.common.singletoken.ComponentHomeTokenAccessPolicy;
import eu.h2020.symbiote.security.commons.SecurityConstants;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import eu.h2020.symbiote.security.communication.payloads.SecurityRequest;
import eu.h2020.symbiote.security.handler.IComponentSecurityHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles the authentication and authorization procedures. This class is used as a wrapper around ComponentSecurityHandler.
 * It is mainly used to mock the security during unit tests as well as disabling security if there is a need to.
 *
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/22/2018.
 */
@Service
public class AuthorizationService {
    private static Log log = LogFactory.getLog(AuthorizationService.class);

    private final String FEDERATION_MANAGER_ID = "fm";

    private String componentOwnerName;
    private String componentOwnerPassword;
    private String aamAddress;
    private String clientId;
    private String keystoreName;
    private String keystorePass;
    private Boolean securityEnabled;

    private IComponentSecurityHandler componentSecurityHandler;

    public AuthorizationService(@Value("${aam.deployment.owner.username}") String componentOwnerName,
                                @Value("${aam.deployment.owner.password}") String componentOwnerPassword,
                                @Value("${aam.environment.coreInterfaceAddress}") String aamAddress,
                                @Value("${aam.environment.clientId}") String clientId,
                                @Value("${aam.environment.keystoreName}") String keystoreName,
                                @Value("${aam.environment.keystorePass}") String keystorePass,
                                @Value("${symbIoTe.aam.integration}") Boolean securityEnabled)
            throws SecurityHandlerException {

        Assert.notNull(componentOwnerName,"componentOwnerName can not be null!");
        this.componentOwnerName = componentOwnerName;

        Assert.notNull(componentOwnerPassword,"componentOwnerPassword can not be null!");
        this.componentOwnerPassword = componentOwnerPassword;

        Assert.notNull(aamAddress,"aamAddress can not be null!");
        this.aamAddress = aamAddress;

        Assert.notNull(clientId,"clientId can not be null!");
        this.clientId = clientId;

        Assert.notNull(keystoreName,"keystoreName can not be null!");
        this.keystoreName = keystoreName;

        Assert.notNull(keystorePass,"keystorePass can not be null!");
        this.keystorePass = keystorePass;

        Assert.notNull(securityEnabled,"securityEnabled can not be null!");
        this.securityEnabled = securityEnabled;

        if (securityEnabled)
            enableSecurity();
    }


    public HttpHeaders getHttpHeadersWithSecurityRequest() {
        HttpHeaders httpHeaders = new HttpHeaders();

        if (securityEnabled) {
            try {
                SecurityRequest securityRequest = componentSecurityHandler.generateSecurityRequestUsingLocalCredentials();
                for (Map.Entry<String, String> entry : securityRequest.getSecurityRequestHeaderParams().entrySet()) {
                    httpHeaders.add(entry.getKey(), entry.getValue());
                }

            } catch (SecurityHandlerException | JsonProcessingException e) {
                log.info("Could not create security request", e);
            }
        }

        return httpHeaders;
    }

    public boolean validateServiceResponse(String component, String platformId, HttpHeaders httpHeaders) {
        String serviceResponse = httpHeaders.get(SecurityConstants.SECURITY_RESPONSE_HEADER).get(0);

        if (serviceResponse == null)
            log.warn("There no was service response in the reply of " + component + " platform " + platformId);


        boolean isServiceResponseVerified;
        try {
            isServiceResponseVerified = componentSecurityHandler.
                    isReceivedServiceResponseVerified(serviceResponse, component, platformId);
        } catch (SecurityHandlerException e) {
            log.warn("Exception during verifying service response", e);
            return false;
        }

        if (!isServiceResponseVerified) {
            log.warn("The service response in the reply of " + component + " platform " + platformId + " was not verified");
            return false;
        }

        return true;
    }

    public ResponseEntity checkJoinedFederationsRequest(String platformId, HttpHeaders httpHeaders, String serviceResponse) {
        if (securityEnabled) {
            if (httpHeaders == null) {
                return AuthorizationServiceHelper.addSecurityService(
                        "HttpHeaders are null", new HttpHeaders(),
                        HttpStatus.BAD_REQUEST, serviceResponse);
            }

            SecurityRequest securityRequest;
            try {
                securityRequest = new SecurityRequest(httpHeaders.toSingleValueMap());
                log.debug("Received SecurityRequest of listResources request to be verified: (" + securityRequest + ")");
            } catch (InvalidArgumentsException e) {
                log.info("Could not create the SecurityRequest", e);
                return AuthorizationServiceHelper.addSecurityService(
                        e.getErrorMessage(), new HttpHeaders(),
                        HttpStatus.BAD_REQUEST, serviceResponse);
            }

            boolean checkResult;
            try {
                // Here we check the ComponentHomeTokenAccessPolicy. This has to be modified if we need to satisfy
                // another access policy
                checkResult = checkComponentHomeTokenAccessPolicy(platformId, securityRequest);
            } catch (Exception e) {
                log.info("Could not verify the access policies", e);
                return AuthorizationServiceHelper.addSecurityService(
                        e.getMessage(), new HttpHeaders(),
                        HttpStatus.INTERNAL_SERVER_ERROR, serviceResponse);
            }

            if (checkResult) {
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                return AuthorizationServiceHelper.addSecurityService(
                        "The stored resource access policy was not satisfied",
                        new HttpHeaders(), HttpStatus.UNAUTHORIZED, serviceResponse);
            }
        } else {
            log.debug("checkAccess: Security is disabled");

            // if security is disabled in properties
            return new ResponseEntity<>("Security disabled", HttpStatus.OK);
        }
    }

    /**
     * Generates a service response to be included in the response to the client, so that the server can be authenticated.
     *
     * @return if the status of the returned ResponseEntity is OK, then the service response was successfully created and
     * it is contained in the body. Otherwise, the body includes some indication of the failure.
     */
    public ResponseEntity generateServiceResponse() {
        if (securityEnabled) {
            try {
                String serviceResponse = componentSecurityHandler.generateServiceResponse();
                return new ResponseEntity<>(serviceResponse, HttpStatus.OK);
            } catch (SecurityHandlerException e) {
                log.info("Failed to generate a service response", e);
                return new ResponseEntity<>(e.getErrorMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            String message = "generateServiceResponse: Security is disabled";
            log.debug(message);
            return new ResponseEntity<>(message, HttpStatus.OK);
        }

    }

    /**
     * Enables Security
     *
     * @throws SecurityHandlerException when ComponentSecurityHandler failed to be instantiated
     */
    private void enableSecurity() throws SecurityHandlerException {
        securityEnabled = true;
        componentSecurityHandler = ComponentSecurityHandlerFactory.getComponentSecurityHandler(
                keystoreName,
                keystorePass,
                clientId,
                aamAddress,
                componentOwnerName,
                componentOwnerPassword);

    }

    private boolean checkComponentHomeTokenAccessPolicy(String platformId, SecurityRequest securityRequest)
            throws Exception {
        Map<String, IAccessPolicy> accessPoliciesMap = new HashMap<>();

        accessPoliciesMap.put("ComponentHomeTokenAccessPolicy",
                new ComponentHomeTokenAccessPolicy(platformId, FEDERATION_MANAGER_ID,  new HashMap<>()));
        return componentSecurityHandler.getSatisfiedPoliciesIdentifiers(accessPoliciesMap, securityRequest).size() > 0;
    }
}

