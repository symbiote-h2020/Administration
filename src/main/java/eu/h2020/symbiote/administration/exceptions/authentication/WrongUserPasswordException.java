package eu.h2020.symbiote.administration.exceptions.authentication;

import org.springframework.security.core.AuthenticationException;

public class WrongUserPasswordException extends AuthenticationException {

    public WrongUserPasswordException() {
        super("Wrong password!");
    }

}
