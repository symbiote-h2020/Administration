package eu.h2020.symbiote.administration.model;

import javax.validation.constraints.Size;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;


/**
 * Class for a symbIoTe federation entity, used in form validation and messages
 *
 * @author Tilemachos Pechlivanoglou (ICOM)
 */
public class Federation {

    /* -------- Properties -------- */

    // @NotNull
    @Size(min=3, max=30)
    private String name;

    @Size(min=3, max=300)
    private String description;



    /* -------- Constructors -------- */

    /**
     * Empty constructor
     */
    public Federation() {
    }

    /**
     * Constructor with properties
     *
     * @param name              federation's name
     * @param description       federation's description
     */
    public Federation(String name, String description) {

        this.name = name;
        this.description = description;
    }


    /* -------- Getters & Setters -------- */

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }


    public void setDescription(String description) {
        this.description = description;
    }

    public void setName(String name) {
        this.name = name;
    }


    /* -------- Helper Methods -------- */

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

}