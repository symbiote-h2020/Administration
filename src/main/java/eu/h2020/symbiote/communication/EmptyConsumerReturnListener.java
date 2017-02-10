package eu.h2020.symbiote.communication;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ReturnListener;
import eu.h2020.symbiote.model.RpcPlatformResponse;
import javafx.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Class responsible for handling RPC messages send to an exchange without bound consumers.
 * <p>
 * When request is sent to an exchange without any consumers subscribed, it will not be buffered, but will disappear
 * without any trace. Therefore, not only the request will not be served, but also caller will not get back any
 * notification, since communication is done asynchronously. That's where EmptyConsumerReturnListener comes to work.
 * If it gets called by failing to deliver a mandatory message, it will notify the registered response listener that
 * the request could not be served.
 */
public class EmptyConsumerReturnListener implements ReturnListener {
    private static Log log = LogFactory.getLog(EmptyConsumerReturnListener.class);

    private Map<Pair, IRpcResponseListener> listenerMap;

    /**
     * Default empty constructor.
     */
    public EmptyConsumerReturnListener() {
        this.listenerMap = new HashMap<>();
    }

    /**
     * Implementation of handling return message from RabbitMQ.
     * <p>
     * When a mandatory message is to be sent, its response listener is registered using
     * {{@link #addListener(String, String, IRpcResponseListener)}} method, using reply queue name and correlation ID
     * as key. When the handleReturn method is called, it checks the message's reply queue name and correlation ID to
     * find out if the response listener has been registered for it. If so, it fires it with proper response message
     * (that is status 500 - Internal Server Error, and empty body).
     * <p>
     * When message was handled succesfully by consumer, its response listener is unregistered from
     * EmptyConsumerReturnListener.
     *
     * @param replyCode  irrelevant for this implementation of ReturnListener
     * @param replyText  irrelevant for this implementation of ReturnListener
     * @param exchange   irrelevant for this implementation of ReturnListener
     * @param routingKey irrelevant for this implementation of ReturnListener
     * @param properties properties object that was sent with the message; it should contain both replyTo and
     *                   correlationId parameters
     * @param body       irrelevant for this implementation of ReturnListener
     * @throws IOException
     */
    @Override
    public void handleReturn(int replyCode, String replyText, String exchange, String routingKey, AMQP.BasicProperties properties, byte[] body) throws IOException {
        log.debug("Return listener fired");
        if (properties == null)
            return;

        String queueName = properties.getReplyTo();
        if (queueName == null)
            return;

        String correlationId = properties.getCorrelationId();
        if (correlationId == null)
            return;

        log.debug("Has queue name and correlationID");

        Pair<String, String> listenerKey = new Pair<>(queueName, correlationId);
        IRpcResponseListener listener = this.listenerMap.get(listenerKey);
        if (listener == null)
            return;

        log.debug("Has listener to fire");

        RpcPlatformResponse response = new RpcPlatformResponse();
        response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);

        listener.onRpcResponseReceive(response);
        this.removeListener(queueName, correlationId);
    }

    /**
     * Method used to register response listener for specified reply queue name and correlation ID.
     *
     * @param queueName reply queue name to register listener for
     * @param correlationId correlation ID to register listener for
     * @param listener response listener to register
     */
    public void addListener(String queueName, String correlationId, IRpcResponseListener listener) {
        log.debug("Adding return listener");
        Pair<String, String> listenerKey = new Pair<>(queueName, correlationId);
        this.listenerMap.put(listenerKey, listener);
    }

    /**
     * Method used to unregister response listener for specified reply queue name and correlation ID.
     *
     * @param queueName reply queue name to unregister listener for
     * @param correlationId correlation ID to unregister listener for

     */
    public void removeListener(String queueName, String correlationId) {
        log.debug("Removing return listener");
        Pair<String, String> listenerKey = new Pair<>(queueName, correlationId);
        this.listenerMap.remove(listenerKey);
    }
}
