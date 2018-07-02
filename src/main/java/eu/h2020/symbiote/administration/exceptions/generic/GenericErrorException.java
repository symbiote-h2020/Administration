package eu.h2020.symbiote.administration.exceptions.generic;

public class GenericErrorException extends Exception {

    public GenericErrorException(String s) {
        super("An error occurred : " + s);
    }
}
