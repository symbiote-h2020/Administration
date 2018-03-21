package eu.h2020.symbiote.administration.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ListUserServicesResponse {

    private String message;
    private List<PlatformDetails> availablePlatforms;
    private List<SSPDetails> availableSSPs;
    private List<String> unavailablePlatforms;
    private List<String> unavailableSSPs;

    @JsonCreator
    public ListUserServicesResponse(@JsonProperty("message") String message,
                                    @JsonProperty("availablePlatforms") List<PlatformDetails> availablePlatforms,
                                    @JsonProperty("availableSSPs") List<SSPDetails> availableSSPs,
                                    @JsonProperty("unavailablePlatforms") List<String> unavailablePlatforms,
                                    @JsonProperty("unavailableSSPs") List<String> unavailableSSPs) {

        this.message = message;
        this.availablePlatforms = availablePlatforms;
        this.availableSSPs = availableSSPs;
        this.unavailablePlatforms = unavailablePlatforms;
        this.unavailableSSPs = unavailableSSPs;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<PlatformDetails> getAvailablePlatforms() { return availablePlatforms; }

    public List<SSPDetails> getAvailableSSPs() { return availableSSPs; }

    public List<String> getUnavailablePlatforms() { return unavailablePlatforms; }

    public List<String> getUnavailableSSPs() { return unavailableSSPs; }
}
