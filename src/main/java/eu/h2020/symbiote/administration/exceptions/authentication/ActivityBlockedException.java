package eu.h2020.symbiote.administration.exceptions.authentication;

import org.springframework.security.core.AuthenticationException;

import javax.servlet.http.HttpServletResponse;

public class ActivityBlockedException extends AuthenticationException implements CustomAuthenticationException {

    public ActivityBlockedException() { super("The account is blocked!"); }

    @Override
    public int getHttpStatus() { return HttpServletResponse.SC_FORBIDDEN; }
}
