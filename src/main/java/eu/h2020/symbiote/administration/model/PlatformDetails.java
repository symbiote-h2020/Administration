package eu.h2020.symbiote.administration.model;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import eu.h2020.symbiote.core.model.InterworkingService;
import eu.h2020.symbiote.core.model.Platform;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import java.util.ArrayList;
import java.util.List;


/**
 * Class for a symbIoTe platform secondary details entity, used in form validation and messages
 *
 * @author Vasileios Glykantzis (ICOM)
 */
public class PlatformDetails {

    @NotNull
    @Pattern(regexp="^(\\Z|[\\w-][\\w-][\\w-][\\w-]+)")
    @Size(max=30)
    private String id;

    @NotNull
    @Size(min=3, max=30)
    private String name;

    @Size(min=3, max=300)
    private String description;

    @Valid
    private List<Label> labels;

    @Valid
    private List<Comment> comments;

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
     * @param description                   platform description
     * @param labels                        labels
     * @param comments                      comments
     * @param interworkingServices          list of interworking services
     * @param isEnabler                     specify if it is an enabler
     */
    public PlatformDetails(String id, String name, String description, List<Label> labels,
                           List<Comment> comments, List<InterworkingService> interworkingServices, Boolean isEnabler) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.labels = labels;
        this.comments = comments;
        this.interworkingServices = interworkingServices;
        this.isEnabler = isEnabler;
    }

    /**
     *
     * @param platform the platform details sent by registry
     */
    public PlatformDetails(Platform platform) {
        this.id = platform.getId();
        this.name = platform.getLabels().get(0);
        this.description = platform.getComments().get(0);
        this.isEnabler = platform.isEnabler();
        this.interworkingServices = new ArrayList<>(platform.getInterworkingServices());

        ArrayList<Label> labels = new ArrayList<>();
        for(String label : platform.getLabels())
            labels.add(new Label(label));
        labels.remove(0);

        ArrayList<Comment> comments = new ArrayList<>();
        for(String comment : platform.getComments())
            comments.add(new Comment(comment));
        comments.remove(0);

        this.labels = labels;
        this.comments = comments;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Label> getLabels() { return labels; }
    public void setLabels(List<Label> labels) { this.labels = labels; }

    public List<Comment> getComments() { return comments; }
    public void setComments(List<Comment> comments) { this.comments = comments; }

    public List<InterworkingService> getInterworkingServices() { return interworkingServices; }
    public void setInterworkingServices(List<InterworkingService> interworkingServices) { this.interworkingServices = interworkingServices; }

    public Boolean getIsEnabler() { return isEnabler; }
    public void setIsEnabler(Boolean isEnabler) { this.isEnabler = isEnabler; }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

}