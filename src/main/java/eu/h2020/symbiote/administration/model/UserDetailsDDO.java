package eu.h2020.symbiote.administration.model;

public class UserDetailsDDO {

    private String username;
    private String email;
    private String role;

    public UserDetailsDDO(String username, String email, String role) {
        setUsername(username);
        setEmail(email);
        setRole(role);
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    @Override
    public String toString() {
        return "UserDetailsDDO{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
