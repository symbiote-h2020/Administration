package eu.h2020.symbiote.communication;

import eu.h2020.symbiote.security.payloads.Token;

/**
 * Interface used as a response listener for RPC RabbitMQ replies from AAM for platform registration.
 * <p>
 * When a message is sent using asynchronous RPC call, it needs to pass listener that waits for the response.
 */
public interface AAMLoginListener {
    /**
     * When the response to sent request is available, this method is called, passing response object as parameter.
     *
     * @param platformRegistrationRequest RPC response object
     */
    void onRpcResponseReceive(Token token);
}
