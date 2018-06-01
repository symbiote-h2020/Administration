package eu.h2020.symbiote.administration.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

public class ChangePermissions {

    @NotNull
    private boolean usernamePermission;

    @NotNull
    private boolean emailPermission;

    @NotNull
    private boolean publicKeysPermission;

    @NotNull
    private boolean jwtPermission;

    @JsonCreator
    public ChangePermissions(@JsonProperty("usernamePermission") boolean usernamePermission,
                             @JsonProperty("emailPermission") boolean emailPermission,
                             @JsonProperty("publicKeysPermission") boolean publicKeysPermission,
                             @JsonProperty("jwtPermission") boolean jwtPermission) {
        this.usernamePermission = usernamePermission;
        this.emailPermission = emailPermission;
        this.publicKeysPermission = publicKeysPermission;
        this.jwtPermission = jwtPermission;
    }

    public boolean isUsernamePermission() { return usernamePermission; }
    public boolean isEmailPermission() { return emailPermission; }
    public boolean isPublicKeysPermission() { return publicKeysPermission; }
    public boolean isJwtPermission() { return jwtPermission; }
}
