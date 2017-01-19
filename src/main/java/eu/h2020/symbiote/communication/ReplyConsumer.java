package eu.h2020.symbiote.communication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import eu.h2020.symbiote.model.PlatformCreationResponse;

import java.io.IOException;

/**
 * RPC response handler.
 */
public class ReplyConsumer extends QueueingConsumer {

    private String correlationId;
    private IPlatformCreationResponseListener responseListener;
    private EmptyConsumerReturnListener emptyConsumerReturnListener;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     *
     * @param channel the channel to which this consumer is attached
     * @param correlationId correlationId used to send RPC request
     * @param responseListener listener to be notified when the response is received
     * @param emptyConsumerReturnListener listener that handles empty exchange bindings
     */
    public ReplyConsumer(Channel channel, String correlationId, IPlatformCreationResponseListener responseListener, EmptyConsumerReturnListener emptyConsumerReturnListener) {
        super(channel);
        this.correlationId = correlationId;
        this.responseListener = responseListener;
        this.emptyConsumerReturnListener = emptyConsumerReturnListener;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope,
                               AMQP.BasicProperties properties, byte[] body)
            throws IOException {

        String queueName = envelope.getRoutingKey();

        if (properties.getCorrelationId().equals(this.correlationId)){
            String message = new String(body, "UTF-8");
            System.out.println(" [x] Received '" + message + "'");

            ObjectMapper mapper = new ObjectMapper();
            PlatformCreationResponse response = mapper.readValue(message, PlatformCreationResponse.class);
            if (responseListener != null)
                responseListener.onPlatformCreationResponseReceive(response);
            this.emptyConsumerReturnListener.removeListener(queueName, correlationId);
        }
    }
}
