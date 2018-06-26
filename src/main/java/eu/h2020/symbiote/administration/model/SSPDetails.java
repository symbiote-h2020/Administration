package eu.h2020.symbiote.administration.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.model.mim.SmartSpace;
import eu.h2020.symbiote.security.communication.payloads.OwnedService;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

public class SSPDetails {

    @NotNull
    @Pattern(regexp="^(\\Z|[\\w-]{4,})$", message = "{validation.service.id}")
    @Size(max=30)
    private final String id;

    @NotNull
    @Size(min=3, max=30)
    private final String name;

    @Valid
    private List<Description> description;

    @NotNull
    @Pattern(regexp = "^(\\Z|((https://www\\.|https://)?[a-z0-9]+([\\-.]{1}[a-z0-9]+)*\\." +
            "[a-z]{2,5}(:[0-9]{1,5})?(/.*)?))$", message = "{validation.https.url}")
    private final String externalAddress;

    @NotNull
    @Pattern(regexp = "^(\\Z|((https://www\\.|https://)?[a-z0-9]+([\\-.]{1}[a-z0-9]+)*\\." +
            "[a-z]{2,5}(:[0-9]{1,5})?(/.*)?))$", message = "{validation.https.url}")
    private final String siteLocalAddress;

    @NotNull
    private final String informationModelId;

    @NotNull
    private final Boolean exposingSiteLocalAddress;

    /**
     *
     * @param id                            SymbIoTe-unique service identifier
     * @param name                          the name of the SSP
     * @param description                   the description of the SSP
     * @param externalAddress               address where the Smart Space AAM is available from the Internet
     * @param siteLocalAddress              address where the Smart Space AAM is available for clients residing in the
     *                                      same network that the server (e.g. local WiFi of a smart space)
     * @param informationModelId            the information model id used by the SSP
     * @param exposingSiteLocalAddress      should siteLocalAddress be exposed
     */
    @JsonCreator
    public SSPDetails(@JsonProperty("id") String id,
                      @JsonProperty("name") String name,
                      @JsonProperty("description") List<Description> description,
                      @JsonProperty("externalAddress") String externalAddress,
                      @JsonProperty("siteLocalAddress") String siteLocalAddress,
                      @JsonProperty("informationModelId") String informationModelId,
                      @JsonProperty("exposingSiteLocalAddress") Boolean exposingSiteLocalAddress) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.externalAddress = externalAddress != null ? externalAddress.replaceFirst("/+$", "") : null;
        this.siteLocalAddress = siteLocalAddress != null ? siteLocalAddress.replaceFirst("/+$", "") : null;
        this.informationModelId = informationModelId;
        this.exposingSiteLocalAddress = exposingSiteLocalAddress;
    }

    public SSPDetails(SmartSpace smartSpace, OwnedService sspService) {
        this.id = smartSpace.getId();
        this.name = smartSpace.getName();
        this.externalAddress = sspService.getExternalAddress();
        this.siteLocalAddress = sspService.getSiteLocalAddress();

        this.informationModelId = smartSpace.getInterworkingServices() != null &&
                smartSpace.getInterworkingServices().get(0) != null ?
                smartSpace.getInterworkingServices().get(0).getInformationModelId() : null;
        this.exposingSiteLocalAddress = sspService.isExposingSiteLocalAddress();

        ArrayList<Description> descriptions = new ArrayList<>();
        for (String description : smartSpace.getDescription())
            descriptions.add(new Description(description));
        this.description = descriptions;
    }

    public String getId() { return id; }

    public String getName() { return name; }

    public List<Description> getDescription() { return description; }

    public String getExternalAddress() { return externalAddress; }

    public String getSiteLocalAddress() { return siteLocalAddress; }

    public String getInformationModelId() { return informationModelId; }

    public Boolean getExposingSiteLocalAddress() { return exposingSiteLocalAddress; }

}
