package eu.h2020.symbiote.administration.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.Email;

import javax.validation.Valid;

public class ChangeEmailRequest {

    @Valid
    @Email
    private String newEmail;

    @Valid
    @Email
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
