package eu.h2020.symbiote.administration.model;

import eu.h2020.symbiote.core.model.InterworkingService;
import eu.h2020.symbiote.core.model.Platform;
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
public class CreateFederationRequest {

    @NotNull
    @Pattern(regexp="^([\\w-][\\w-][\\w-][\\w-]+)")
    @Size(max=30)
    private String id;

    @NotNull
    @Pattern(regexp="^([\\w-][\\w-][\\w-][\\w-]+)")
    @Size(max=30)
    private String platform1Id;

    @NotNull
    @Pattern(regexp="^([\\w-][\\w-][\\w-][\\w-]+)")
    @Size(max=30)
    private String platform2Id;

    public CreateFederationRequest() {
    }

    public CreateFederationRequest(String id, String platform1Id, String platform2Id) {
        setId(id);
        setPlatform1Id(platform1Id);
        setPlatform2Id(platform2Id);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPlatform1Id() { return platform1Id; }
    public void setPlatform1Id(String platform1Id) { this.platform1Id = platform1Id; }

    public String getPlatform2Id() { return platform2Id; }
    public void setPlatform2Id(String platform2Id) { this.platform2Id = platform2Id; }
}