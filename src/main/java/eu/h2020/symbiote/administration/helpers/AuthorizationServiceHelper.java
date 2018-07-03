package eu.h2020.symbiote.administration.helpers;

import eu.h2020.symbiote.administration.services.authorization.AuthorizationService;
import eu.h2020.symbiote.security.commons.SecurityConstants;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;

/**
 * This class includes helper functions for handling security operations. There is no need to be mocked during unit tests
 * since it uses AuthorizationService is which mocked.
 *
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/24/2018.
 */
public class AuthorizationServiceHelper {

    /**
     * Checks security request and creates service response
     * @param authorizationService the authorization service
     * @param platformId the platform id where the federation manager belongs
     * @param httpHeaders the httpHeaders of the client's request
     * @return if the status of the returned ResponseEntity is OK, then the access policies were satisfied, the
     * service response was successfully created and the body of the ResponseEntity contains the service response.
     * Otherwise, the body includes some indication of the failure
     */
    public static ResponseEntity checkJoinedFederationsRequestAndCreateServiceResponse(
            AuthorizationService authorizationService,
            String platformId,
            HttpHeaders httpHeaders) {

        // Create the service response. If it fails, return appropriate error since there is no need to continue
        ResponseEntity serviceResponseResult = authorizationService.generateServiceResponse();
        if (serviceResponseResult.getStatusCode() != HttpStatus.valueOf(200))
            return serviceResponseResult;

        // Check the proper security headers. If the check fails, return appropriate error since there is no need to continue
        ResponseEntity checkListResourcesRequestValidity = authorizationService
                .checkJoinedFederationsRequest(platformId, httpHeaders, (String) serviceResponseResult.getBody());

        return checkListResourcesRequestValidity.getStatusCode() != HttpStatus.OK ?
                checkListResourcesRequestValidity :
                serviceResponseResult;
    }

    /**
     * Adds the security service in the reply sent to the client
     * @param response the response sent to the client
     * @param httpHeaders the httpHeaders sent to the client
     * @param httpStatus the httpStatus sent to the client
     * @param serviceResponse the service response created by the authorizationService
     * @return the response with the service response integrated to the headers
     */
    public static ResponseEntity addSecurityService(Object response, HttpHeaders httpHeaders,
                                                    HttpStatus httpStatus, String serviceResponse) {
        httpHeaders.put(SecurityConstants.SECURITY_RESPONSE_HEADER, Collections.singletonList(serviceResponse));
        return new ResponseEntity<>(response, httpHeaders, httpStatus);
    }
}
