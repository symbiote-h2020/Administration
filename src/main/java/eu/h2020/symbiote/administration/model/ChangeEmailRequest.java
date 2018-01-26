package eu.h2020.symbiote.administration.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;

public class ChangeEmailRequest {

    @Valid
    @Pattern(regexp = "^(([^<>()\\[\\]\\\\.,;:\\s@\"]+(\\.[^<>()\\[\\]\\\\.,;:\\s@\"]+)" +
            "*)|(\".+\"))@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}])|" +
            "(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$")
    private String newEmail;

    @Valid
    @Pattern(regexp = "^(([^<>()\\[\\]\\\\.,;:\\s@\"]+(\\.[^<>()\\[\\]\\\\.,;:\\s@\"]+)" +
            "*)|(\".+\"))@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}])|" +
            "(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$")
    private String newEmailRetyped;

    @JsonCreator
    public ChangeEmailRequest(@JsonProperty("newEmail") String newEmail,
                              @JsonProperty("newEmailRetyped") String newEmailRetyped) {
        setNewEmail(newEmail);
        setNewEmailRetyped(newEmailRetyped);
    }

    public String getNewEmail() { return newEmail; }
    public void setNewEmail(String newEmail) { this.newEmail = newEmail; }

    public String getNewEmailRetyped() { return newEmailRetyped; }
    public void setNewEmailRetyped(String newEmailRetyped) { this.newEmailRetyped = newEmailRetyped; }
}
