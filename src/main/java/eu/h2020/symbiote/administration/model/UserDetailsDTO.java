package eu.h2020.symbiote.administration.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.security.commons.Certificate;
import eu.h2020.symbiote.security.commons.enums.AccountStatus;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.communication.payloads.UserDetails;

import java.util.Map;

public class UserDetailsDTO {

    private final String username;
    private final String email;
    private final UserRole role;
    private final AccountStatus accountStatus;
    private final boolean termsAccepted;
    private final boolean conditionsAccepted;
    private final boolean analyticsAndResearchConsent;

    private Map<String, Certificate> clients;

    @JsonCreator
    public UserDetailsDTO(@JsonProperty("username") String username,
                          @JsonProperty("email") String email,
                          @JsonProperty("role") UserRole role,
                          @JsonProperty("accountStatus") AccountStatus accountStatus,
                          @JsonProperty("termsAccepted") boolean termsAccepted,
                          @JsonProperty("conditionsAccepted") boolean conditionsAccepted,
                          @JsonProperty("analyticsAndResearchConsent") boolean analyticsAndResearchConsent,
                          @JsonProperty("clients") Map<String, Certificate> clients) {
        this.username = username;
        this.email = email;
        this.role = role;
        this.accountStatus = accountStatus;
        this.termsAccepted = termsAccepted;
        this.conditionsAccepted = conditionsAccepted;
        this.analyticsAndResearchConsent = analyticsAndResearchConsent;
        this.clients = clients;
    }

    public UserDetailsDTO(UserDetails userDetails) {
        this.username = userDetails.getCredentials().getUsername();
        this.email = userDetails.getRecoveryMail();
        this.role = userDetails.getRole();
        this.accountStatus = userDetails.getStatus();
        this.termsAccepted = userDetails.hasGrantedServiceConsent();
        this.conditionsAccepted = userDetails.hasGrantedServiceConsent();
        this.analyticsAndResearchConsent = userDetails.hasGrantedAnalyticsAndResearchConsent();
        this.clients = userDetails.getClients();
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public UserRole getRole() { return role; }
    public AccountStatus getAccountStatus() { return accountStatus; }
    public boolean isTermsAccepted() { return termsAccepted; }
    public boolean isConditionsAccepted() { return conditionsAccepted; }
    public boolean isAnalyticsAndResearchConsent() { return analyticsAndResearchConsent; }
    public Map<String, Certificate> getClients() { return clients; }

    @Override
    public String toString() {
        return "UserDetailsDTO{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", accountStatus=" + accountStatus +
                ", termsAccepted=" + termsAccepted +
                ", conditionsAccepted=" + conditionsAccepted +
                ", analyticsAndResearchConsent=" + analyticsAndResearchConsent +
                ", clients=" + clients +
                '}';
    }
}
