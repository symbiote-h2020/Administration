package eu.h2020.symbiote.administration.services;

import eu.h2020.symbiote.model.mim.Federation;
import eu.h2020.symbiote.model.mim.FederationMember;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class FederationNotificationService {
    private static Log log = LogFactory.getLog(FederationNotificationService.class);

    public static final String FEDERATION_MANAGER_URL = "/fm/federations";
    private static final String FEDERATION_MANAGER_COMPONENT_ID = "fm";

    private RestTemplate restTemplate;
    private AuthorizationService authorizationService;

    @Autowired
    public FederationNotificationService(RestTemplate restTemplate,
                                         AuthorizationService authorizationService) {
        Assert.notNull(restTemplate,"RestTemplate can not be null!");
        this.restTemplate = restTemplate;

        Assert.notNull(authorizationService,"AuthorizationService can not be null!");
        this.authorizationService = authorizationService;
    }

    public void notifyAboutFederationUpdate(Federation federation) {
        notifyAboutFederationUpdate(federation, federation.getMembers());
    }

    public void notifyAboutFederationUpdate(Federation federation, List<FederationMember> memberList) {
        HttpHeaders httpHeaders = authorizationService.getHttpHeadersWithSecurityRequest();
        HttpEntity<Federation> httpEntity = new HttpEntity<>(federation, httpHeaders);

        for (FederationMember member : memberList) {
            ResponseEntity federationNotificationResponse = null;

            try {
                federationNotificationResponse = restTemplate.exchange(
                        member.getInterworkingServiceURL() + FEDERATION_MANAGER_URL,
                        HttpMethod.POST,
                        httpEntity,
                        Object.class
                );
                authorizationService.validateServiceResponse(FEDERATION_MANAGER_COMPONENT_ID, member.getPlatformId(),
                        federationNotificationResponse.getHeaders());
            } catch (RestClientException e) {
                log.warn("Problem while conducting federation managers", e);
            }

        }
    }

    public void notifyAboutFederationDeletion(Federation federation) {
        HttpHeaders httpHeaders = authorizationService.getHttpHeadersWithSecurityRequest();
        HttpEntity<Federation> httpEntity = new HttpEntity<>(federation, httpHeaders);

        for (FederationMember member : federation.getMembers()) {
            ResponseEntity federationNotificationResponse = null;

            try {
                federationNotificationResponse = restTemplate.exchange(
                        member.getInterworkingServiceURL() + FEDERATION_MANAGER_URL + "/" + federation.getId(),
                        HttpMethod.DELETE,
                        httpEntity,
                        Object.class
                );
                authorizationService.validateServiceResponse(FEDERATION_MANAGER_COMPONENT_ID, member.getPlatformId(),
                        federationNotificationResponse.getHeaders());
            } catch (RestClientException e) {
                log.warn("Problem while conducting federation managers", e);
            }

        }
    }
}
