package eu.h2020.symbiote.administration.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ServerInformation {
    private final String name;
    private final String dataProtectionOrganization;
    private final String address;
    private final String country;
    private final String phoneNumber;
    private final String email;
    private final String website;

    @JsonCreator
    public ServerInformation(@JsonProperty("name") String name,
                             @JsonProperty("dataProtectionOrganization") String dataProtectionOrganization,
                             @JsonProperty("address") String address,
                             @JsonProperty("country") String country,
                             @JsonProperty("phoneNumber") String phoneNumber,
                             @JsonProperty("email") String email,
                             @JsonProperty("website") String website) {
        this.name = name;
        this.dataProtectionOrganization = dataProtectionOrganization;
        this.address = address;
        this.country = country;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.website = website;
    }

    public String getName() { return name; }
    public String getDataProtectionOrganization() { return dataProtectionOrganization; }
    public String getCountry() { return country; }
    public String getAddress() { return address; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getEmail() { return email; }
    public String getWebsite() { return website; }
}
