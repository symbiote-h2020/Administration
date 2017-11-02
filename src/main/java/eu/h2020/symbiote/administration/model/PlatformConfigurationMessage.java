package eu.h2020.symbiote.administration.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class PlatformConfigurationMessage {

    @NotNull
    @Size(min=4, max=30)
    String platformId;

    @NotNull
    @Size(min=1)
    String platformOwnerUsername;

    @NotNull
    @Size(min=1)
    String platformOwnerPassword;

    @NotNull
    @Size(min=1)
    String componentsKeystorePassword;

    @NotNull
    @Size(min=1)
    String aamKeystoreName;

    @NotNull
    @Size(min=1, max=7)
    String aamKeystorePassword;

    @NotNull
    @Size(min=1, max=7)
    String aamPrivateKeyPassword;

    @NotNull
    @Size(min=1)
    String sslKeystore;

    @NotNull
    @Size(min=1)
    String sslKeystorePassword;

    @NotNull
    @Size(min=1)
    String sslKeyPassword;


    public PlatformConfigurationMessage() {
    }

    public PlatformConfigurationMessage(String platformId, String platformOwnerUsername, String platformOwnerPassword,
                                        String componentsKeystorePassword, String aamKeystoreName,
                                        String aamKeystorePassword, String aamPrivateKeyPassword, String sslKeystore,
                                        String sslKeystorePassword, String sslKeyPassword) {
        this.platformId = platformId;
        this.platformOwnerUsername = platformOwnerUsername;
        this.platformOwnerPassword = platformOwnerPassword;
        this.componentsKeystorePassword = componentsKeystorePassword;
        this.aamKeystoreName = aamKeystoreName;
        this.aamKeystorePassword = aamKeystorePassword;
        this.aamPrivateKeyPassword = aamPrivateKeyPassword;
        this.sslKeystore = sslKeystore;
        this.sslKeystorePassword = sslKeystorePassword;
        this.sslKeyPassword = sslKeyPassword;
    }


    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getPlatformOwnerUsername() {
        return platformOwnerUsername;
    }

    public void setPlatformOwnerUsername(String platformOwnerUsername) {
        this.platformOwnerUsername = platformOwnerUsername;
    }

    public String getPlatformOwnerPassword() {
        return platformOwnerPassword;
    }

    public void setPlatformOwnerPassword(String platformOwnerPassword) {
        this.platformOwnerPassword = platformOwnerPassword;
    }

    public String getComponentsKeystorePassword() {
        return componentsKeystorePassword;
    }

    public void setComponentsKeystorePassword(String componentsKeystorePassword) {
        this.componentsKeystorePassword = componentsKeystorePassword;
    }

    public String getAamKeystoreName() { return aamKeystoreName; }

    public void setAamKeystoreName(String aamKeystoreName) { this.aamKeystoreName = aamKeystoreName; }

    public String getAamKeystorePassword() {
        return aamKeystorePassword;
    }

    public void setAamKeystorePassword(String aamKeystorePassword) {
        this.aamKeystorePassword = aamKeystorePassword;
    }

    public String getAamPrivateKeyPassword() {
        return aamPrivateKeyPassword;
    }

    public void setAamPrivateKeyPassword(String aamPrivateKeyPassword) {
        this.aamPrivateKeyPassword = aamPrivateKeyPassword;
    }

    public String getSslKeystore() {
        return sslKeystore;
    }

    public void setSslKeystore(String sslKeystore) {
        this.sslKeystore = sslKeystore;
    }

    public String getSslKeystorePassword() {
        return sslKeystorePassword;
    }

    public void setSslKeystorePassword(String sslKeystorePassword) {
        this.sslKeystorePassword = sslKeystorePassword;
    }

    public String getSslKeyPassword() {
        return sslKeyPassword;
    }

    public void setSslKeyPassword(String sslKeyPassword) {
        this.sslKeyPassword = sslKeyPassword;
    }

    @Override
    public String toString() {
        return "PlatformConfigurationMessage{" +
                "platformId='" + platformId + '\'' +
                ", platformOwnerUsername='" + platformOwnerUsername + '\'' +
                ", platformOwnerPassword='" + platformOwnerPassword + '\'' +
                ", componentsKeystorePassword='" + componentsKeystorePassword + '\'' +
                ", aamKeystoreName='" + aamKeystoreName + '\'' +
                ", aamKeystorePassword='" + aamKeystorePassword + '\'' +
                ", aamPrivateKeyPassword='" + aamPrivateKeyPassword + '\'' +
                ", sslKeystore='" + sslKeystore + '\'' +
                ", sslKeystorePassword='" + sslKeystorePassword + '\'' +
                ", sslKeyPassword='" + sslKeyPassword + '\'' +
                '}';
    }
}
