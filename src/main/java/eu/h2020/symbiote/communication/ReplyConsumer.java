package eu.h2020.symbiote.communication;

import com.rabbitmq.client.*;

import java.io.IOException;

/**
 * Created by mateuszl on 13.01.2017.
 */
public class ReplyConsumer extends QueueingConsumer {

    private String correlationId;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     *
     * @param channel the channel to which this consumer is attached
     */
    public ReplyConsumer(Channel channel, String correlationId) {
        super(channel);
        this.correlationId = correlationId;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope,
                               AMQP.BasicProperties properties, byte[] body)
            throws IOException {

        if (properties.getCorrelationId().equals(this.correlationId)){
            //fixme handle delivered platform with ID from GUI
            String message = new String(body, "UTF-8");
            System.out.println(" [x] Received '" + message + "'");
        }
    }
}
