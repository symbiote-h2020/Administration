package eu.h2020.symbiote.administration.exceptions.authentication;

import org.springframework.security.core.AuthenticationException;

import javax.servlet.http.HttpServletResponse;

public class WrongAdminPasswordException extends AuthenticationException implements CustomAuthenticationException {

    public WrongAdminPasswordException() { super("Wrong admin password!"); }

    @Override
    public int getHttpStatus() { return HttpServletResponse.SC_FORBIDDEN; }
}
