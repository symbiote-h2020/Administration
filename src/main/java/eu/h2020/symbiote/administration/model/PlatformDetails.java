package eu.h2020.symbiote.administration.model;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import eu.h2020.symbiote.core.model.InterworkingService;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import java.util.List;


/**
 * Class for a symbIoTe platform secondary details entity, used in form validation and messages
 *
 * @author Vasileios Glykantzis (ICOM)
 */
public class PlatformDetails {

    @NotNull
    @Pattern(regexp="(^\\Z|^[\\w-][\\w-][\\w-][\\w-]+\\Z)")
    @Size(max=30)
    private String id;

    @NotNull
    @Size(min=4, max=30)
    private String name;

    @Size(min=3, max=300)
    private String description;

    @Valid
    private List<String> labels;

    @Valid
    private List<String> comments;

    // ToDo: Adding validation
    private List<InterworkingService> interworkingServices;

    @NotNull
    private boolean enabler;

    /**
     * Empty constructor
     */
    public PlatformDetails() {
    }

    /**
     *
     * @param id                            platform id
     * @param name                          platform name
     * @param description                   platform description
     * @param labels                        labels
     * @param comments                      comments
     * @param interworkingServices          list of interworking services
     * @param enabler                       specify if it is an enabler
     */
    public PlatformDetails(String id, String name, String description, List<String> labels,
                           List<String> comments, List<InterworkingService> interworkingServices, boolean enabler) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.labels = labels;
        this.comments = comments;
        this.interworkingServices = interworkingServices;
        this.enabler = enabler;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getLabels() { return labels; }
    public void setLabels(List<String> labels) { this.labels = labels; }

    public List<String> getComments() { return comments; }
    public void setComments(List<String> comments) { this.comments = comments; }

    public List<InterworkingService> getInterworkingServices() { return interworkingServices; }
    public void setInterworkingServices(List<InterworkingService> interworkingServices) { this.interworkingServices = interworkingServices; }

    public boolean isEnabler() { return enabler; }
    public void setEnabler(boolean enabler) { this.enabler = enabler; }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

}