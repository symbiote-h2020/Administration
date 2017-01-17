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
    private IPlatformCreationResponseListener listener;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     *
     * @param channel the channel to which this consumer is attached
     * @param correlationId correlationId used to send RPC request
     * @param listener listener to be notified when the response is received
     */
    public ReplyConsumer(Channel channel, String correlationId, IPlatformCreationResponseListener listener) {
        super(channel);
        this.correlationId = correlationId;
        this.listener = listener;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope,
                               AMQP.BasicProperties properties, byte[] body)
            throws IOException {

        if (properties.getCorrelationId().equals(this.correlationId)){
            String message = new String(body, "UTF-8");
            System.out.println(" [x] Received '" + message + "'");

            ObjectMapper mapper = new ObjectMapper();
            PlatformCreationResponse response = mapper.readValue(message, PlatformCreationResponse.class);
            if (listener != null)
                listener.onPlatformCreationResponseReceive(response);
        }
    }
}
