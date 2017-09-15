package eu.h2020.symbiote.administration.model;

import javax.validation.constraints.Size;

/**
 * Wrapper for validating comments
 *
 * @author Vasileios Glykantzis (ICOM)
 */
public class Label {

    @Size(min=3, max=300)
    private String label;

    public Label() {
    }

    public Label(String label) {
        setLabel(label);
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    @Override
    public String toString() {
        return label;
    }
}
