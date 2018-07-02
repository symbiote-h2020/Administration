package eu.h2020.symbiote.administration.exceptions.rabbit;

public class EntityUnreachable extends RuntimeException {

    public EntityUnreachable(String s) {
        super("The component " + s + " is unreachable");
    }
}
