package eu.h2020.symbiote.administration.model;

import eu.h2020.symbiote.model.mim.InterworkingService;
import eu.h2020.symbiote.model.mim.Platform;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;


/**
 * Class for a symbIoTe platform secondary details entity, used in form validation and messages
 *
 * @author Vasileios Glykantzis (ICOM)
 */
public class PlatformDetails {

    @NotNull
    @Pattern(regexp="^(\\Z|[\\w-]{4,})$", message = "{validation.service.id}")
    @Size(max=30)
    private String id;

    @NotNull
    @Size(min=3, max=30)
    private String name;

    @Valid
    private List<Description> description;

    @Valid
    private List<InterworkingService> interworkingServices;

    @NotNull
    private Boolean isEnabler;

    /**
     * Empty constructor
     */
    public PlatformDetails() {
    }

    /**
     *
     * @param id                            platform id
     * @param name                          platform name
     * @param description                   platform descriptions
     * @param interworkingServices          list of interworking services
     * @param isEnabler                     specify if it is an enabler
     */
    public PlatformDetails(String id, String name, List<Description> description,
                           List<InterworkingService> interworkingServices, Boolean isEnabler) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.interworkingServices = interworkingServices;
        this.isEnabler = isEnabler;
    }

    /**
     *
     * @param platform the platform details sent by registry
     */
    public PlatformDetails(Platform platform) {
        this.id = platform.getId();
        this.name = platform.getName();
        this.isEnabler = platform.isEnabler();
        this.interworkingServices = new ArrayList<>(platform.getInterworkingServices());

        ArrayList<Description> descriptions = new ArrayList<>();
        for (String description : platform.getDescription())
            descriptions.add(new Description(description));
        this.description = descriptions;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<Description> getDescription() { return description; }
    public void setDescription(List<Description> description) { this.description = description; }

    public List<InterworkingService> getInterworkingServices() { return interworkingServices; }
    public void setInterworkingServices(List<InterworkingService> interworkingServices) { this.interworkingServices = interworkingServices; }

    public Boolean getIsEnabler() { return isEnabler; }
    public void setIsEnabler(Boolean isEnabler) { this.isEnabler = isEnabler; }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}