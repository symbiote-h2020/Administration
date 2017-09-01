package eu.h2020.symbiote.administration.exceptions.authentication;

import org.springframework.security.core.AuthenticationException;

public class AAMProblemException extends AuthenticationException {

    public AAMProblemException() {
        super("Problem in the communicationn with AAM!");
    }

}
