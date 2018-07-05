package eu.h2020.symbiote.administration.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GmailCredentials {

    private final String userEmail;
    private final String clientId;
    private final String clientSecret;
    private final String accessToken;
    private final String refreshToken;

    public GmailCredentials(@Value("${symbiote.core.administration.email.userEmail}") String userEmail,
                            @Value("${symbiote.core.administration.email.clientId}") String clientId,
                            @Value("${symbiote.core.administration.email.clientSecret}") String clientSecret,
                            @Value("${symbiote.core.administration.email.accessToken}") String accessToken,
                            @Value("${symbiote.core.administration.email.refreshToken}") String refreshToken) {
        this.userEmail = userEmail;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getUserEmail() { return userEmail; }
    public String getClientId() { return clientId; }
    public String getClientSecret() { return clientSecret; }
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
}