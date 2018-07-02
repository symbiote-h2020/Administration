package eu.h2020.symbiote.administration.exceptions.generic;

public class GenericInternalServerErrorException extends Exception {

    public GenericInternalServerErrorException(String s) {
        super("An error occurred : " + s);
    }
}
