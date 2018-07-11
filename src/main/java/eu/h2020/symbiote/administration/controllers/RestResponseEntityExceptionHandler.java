package eu.h2020.symbiote.administration.controllers;

import eu.h2020.symbiote.administration.exceptions.authentication.*;
import eu.h2020.symbiote.administration.exceptions.generic.GenericHttpErrorException;
import eu.h2020.symbiote.administration.exceptions.rabbit.CommunicationException;
import eu.h2020.symbiote.administration.exceptions.rabbit.EntityUnreachableException;
import eu.h2020.symbiote.administration.exceptions.token.VerificationTokenExpired;
import eu.h2020.symbiote.administration.exceptions.token.VerificationTokenNotFoundException;
import eu.h2020.symbiote.administration.exceptions.validation.ServiceValidationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
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
    protected ModelAndView handleVerificationTokenException(HttpServletRequest request, Exception e) {
        log.warn("In handleVerificationTokenException", e);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("timestamp", new Date());
        modelAndView.addObject("path", request.getContextPath());
        modelAndView.addObject("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        modelAndView.addObject("status", HttpStatus.BAD_REQUEST);
        modelAndView.addObject("message", e.getMessage());
        modelAndView.addObject("exception", VerificationTokenNotFoundException.class.getCanonicalName());
        modelAndView.setViewName("error");
        return modelAndView;
    }

    @ExceptionHandler(value = {EntityUnreachableException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    protected Map<String, Object> handleEntityUnreachable(Exception e) {
        log.warn("In handleEntityUnreachable", e);

        Map<String, Object> response = new HashMap<>();
        response.put("errorMessage", e.getMessage());
        return response;
    }

    @ExceptionHandler(value = {CommunicationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected Map<String, Object> handleCommunicationExceptions(Exception e) {
        log.warn("In handleCommunicationExceptions", e);

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

    @ExceptionHandler(value = {WrongUserNameException.class, WrongUserPasswordException.class,
            WrongAdminPasswordException.class, ActivityBlockedException.class, InactiveAccountException.class})
    protected ResponseEntity<String> handleAuthenticationException(CustomAuthenticationException e) {
        log.warn("In handleAuthenticationException", (Throwable) e);

        return new ResponseEntity<>(e.getMessage(), HttpStatus.valueOf(e.getHttpStatus()));
    }
}