package eu.h2020.symbiote.administration.model;

import eu.h2020.symbiote.security.commons.Certificate;

import java.util.Map;

public class UserDetailsDDO {

    private String username;
    private String email;
    private String role;
    private boolean termsAccepted;
    private boolean conditionsAccepted;
    private boolean usernamePermission;
    private boolean emailPermission;
    private boolean publicKeysPermission;
    private boolean jwtPermission;
    private Map<String, Certificate> clients;

    public UserDetailsDDO(String username, String email, String role, boolean termsAccepted,
                          boolean conditionsAccepted, boolean usernamePermission, boolean emailPermission,
                          boolean publicKeysPermission, boolean jwtPermission, Map<String, Certificate> clients) {
        this.username = username;
        this.email = email;
        this.role = role;
        this.termsAccepted = termsAccepted;
        this.conditionsAccepted = conditionsAccepted;
        this.usernamePermission = usernamePermission;
        this.emailPermission = emailPermission;
        this.publicKeysPermission = publicKeysPermission;
        this.jwtPermission = jwtPermission;
        this.clients = clients;
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public boolean isTermsAccepted() { return termsAccepted; }
    public boolean isConditionsAccepted() { return conditionsAccepted; }
    public boolean isUsernamePermission() { return usernamePermission; }
    public boolean isEmailPermission() { return emailPermission; }
    public boolean isPublicKeysPermission() { return publicKeysPermission; }
    public boolean isJwtPermission() { return jwtPermission; }
    public Map<String, Certificate> getClients() { return clients; }

    @Override
    public String toString() {
        return "UserDetailsDDO{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", termsAccepted=" + termsAccepted +
                ", conditionsAccepted=" + conditionsAccepted +
                ", usernamePermission=" + usernamePermission +
                ", emailPermission=" + emailPermission +
                ", publicKeysPermission=" + publicKeysPermission +
                ", jwtPermission=" + jwtPermission +
                ", clients=" + clients +
                '}';
    }
}
