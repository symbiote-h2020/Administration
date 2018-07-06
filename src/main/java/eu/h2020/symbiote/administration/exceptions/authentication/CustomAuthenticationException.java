package eu.h2020.symbiote.administration.exceptions.authentication;

public interface CustomAuthenticationException {
    int getHttpStatus();
    String getMessage();
}
