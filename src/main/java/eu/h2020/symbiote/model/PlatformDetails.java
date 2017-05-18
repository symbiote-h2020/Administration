package eu.h2020.symbiote.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;

public class PlatformDetails {

    @Size(min=4, max=300)
    private String description;

    // @NotNull
    @Size(min=3, max=30)
    private String informationModelId;


    public PlatformDetails(String description, String informationModelId) {

        this.description = description;
        this.informationModelId = informationModelId;
    }

    public PlatformDetails() {
    }


    public String getDescription() {
        return this.description;
    }

    public String getInformationModelId() {
        return this.informationModelId;
    }


    public void setDescription(String description) {
        this.description = description;
    }

    public void setInformationModelId(String informationModelId) {
        this.informationModelId = informationModelId;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

}