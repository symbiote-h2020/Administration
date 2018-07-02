package eu.h2020.symbiote.administration.controllers;

import eu.h2020.symbiote.administration.exceptions.generic.GenericErrorException;
import eu.h2020.symbiote.administration.exceptions.rabbit.CommunicationException;
import eu.h2020.symbiote.administration.exceptions.rabbit.EntityUnreachable;
import eu.h2020.symbiote.administration.exceptions.token.VerificationTokenExpired;
import eu.h2020.symbiote.administration.exceptions.token.VerificationTokenNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = {VerificationTokenNotFoundException.class, VerificationTokenExpired.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected String handleVerificationTokenException(Exception e) {
        return e.getMessage();
    }

    @ExceptionHandler(value = {CommunicationException.class, GenericErrorException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected Map<String, Object> handleCommunicationExceptions(Exception e) {

        Map<String, Object> response = new HashMap<>();
        response.put("errorMessage", e.getMessage());
        return response;
    }

    @ExceptionHandler(value = {EntityUnreachable.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    protected Map<String, Object> handleEntityUnreachableException(Exception e) {

        Map<String, Object> response = new HashMap<>();
        response.put("errorMessage", e.getMessage());
        return response;
    }
}