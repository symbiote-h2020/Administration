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
    private String id;

    @JsonCreator
    public SSPDetails(@JsonProperty("id") String id) {
        this.id = id;
    }

    public String getId() { return id; }
}
