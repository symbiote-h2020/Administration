package eu.h2020.symbiote.model;

import javax.validation.constraints.Size;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;


/**
 * Class for a symbIoTe platform secondary details entity, used in form validation and messages
 * // TODO R3 more details e.g. location etc
 *
 * @author Tilemachos Pechlivanoglou (ICOM)
 */
public class PlatformDetails {

    /* -------- Properties -------- */

    @Size(min=3, max=300)
    private String description;

    // @NotNull
    @Size(min=3, max=30)
    private String informationModelId;


    /* -------- Constructors -------- */

    /**
     * Empty constructor
     */
    public PlatformDetails() {
    }

    /**
     * Constructor with properties
     *
     * @param description           platform's description
     * @param informationModelId    id of the platform's information model
     */
    public PlatformDetails(String description, String informationModelId) {

        this.description = description;
        this.informationModelId = informationModelId;
    }


    /* -------- Getters & Setters -------- */

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


    /* -------- Helper Methods -------- */

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

}