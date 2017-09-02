package eu.h2020.symbiote.administration.model;

import javax.validation.constraints.Size;

/**
 * Wrapper for validating comments
 *
 * @author Vasileios Glykantzis (ICOM)
 */
public class Comment {

    @Size(min=3, max=300)
    private String comment;

    public Comment() {
    }

    public Comment(String comment) {
        setComment(comment);
    }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    @Override
    public String toString() {
        return comment;
    }
}
