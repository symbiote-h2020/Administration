package eu.h2020.symbiote.administration.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.h2020.symbiote.security.ComponentSecurityHandlerFactory;
import eu.h2020.symbiote.security.commons.SecurityConstants;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import eu.h2020.symbiote.security.communication.payloads.SecurityRequest;
import eu.h2020.symbiote.security.handler.IComponentSecurityHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

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

    /**
     * Enables Security
     *
     * @throws SecurityHandlerException
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
}

