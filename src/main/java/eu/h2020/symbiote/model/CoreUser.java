package eu.h2020.symbiote.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.constraints.Max;
import javax.validation.constraints.Pattern;
import java.util.Collection;
import java.util.ArrayList;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import eu.h2020.symbiote.core.model.Platform;

public class CoreUser extends User {

    @NotNull
    @Size(min=4, max=30)
    private String validUsername;

    @NotNull
    @Size(min=4, max=30)
    private String validPassword;

    @NotNull
    @Size(min=4, max=30)
    private String recoveryMail;

    // @NotNull
    @Size(min=4, max=30)
    private String federatedId;

    // Match either a word (letters, digits and _) with min=4, max=30 characters or an empty string
    // @Pattern(regexp="(^\\Z|^\\w\\w\\w\\w*\\Z)")
    // @Max(30)
    private String platformId;

    private Platform platform;


    public CoreUser(String username, String password, boolean enabled,
        boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked, Collection authorities,
        String platformId) {

     super(username, password, enabled, accountNonExpired,
        credentialsNonExpired, accountNonLocked, authorities);

        this.platformId = platformId;
    }

    public CoreUser () {
        super("placeholder", "placeholder", false, false, false, false, new ArrayList<>());
    }

    public String getValidUsername() {
        return this.validUsername;
    }

    public String getValidPassword() {
        return this.validPassword;
    }

    public String getRecoveryMail() {
        return this.recoveryMail;
    }

    public String getFederatedId() {
        return this.federatedId;
    }

    public String getPlatformId() {
        return this.platformId;
    }

    public Platform getPlatform() {
        return this.platform;
    }

    public void setValidUsername(String validUsername) {
        this.validUsername = validUsername;
    }

    public void setValidPassword(String validPassword) {
        this.validPassword = validPassword;
    }

    public void setRecoveryMail(String recoveryMail) {
        this.recoveryMail = recoveryMail;
    }

    public void setFederatedId(String federatedId) {
        this.federatedId = federatedId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public void clearPassword() {
        this.validPassword = null;
    }

}