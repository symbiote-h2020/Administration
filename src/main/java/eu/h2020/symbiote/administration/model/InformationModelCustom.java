package eu.h2020.symbiote.administration.model;

import eu.h2020.symbiote.core.model.RDFInfo;

import javax.validation.constraints.Size;


public class InformationModelCustom extends RDFInfo {

    @Size(min = 3)
    private String id;
    private String uri;
    private String name;
    private String owner;

    public InformationModelCustom() {
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUri() {
        return this.uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return this.owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
