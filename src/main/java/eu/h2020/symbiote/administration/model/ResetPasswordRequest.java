package eu.h2020.symbiote.administration.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

public class ResetPasswordRequest {

    @NotNull
    private final String username;

    @NotNull
    private final String email;

    @JsonCreator
    public ResetPasswordRequest(@JsonProperty("username") String username,
                                @JsonProperty("email") String email) {
        this.username = username;
        this.email = email;
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }

    @Override
    public String toString() {
        return "ResetPasswordRequest{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
