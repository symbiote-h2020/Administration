package eu.h2020.symbiote.administration.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Objects;

public class PlatformId {

    @NotNull
    @Pattern(regexp="^[\\w-]{4,}$")
    @Size(max=30)
    private String id;

    @JsonCreator
    public PlatformId(@JsonProperty("id") String id) {
        this.id = id;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlatformId that = (PlatformId) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {

        return Objects.hash(getId());
    }
}
