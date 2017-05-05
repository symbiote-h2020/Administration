package eu.h2020.symbiote.communication;

import eu.h2020.symbiote.model.PlatformResponse;

/**
 * Interface used as a response listener for RPC RabbitMQ replies from Registry.
 * <p>
 * When a message is sent using asynchronous RPC call, it needs to pass listener that waits for the response.
 */
public interface RegistryListener {
    /**
     * When the response to sent request is available, this method is called, passing response object as parameter.
     *
     * @param platformResponse RPC response object
     */
    void onRpcResponseReceive(PlatformResponse platformResponse);
}
