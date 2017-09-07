package eu.h2020.symbiote.administration.model;


import java.util.List;

public class ListUserPlatformsResponse {

    private String message;
    private List<PlatformDetails> availablePlatforms;

    public ListUserPlatformsResponse() {
    }

    public ListUserPlatformsResponse(String message, List<PlatformDetails> availablePlatforms) {
        this.message = message;
        this.availablePlatforms = availablePlatforms;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<PlatformDetails> getAvailablePlatforms() { return availablePlatforms; }
    public void setAvailablePlatforms(List<PlatformDetails> availablePlatforms) { this.availablePlatforms = availablePlatforms; }
}
