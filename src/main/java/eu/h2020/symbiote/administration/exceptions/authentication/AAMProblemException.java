package eu.h2020.symbiote.administration.exceptions.authentication;

import org.springframework.security.core.AuthenticationException;

import javax.servlet.http.HttpServletResponse;

public class AAMProblemException extends AuthenticationException implements CustomAuthenticationException {

    public AAMProblemException() { super("Problem in the communication with AAM!"); }

    @Override
    public int getHttpStatus() { return HttpServletResponse.SC_INTERNAL_SERVER_ERROR; }
}
