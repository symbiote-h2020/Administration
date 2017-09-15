package eu.h2020.symbiote.administration.exceptions.authentication;

import org.springframework.security.core.AuthenticationException;

public class WrongAdminPasswordException extends AuthenticationException {

    public WrongAdminPasswordException() {
        super("Wrong Admin password!");
    }

}
