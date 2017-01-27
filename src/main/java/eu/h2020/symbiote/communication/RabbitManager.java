package eu.h2020.symbiote.communication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import eu.h2020.symbiote.model.Platform;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * Bean used to manage internal communication using RabbitMQ.
 * It is responsible for declaring exchanges and using routing keys from centralized config server.
 */
@Component
public class RabbitManager {

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

    /**
     * Initialization method.
     */
    public void initCommunication() {
        try {
            ConnectionFactory factory = new ConnectionFactory();

            factory.setHost("localhost");
            // factory.setHost(this.rabbitHost);
            // factory.setUsername(this.rabbitUsername);
            // factory.setPassword(this.rabbitPassword);

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
     * Cleanup method
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

    private void sendMessage(String exchange, String routingKey, String message) {
        try {
            this.channel.basicPublish(exchange, routingKey, null, message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendPlatformRpcMessage(String exchangeName, String routingKey, Platform platform, IRpcResponseListener responseListener) {
        try {
            System.out.println("Sending message...");

            String message;
            ObjectMapper mapper = new ObjectMapper();
            message = mapper.writeValueAsString(platform);

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
            this.channel.basicConsume(replyQueueName, true, consumer);

            this.channel.basicPublish(exchangeName, routingKey, true, props, message.getBytes());

        } catch (IOException e) {
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
        sendPlatformRpcMessage(this.platformExchangeName, this.platformCreationRequestedRoutingKey, platform, listener);
    }

    /**
     * Method used to send RPC request to remove platform.
     *
     * @param platform platform to be removed
     * @param listener listener for rpc response
     */
    public void sendPlatformRemovalRequest(Platform platform, IRpcResponseListener listener) {
        sendPlatformRpcMessage(this.platformExchangeName, this.platformRemovalRequestedRoutingKey, platform, listener);
    }

    /**
     * Method used to send RPC request to modify platform.
     *
     * @param platform platform to be modified
     * @param listener listener for rpc response
     */
    public void sendPlatformModificationRequest(Platform platform, IRpcResponseListener listener) {
        sendPlatformRpcMessage(this.platformExchangeName, this.platformModificationRequestedRoutingKey, platform, listener);
    }
}
