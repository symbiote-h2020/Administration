package eu.h2020.symbiote.administration.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;


/**
 * Class for a symbIoTe platform secondary details entity, used in form validation and messages
 *
 * @author Vasileios Glykantzis (ICOM)
 */
public class CreateFederationRequest {

    @NotNull
    @Pattern(regexp="^[\\w-]{4,}$")
    @Size(max=30)
    private String id;

    @NotNull
    @Valid
    private List<PlatformId> platforms;

    @JsonCreator
    public CreateFederationRequest(@JsonProperty("id") String id,
                                   @JsonProperty("platforms") List<PlatformId> platforms) {
        setId(id);
        setPlatforms(platforms);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public List<PlatformId> getPlatforms() { return platforms; }
    public void setPlatforms(List<PlatformId> platforms) {
        this.platforms = platforms;
    }
}