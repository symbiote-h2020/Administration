package eu.h2020.symbiote.communication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.DefaultConsumer;

import eu.h2020.symbiote.model.PlatformResponse;
import eu.h2020.symbiote.security.payloads.PlatformRegistrationResponse;
import eu.h2020.symbiote.security.payloads.Token
;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.IOException;

/**
 * RPC response handler.
 * <p>
 * This class is used to consume RabbitMQ response message, which comes via temporary, RPC response queue.
 */
public class ReplyConsumer extends DefaultConsumer {

    public enum ReplyType {
        REGISTRY,
        AAM_PLATFORM,
        AAM_USER,
        AAM_LOGIN;
    }

    private static Log log = LogFactory.getLog(ReplyConsumer.class);

    private String correlationId;
    private RegistryListener registryListener;
    private AAMPlatformListener aamRegistrationListener;
    private AAMLoginListener aamLoginListener;
    private ReplyType replyType;

    /**
     * Constructs a new instance and records its association to the passed-in channel, for registry listener
     *
     * @param channel                     the channel to which this consumer is attached
     * @param correlationId               correlationId used to send RPC request
     * @param registryListener            listener to be notified when the response is received
     */
    public ReplyConsumer(Channel channel, String correlationId, RegistryListener registryListener) {
        super(channel);
        this.correlationId = correlationId;
        this.registryListener = registryListener;
        this.replyType = ReplyType.REGISTRY;
    }

    /**
     * Constructs a new instance and records its association to the passed-in channel, for aam registration listener
     *
     * @param channel                     the channel to which this consumer is attached
     * @param correlationId               correlationId used to send RPC request
     * @param aamRegistrationListener     listener to be notified when the response is received
     */
    public ReplyConsumer(Channel channel, String correlationId, AAMPlatformListener aamRegistrationListener) {
        super(channel);
        this.correlationId = correlationId;
        this.aamRegistrationListener = aamRegistrationListener;
        this.replyType = ReplyType.AAM_PLATFORM;
    }

    /**
     * Constructs a new instance and records its association to the passed-in channel, for aam login listener
     *
     * @param channel                     the channel to which this consumer is attached
     * @param correlationId               correlationId used to send RPC request
     * @param aamLoginListener            listener to be notified when the response is received
     */
    public ReplyConsumer(Channel channel, String correlationId, AAMLoginListener aamLoginListener) {
        super(channel);
        this.correlationId = correlationId;
        this.aamLoginListener = aamLoginListener;
        this.replyType = ReplyType.AAM_LOGIN;
    }

    /**
     * Method used to handle messages coming to temporary, RPC response queue.
     * <p>
     * When a new message comes to queue and the correlation ID is the same as sent in request, it is being served.
     * It means, that if there was a response listener registered with it, it gets fired with the response delivered.
     *
     * @param consumerTag irrelevant for this implementation
     * @param envelope    contains routing key, which in this case is the name of temporary, RPC response queue
     * @param properties  contains correlation ID of a message to verify it's validity
     * @param body        contains response to be passed to listener
     * @throws IOException
     */
    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {

        if (properties.getCorrelationId().equals(this.correlationId)) {

            switch(this.replyType){
                case REGISTRY:
                    handleRegistry(body);
                    break;
                case AAM_PLATFORM:
                    handleAAMPlatform(body);
                    break;
                case AAM_LOGIN:
                    handleAAMLogin(body);
                    break;
            }            
        }
    }

    private void handleRegistry(byte[] body) throws IOException{

        PlatformResponse response = null;
        if (body!= null) {
            String message = new String(body, "UTF-8");
            log.debug(" [x] Received '" + message + "'");

            try {
                ObjectMapper mapper = new ObjectMapper();
                response = mapper.readValue(message, PlatformResponse.class);
            } catch (IOException e) {
                response = null;
            }
        }
        if (registryListener != null)
            registryListener.onRpcResponseReceive(response);
    }

    private void handleAAMPlatform(byte[] body) throws IOException{

        PlatformRegistrationResponse response = null;
        if (body!= null) {
            String message = new String(body, "UTF-8");
            log.debug(" [x] Received '" + message + "'");

            try {
                ObjectMapper mapper = new ObjectMapper();
                response = mapper.readValue(message, PlatformRegistrationResponse.class);
            } catch (IOException e) {
                response = null;
            }
        }
        if (aamRegistrationListener != null)
            aamRegistrationListener.onRpcResponseReceive(response);
    }

    private void handleAAMLogin(byte[] body) throws IOException{

        Token response = null;
        if (body!= null) {
            String message = new String(body, "UTF-8");
            log.debug(" [x] Received '" + message + "'");

            try {
                ObjectMapper mapper = new ObjectMapper();
                response = mapper.readValue(message, Token.class);
            } catch (IOException e) {
                response = null;
            }
        }
        if (aamLoginListener != null)
            aamLoginListener.onRpcResponseReceive(response);
    }
}
