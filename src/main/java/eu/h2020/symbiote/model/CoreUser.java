package eu.h2020.symbiote.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.constraints.Pattern;
import java.util.Collection;
import java.util.ArrayList;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import eu.h2020.symbiote.model.PlatformDetails;
import eu.h2020.symbiote.security.token.Token;

public class CoreUser extends User {

    public static final int ERROR = 0;
    public static final int APP = 1;
    public static final int PLATFORM_INACTIVE = 2;
    public static final int PLATFORM_ACTIVE = 3;

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
    // @Size(min=4, max=30)
    private String federatedId;

    // Match either a word (letters, digits, "-" and "_") with min=4, max=30 characters or an empty string
    @Pattern(regexp="(^\\Z|^[\\w-][\\w-][\\w-][\\w-]+\\Z)")
    @Size(max=30)
    private String platformId;

    @NotNull
    @Size(min=4, max=30)
    private String platformName;

    @NotNull
    // @Size(min=4, max=120)
    private String platformUrl;

    private int state;

    private PlatformDetails platformDetails;

    private Token token;


    public CoreUser(String username, String password, boolean enabled,
        boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked, Collection authorities,
        Token token, String platformId) {

     super(username, password, enabled, accountNonExpired,
        credentialsNonExpired, accountNonLocked, authorities);

        this.token = token;

        if(platformId == null){

            this.state = this.APP;
        } else {

            this.state = this.PLATFORM_INACTIVE;
            this.platformId = platformId;
        }

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

    public String getPlatformName() {
        return this.platformName;
    }

    public String getPlatformUrl() {
        return this.platformUrl;
    }

    public int getState() {
        return this.state;
    }

    public PlatformDetails getPlatformDetails() {
        return this.platformDetails;
    }

    public Token getToken() {
        return this.token;
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

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public void setPlatformUrl(String platformUrl) {
        this.platformUrl = platformUrl;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setPlatformDetails(PlatformDetails platformDetails) {
        this.platformDetails = platformDetails;
    }

    public void setToken(Token token) {
        this.token = token;
    }


    public void clearPassword() {
        this.validPassword = null;
    }


    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

}