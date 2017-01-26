package eu.h2020.symbiote.model;

/**
 * Class used to describe platform instance.
 * Used for Rabbit messaging.
 */
public class Platform {
    private String platformId;
    private String name;
    private String description;
    private String url;
    private String informationModelId;

    public Platform() {

    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getInformationModelId() {
        return informationModelId;
    }

    public void setInformationModelId(String informationModelId) {
        this.informationModelId = informationModelId;
    }

    @Override
    public String toString() {
        return "Platform{" +
                "platformId='" + platformId + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", url='" + url + '\'' +
                ", informationModelId='" + informationModelId + '\'' +
                '}';
    }
}
