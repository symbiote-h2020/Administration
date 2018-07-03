package eu.h2020.symbiote.administration.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.security.commons.Certificate;

import java.util.Map;

public class UserDetailsDTO {

    private final String username;
    private final String email;
    private final String role;
    private final boolean termsAccepted;
    private final boolean conditionsAccepted;
    private final boolean analyticsAndResearchConsent;

    private Map<String, Certificate> clients;

    @JsonCreator
    public UserDetailsDTO(@JsonProperty("username") String username,
                          @JsonProperty("email") String email,
                          @JsonProperty("role") String role,
                          @JsonProperty("termsAccepted") boolean termsAccepted,
                          @JsonProperty("conditionsAccepted") boolean conditionsAccepted,
                          @JsonProperty("analyticsAndResearchConsent") boolean analyticsAndResearchConsent,
                          @JsonProperty("clients") Map<String, Certificate> clients) {
        this.username = username;
        this.email = email;
        this.role = role;
        this.termsAccepted = termsAccepted;
        this.conditionsAccepted = conditionsAccepted;
        this.analyticsAndResearchConsent = analyticsAndResearchConsent;
        this.clients = clients;
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public boolean isTermsAccepted() { return termsAccepted; }
    public boolean isConditionsAccepted() { return conditionsAccepted; }
    public boolean isAnalyticsAndResearchConsent() { return analyticsAndResearchConsent; }
    public Map<String, Certificate> getClients() { return clients; }

    @Override
    public String toString() {
        return "UserDetailsDTO{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", termsAccepted=" + termsAccepted +
                ", conditionsAccepted=" + conditionsAccepted +
                ", analyticsAndResearchConsent=" + analyticsAndResearchConsent +
                ", clients=" + clients +
                '}';
    }
}
