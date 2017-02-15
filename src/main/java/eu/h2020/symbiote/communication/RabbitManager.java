package eu.h2020.symbiote.communication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import eu.h2020.symbiote.model.Platform;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * Class used for all internal communication using RabbitMQ AMQP implementation.
 * It works as a Spring Bean, and should be used via autowiring.
 * <p>
 * RabbitManager uses properties taken from CoreConfigServer to set up communication (exchange parameters, routing keys etc.)
 */
@Component
public class RabbitManager {
    private static Log log = LogFactory.getLog(RabbitManager.class);

    @Value("${rabbit.host}")
    private String rabbitHost;

    @Value("${rabbit.username}")
    private String rabbitUsername;

    @Value("${rabbit.password}")
    private String rabbitPassword;

    @Value("${rabbit.exchange.platform.name}")
    private String platformExchangeName;

    @Value("${rabbit.exchange.platform.type}")
    private String platformExchangeType;

    @Value("${rabbit.exchange.platform.durable}")
    private boolean platformExchangeDurable;

    @Value("${rabbit.exchange.platform.autodelete}")
    private boolean platformExchangeAutodelete;

    @Value("${rabbit.exchange.platform.internal}")
    private boolean platformExchangeInternal;

    @Value("${rabbit.routingKey.platform.creationRequested}")
    private String platformCreationRequestedRoutingKey;

    @Value("${rabbit.routingKey.platform.removalRequested}")
    private String platformRemovalRequestedRoutingKey;

    @Value("${rabbit.routingKey.platform.modificationRequested}")
    private String platformModificationRequestedRoutingKey;

    private Connection connection;
    private Channel channel;

    private EmptyConsumerReturnListener emptyConsumerReturnListener;

    //For the sake of unit tests
    private ReplyConsumer lastReplyConsumer;

    /**
     * Default, empty constructor.
     */
    public RabbitManager() {

    }


    /**
     * Constructor used only when doing Unit Tests.
     * Used to pass mocked instance of emptyConsumerReturnListener.
     *
     * @param emptyConsumerReturnListener instance of listener handling empty consumer messages
     * @param channel rabbit channel
     */
    public RabbitManager(EmptyConsumerReturnListener emptyConsumerReturnListener, Channel channel) {
        this.emptyConsumerReturnListener = emptyConsumerReturnListener;
        this.channel = channel;
    }

    /**
     * Method used to initialise RabbitMQ connection and declare all required exchanges.
     * This method should be called once, after bean initialization (so that properties from CoreConfigServer are obtained),
     * but before using RabbitManager to send any message.
     */
    public void initCommunication() {
        try {
            ConnectionFactory factory = new ConnectionFactory();

            // factory.setHost("localhost");
            factory.setHost(this.rabbitHost);
            factory.setUsername(this.rabbitUsername);
            factory.setPassword(this.rabbitPassword);

            this.connection = factory.newConnection();

            this.channel = this.connection.createChannel();
            this.channel.exchangeDeclare(this.platformExchangeName,
                    this.platformExchangeType,
                    this.platformExchangeDurable,
                    this.platformExchangeAutodelete,
                    this.platformExchangeInternal,
                    null);

            this.emptyConsumerReturnListener = new EmptyConsumerReturnListener();
            this.channel.addReturnListener(this.emptyConsumerReturnListener);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    /**
     * Cleanup method, used to close RabbitMQ channel and connection.
     */
    @PreDestroy
    private void cleanup() {
        try {
            if (this.channel != null && this.channel.isOpen())
                this.channel.close();
            if (this.connection != null && this.connection.isOpen())
                this.connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to send message via RPC (Remote Procedure Call) pattern.
     * Before sending a message, a temporary response queue is declared and its name is passed along with the message.
     * When a consumer handles the message, it returns the result via the response queue.
     * When the response is available, the responseListener is notified of its arrival.
     *
     * @param exchangeName     name of the exchange to send message to
     * @param routingKey       routing key to send message to
     * @param message          message to be sent
     * @param responseListener listener to be informed when the response message is available
     */
    public void sendRpcMessage(String exchangeName, String routingKey, String message, IRpcResponseListener responseListener) {
        try {
            log.debug("Sending message...");

            String replyQueueName = this.channel.queueDeclare().getQueue();

            String correlationId = UUID.randomUUID().toString();
            AMQP.BasicProperties props = new AMQP.BasicProperties()
                    .builder()
                    .correlationId(correlationId)
                    .replyTo(replyQueueName)
                    .build();

            if (responseListener != null)
                this.emptyConsumerReturnListener.addListener(replyQueueName, correlationId, responseListener);

            ReplyConsumer consumer = new ReplyConsumer(this.channel, correlationId, responseListener, this.emptyConsumerReturnListener);
            this.lastReplyConsumer = consumer;
            this.channel.basicConsume(replyQueueName, true, consumer);

            this.channel.basicPublish(exchangeName, routingKey, true, props, message.getBytes());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method that provides JSON marshalling and unmarshalling for the sake of Rabbit communication.
     *
     * @param exchangeName name of the exchange to send message to
     * @param routingKey   routing key to send message to
     * @param platform     platform to be sent
     */
    public void sendRpcPlatformMessage(String exchangeName, String routingKey, Platform platform, IRpcResponseListener responseListener) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String message = mapper.writeValueAsString(platform);

            this.sendRpcMessage(exchangeName, routingKey, message, responseListener);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to send RPC request to create platform.
     *
     * @param platform platform to be created
     * @param listener listener for rpc response
     */
    public void sendPlatformCreationRequest(Platform platform, IRpcResponseListener listener) {
        sendRpcPlatformMessage(this.platformExchangeName, this.platformCreationRequestedRoutingKey, platform, listener);
    }

    /**
     * Method used to send RPC request to remove platform.
     *
     * @param platform platform to be removed
     * @param listener listener for rpc response
     */
    public void sendPlatformRemovalRequest(Platform platform, IRpcResponseListener listener) {
        sendRpcPlatformMessage(this.platformExchangeName, this.platformRemovalRequestedRoutingKey, platform, listener);
    }

    /**
     * Method used to send RPC request to modify platform.
     *
     * @param platform platform to be modified
     * @param listener listener for rpc response
     */
    public void sendPlatformModificationRequest(Platform platform, IRpcResponseListener listener) {
        sendRpcPlatformMessage(this.platformExchangeName, this.platformModificationRequestedRoutingKey, platform, listener);
    }

    /**
     * Method used only when doing unit tests.
     *
     * @return Last created reply consumer
     */
    public ReplyConsumer getLastReplyConsumer() {
        return lastReplyConsumer;
    }
}
