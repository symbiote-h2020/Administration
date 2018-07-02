package eu.h2020.symbiote.administration.exceptions.rabbit;

import org.springframework.http.HttpStatus;

/**
 * Custom exception thrown when a there is an error during rabbitmq communication
 *
 * @author Tilemachos Pechlivanoglou (ICOM)
 */
public class CommunicationException extends Exception {

    private final static String errorMessage = "ERR_RABBITMQ_COMMUNICATION";
    private final static HttpStatus statusCode = HttpStatus.BAD_REQUEST;

    public CommunicationException() {
        super(errorMessage);
    }

    public CommunicationException(String message) {
        super(message);
    }

    public CommunicationException(Throwable cause) {
        super(cause);
    }

    public CommunicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

}