package eu.h2020.symbiote.administration.exceptions.generic;

public class GenericBadRequestException extends Exception {

    public GenericBadRequestException(String s) {
        super("An error occurred : " + s);
    }
}
