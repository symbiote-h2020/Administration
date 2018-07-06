package eu.h2020.symbiote.administration.exceptions.authentication;

import org.springframework.security.core.AuthenticationException;

import javax.servlet.http.HttpServletResponse;

public class WrongUserNameException extends AuthenticationException implements CustomAuthenticationException {

    public WrongUserNameException() { super("Username does not exist!"); }

    @Override
    public int getHttpStatus() { return HttpServletResponse.SC_BAD_REQUEST; }

}
