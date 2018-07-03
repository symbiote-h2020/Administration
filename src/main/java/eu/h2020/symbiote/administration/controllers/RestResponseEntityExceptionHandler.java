package eu.h2020.symbiote.administration.controllers;

import eu.h2020.symbiote.administration.exceptions.ServiceValidationException;
import eu.h2020.symbiote.administration.exceptions.generic.GenericHttpErrorException;
import eu.h2020.symbiote.administration.exceptions.rabbit.CommunicationException;
import eu.h2020.symbiote.administration.exceptions.rabbit.EntityUnreachable;
import eu.h2020.symbiote.administration.exceptions.token.VerificationTokenExpired;
import eu.h2020.symbiote.administration.exceptions.token.VerificationTokenNotFoundException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    private static Log log = LogFactory.getLog(RestResponseEntityExceptionHandler.class);

    @ExceptionHandler(value = {GenericHttpErrorException.class})
    protected ResponseEntity handleGenericHttpErrorException(GenericHttpErrorException e) {
        log.warn("In handleGenericHttpErrorException", e);

        if (e.getResponse() != null)
            return new ResponseEntity<>(e.getResponse(), e.getHttpStatus());
        else {
            Map<String, Object> response = new HashMap<>();
            response.put("errorMessage", e.getMessage());
            return new ResponseEntity<>(response, e.getHttpStatus());
        }
    }

    @ExceptionHandler(value = {VerificationTokenNotFoundException.class, VerificationTokenExpired.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected String handleVerificationTokenException(Exception e) {
        log.warn("In handleVerificationTokenException", e);
        return e.getMessage();
    }

    @ExceptionHandler(value = {EntityUnreachable.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    protected Map<String, Object> handleEntityUnreachable(Exception e) {
        log.warn("In handleInternalServerErrorExceptions", e);

        Map<String, Object> response = new HashMap<>();
        response.put("errorMessage", e.getMessage());
        return response;
    }

    @ExceptionHandler(value = {CommunicationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected Map<String, Object> handleCommunicationExceptions(Exception e) {
        log.warn("In handleInternalServerErrorExceptions", e);

        Map<String, Object> response = new HashMap<>();
        response.put("errorMessage", e.getMessage());
        return response;
    }

    @ExceptionHandler(value = {ServiceValidationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected Map<String, Object> handleValidationException(ServiceValidationException e) {
        log.warn("In handleValidationException", e);

        Map<String, Object> response = new HashMap<>();
        response.put("validationErrors", e.getValidationErrors());
        return response;
    }
}