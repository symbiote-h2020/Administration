package eu.h2020.symbiote.administration.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class PlatformConfigurationMessage {

    @NotNull
    @Size(min=4, max=30)
    private String platformId;

    @NotNull
    @Size(min=1)
    private String platformOwnerUsername;

    @NotNull
    @Size(min=1)
    private String platformOwnerPassword;

    @NotNull
    private String componentsKeystorePassword;

    @NotNull
    private String aamKeystoreName;

    @NotNull
    @Size(max=7)
    private String aamKeystorePassword;

    // @NotNull
    private String aamPrivateKeyPassword;

    @NotNull
    private Long tokenValidity;

    @NotNull
    private Boolean useBuiltInRapPlugin;

    @NotNull
    private Level level;

    @NotNull
    private DeploymentType deploymentType;

    @JsonCreator
    public PlatformConfigurationMessage(@JsonProperty("platformId") String platformId,
                                        @JsonProperty("platformOwnerUsername") String platformOwnerUsername,
                                        @JsonProperty("platformOwnerPassword") String platformOwnerPassword,
                                        @JsonProperty("componentsKeystorePassword") String componentsKeystorePassword,
                                        @JsonProperty("aamKeystoreName") String aamKeystoreName,
                                        @JsonProperty("aamKeystorePassword") String aamKeystorePassword,
                                        @JsonProperty("aamPrivateKeyPassword") String aamPrivateKeyPassword,
                                        @JsonProperty("tokenValidity") Long tokenValidity,
                                        @JsonProperty("useBuiltInRapPlugin") Boolean useBuiltInRapPlugin,
                                        @JsonProperty("level") Level level,
                                        @JsonProperty("deploymentType") DeploymentType deploymentType) {
        this.platformId = platformId;
        this.platformOwnerUsername = platformOwnerUsername;
        this.platformOwnerPassword = platformOwnerPassword;
        this.componentsKeystorePassword = componentsKeystorePassword;
        this.aamKeystoreName = aamKeystoreName;
        this.aamKeystorePassword = aamKeystorePassword;
        this.aamPrivateKeyPassword = aamPrivateKeyPassword;
        this.tokenValidity = tokenValidity;
        this.useBuiltInRapPlugin = useBuiltInRapPlugin;
        this.level = level;
        this.deploymentType = deploymentType;
    }


    public String getPlatformId() {
        return platformId;
    }
    public String getPlatformOwnerUsername() {
        return platformOwnerUsername;
    }
    public String getPlatformOwnerPassword() {
        return platformOwnerPassword;
    }
    public String getComponentsKeystorePassword() {
        return componentsKeystorePassword;
    }
    public String getAamKeystoreName() { return aamKeystoreName; }
    public String getAamKeystorePassword() {
        return aamKeystorePassword;
    }
    public String getAamPrivateKeyPassword() {
        return aamPrivateKeyPassword;
    }
    public Long getTokenValidity() { return tokenValidity; }
    public Boolean getUseBuiltInRapPlugin() { return useBuiltInRapPlugin; }
    public Level getLevel() { return level; }
    public DeploymentType getDeploymentType() { return deploymentType; }

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
                ", tokenValidity=" + tokenValidity +
                ", useBuiltInRapPlugin=" + useBuiltInRapPlugin +
                ", level=" + level +
                ", deploymentType=" + deploymentType +
                '}';
    }

    public enum Level {
        L1,
        L2,
        L3_4,
        ENABLER
    }

    public enum DeploymentType {
        DOCKER,
        MANUAL
    }
}
