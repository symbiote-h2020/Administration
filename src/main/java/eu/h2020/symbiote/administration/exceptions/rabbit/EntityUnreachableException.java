package eu.h2020.symbiote.administration.exceptions.rabbit;

public class EntityUnreachableException extends RuntimeException {

    public EntityUnreachableException(String s) {
        super("The component " + s + " is unreachable");
    }
}
