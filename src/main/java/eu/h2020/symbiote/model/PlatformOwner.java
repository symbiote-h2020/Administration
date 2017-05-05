package eu.h2020.symbiote.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;


/**
 * Platform Owner
 * <p>
 * This class is used to validate platform owner registration requests.
 */
public class PlatformOwner {

    public PlatformOwner (String username, String password, String recoveryMail, String federatedId, String platformId) {
        
        this.username = username;
        this.password = password;
        this.recoveryMail = recoveryMail;
        this.federatedId = federatedId;
        this.platformId = platformId;
    }

    public PlatformOwner () {

    }

    @NotNull
    @Size(min=4, max=30)
    private String username;

    @NotNull
    @Size(min=4, max=30)
    private String password;

    @NotNull
    @Size(min=4, max=30)
    private String recoveryMail;

    // @NotNull
    @Size(min=4, max=30)
    private String federatedId;

    @Size(min=4, max=30)
    private String platformId;


    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
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

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
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

}