package eu.h2020.symbiote.administration.exceptions.generic;

import org.springframework.http.HttpStatus;

public class GenericHttpErrorException extends Exception {

    private final HttpStatus httpStatus;
    private Object response;

    public GenericHttpErrorException(String s, HttpStatus httpStatus) {

        super("An error occurred: " + s);
        this.httpStatus = httpStatus;
    }

    public GenericHttpErrorException(String s, Object response, HttpStatus httpStatus) {

        this(s, httpStatus);
        this.response = response;
    }

    public HttpStatus getHttpStatus() { return httpStatus; }
    public Object getResponse() { return response; }
}
