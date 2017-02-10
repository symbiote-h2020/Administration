package eu.h2020.symbiote.communication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer;
import eu.h2020.symbiote.model.RpcPlatformResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * RPC response handler.
 * <p>
 * This class is used to consume RabbitMQ response message, which comes via temporary, RPC response queue.
 */
public class ReplyConsumer extends QueueingConsumer {
    private static Log log = LogFactory.getLog(ReplyConsumer.class);

    private String correlationId;
    private IRpcResponseListener responseListener;
    private EmptyConsumerReturnListener emptyConsumerReturnListener;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     *
     * @param channel                     the channel to which this consumer is attached
     * @param correlationId               correlationId used to send RPC request
     * @param responseListener            listener to be notified when the response is received
     * @param emptyConsumerReturnListener listener that handles empty exchange bindings
     */
    public ReplyConsumer(Channel channel, String correlationId, IRpcResponseListener responseListener, EmptyConsumerReturnListener emptyConsumerReturnListener) {
        super(channel);
        this.correlationId = correlationId;
        this.responseListener = responseListener;
        this.emptyConsumerReturnListener = emptyConsumerReturnListener;
    }

    /**
     * Method used to handle messages coming to temporary, RPC response queue.
     * <p>
     * When a new message comes to queue and the correlation ID is the same as sent in request, it is being served.
     * It means, that if there was a response listener registered with it, it gets fired with the response delivered,
     * and the (replyQueueName, correlationID) pair is unregistered from EmptyConsumerReturnListener.
     *
     * @param consumerTag irrelevant for this implementation
     * @param envelope    contains routing key, which in this case is the name of temporary, RPC response queue
     * @param properties  contains correlation ID of a message to verify it's validity
     * @param body        contains response to be passed to listener
     * @throws IOException
     */
    @Override
    public void handleDelivery(String consumerTag, Envelope envelope,
                               AMQP.BasicProperties properties, byte[] body)
            throws IOException {

        String queueName = envelope.getRoutingKey();

        if (properties.getCorrelationId().equals(this.correlationId)) {
            String message = new String(body, "UTF-8");
            log.debug(" [x] Received '" + message + "'");

            ObjectMapper mapper = new ObjectMapper();
            RpcPlatformResponse response = mapper.readValue(message, RpcPlatformResponse.class);
            if (responseListener != null)
                responseListener.onRpcResponseReceive(response);
            if (this.emptyConsumerReturnListener != null)
                this.emptyConsumerReturnListener.removeListener(queueName, correlationId);
        }
    }
}
