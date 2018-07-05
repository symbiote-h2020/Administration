package eu.h2020.symbiote.administration.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

public class ResendVerificationEmailRequest {

    @NotNull
    private final String username;

    @NotNull
    private final String password;

    @JsonCreator
    public ResendVerificationEmailRequest(@JsonProperty("username") String username,
                                          @JsonProperty("password") String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }

    @Override
    public String toString() {
        return "ResendVerificationEmailRequest{" +
                "username='" + username + '\'' +
                '}';
    }

}
