package eu.h2020.symbiote.administration.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Wrapper for validating comments
 *
 * @author Vasileios Glykantzis (ICOM)
 */
public class Description {

    @NotNull
    @Size(min=4, max=300)
    private String description;

    public Description() {
    }

    public Description(String description) {
        setDescription(description);
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return description;
    }
}
