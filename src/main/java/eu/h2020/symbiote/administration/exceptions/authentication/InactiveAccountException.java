package eu.h2020.symbiote.administration.exceptions.authentication;

import org.springframework.security.core.AuthenticationException;

import javax.servlet.http.HttpServletResponse;

public class InactiveAccountException extends AuthenticationException implements CustomAuthenticationException {

    public InactiveAccountException() { super("The account is inactive!"); }

    @Override
    public int getHttpStatus() { return HttpServletResponse.SC_FORBIDDEN; }
}
