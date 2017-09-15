package eu.h2020.symbiote.administration.exceptions.authentication;

import org.springframework.security.core.AuthenticationException;

public class WrongUserNameException extends AuthenticationException {

    public WrongUserNameException() {
        super("Username does not exist!");
    }

}
