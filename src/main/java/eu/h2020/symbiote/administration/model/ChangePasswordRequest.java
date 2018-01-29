package eu.h2020.symbiote.administration.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ChangePasswordRequest {

    private String oldPassword;

    @NotNull
    @Size(min=4, max=30)
    private String newPassword;

    @NotNull
    @Size(min=4, max=30)
    private String newPasswordRetyped;

    @JsonCreator
    public ChangePasswordRequest(@JsonProperty("oldPassword") String oldPassword,
                                 @JsonProperty("newPassword") String newPassword,
                                 @JsonProperty("newPasswordRetyped") String newPasswordRetyped) {
        setOldPassword(oldPassword);
        setNewPassword(newPassword);
        setNewPasswordRetyped(newPasswordRetyped);
    }

    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getNewPasswordRetyped() { return newPasswordRetyped; }
    public void setNewPasswordRetyped(String newPasswordRetyped) { this.newPasswordRetyped = newPasswordRetyped; }

    @Override
    public String toString() {
        return "ChangePasswordRequest{" +
                "oldPassword='" + oldPassword + '\'' +
                ", newPassword='" + newPassword + '\'' +
                ", newPasswordRetyped='" + newPasswordRetyped + '\'' +
                '}';
    }
}
