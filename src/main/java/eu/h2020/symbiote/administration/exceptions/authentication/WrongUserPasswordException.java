package eu.h2020.symbiote.administration.exceptions.authentication;

import org.springframework.security.core.AuthenticationException;

import javax.servlet.http.HttpServletResponse;

public class WrongUserPasswordException extends AuthenticationException implements CustomAuthenticationException {

    public WrongUserPasswordException() { super("Wrong user password!"); }

    @Override
    public int getHttpStatus() { return HttpServletResponse.SC_UNAUTHORIZED; }
}
