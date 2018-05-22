package eu.h2020.symbiote.administration.model;

import eu.h2020.symbiote.security.commons.Certificate;

import java.util.Map;

public class UserDetailsDDO {

    private String username;
    private String email;
    private String role;
    private Map<String, Certificate> clients;

    public UserDetailsDDO(String username, String email, String role, Map<String, Certificate> clients) {
        this.username = username;
        this.email = email;
        this.role = role;
        this.clients = clients;
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public Map<String, Certificate> getClients() { return clients; }

    @Override
    public String toString() {
        return "UserDetailsDDO{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
