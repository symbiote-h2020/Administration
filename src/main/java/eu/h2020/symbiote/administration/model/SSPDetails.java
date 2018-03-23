package eu.h2020.symbiote.administration.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class SSPDetails {

    @NotNull
    @Pattern(regexp="^(\\Z|[\\w-]{4,})$")
    @Size(max=30)
    private final String id;

    @NotNull
    @Size(min=3, max=30)
    private final String name;

    @NotNull
    @Pattern(regexp = "^(https:\\/\\/www\\.|https:\\/\\/)?[a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*\\." +
            "[a-z]{2,5}(:[0-9]{1,5})?(\\/.*)?$")
    private final String externalAddress;

    @NotNull
    @Pattern(regexp = "^(https:\\/\\/www\\.|https:\\/\\/)?[a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*\\." +
            "[a-z]{2,5}(:[0-9]{1,5})?(\\/.*)?$")
    private final String siteLocalAddress;

    @NotNull
    private final Boolean exposingSiteLocalAddress;

    /**
     *
     * @param id                            SymbIoTe-unique service identifier
     * @param name                          the name of the SSP
     * @param externalAddress               address where the Smart Space AAM is available from the Internet
     * @param siteLocalAddress              address where the Smart Space AAM is available for clients residing in the
     *                                      same network that the server (e.g. local WiFi of a smart space)
     * @param exposingSiteLocalAddress      should siteLocalAddress be exposed
     */
    @JsonCreator
    public SSPDetails(@JsonProperty("id") String id,
                      @JsonProperty("name") String name,
                      @JsonProperty("externalAddress") String externalAddress,
                      @JsonProperty("siteLocalAddress") String siteLocalAddress,
                      @JsonProperty("exposingSiteLocalAddress") Boolean exposingSiteLocalAddress) {
        this.id = id;
        this.name = name;
        this.externalAddress = externalAddress != null ? externalAddress.replaceFirst("\\/+$", "") : null;
        this.siteLocalAddress = siteLocalAddress != null ? siteLocalAddress.replaceFirst("\\/+$", "") : null;
        this.exposingSiteLocalAddress = exposingSiteLocalAddress;
    }

    public String getId() { return id; }

    public String getName() { return name; }

    public String getExternalAddress() { return externalAddress; }

    public String getSiteLocalAddress() { return siteLocalAddress; }

    public Boolean getExposingSiteLocalAddress() { return exposingSiteLocalAddress; }

}
