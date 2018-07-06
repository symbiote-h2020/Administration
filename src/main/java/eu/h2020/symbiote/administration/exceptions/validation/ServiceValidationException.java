package eu.h2020.symbiote.administration.exceptions.validation;

import java.util.Map;

public class ServiceValidationException extends Exception {
    private final Map<String, String> validationErrors;

    public ServiceValidationException(String message, Map<String, String> validationErrors) {
        super(message);
        this.validationErrors = validationErrors;
    }

    public Map<String, String> getValidationErrors() { return validationErrors; }
}
