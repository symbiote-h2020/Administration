package eu.h2020.symbiote.administration.exceptions;

import java.util.Map;

public class ValidationException extends Exception {
    private final Map<String, String> validationErrors;

    public ValidationException(String message, Map<String, String> validationErrors) {
        super(message);
        this.validationErrors = validationErrors;
    }

    public Map<String, String> getValidationErrors() { return validationErrors; }
}
