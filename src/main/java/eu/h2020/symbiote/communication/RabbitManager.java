package eu.h2020.symbiote.communication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import eu.h2020.symbiote.model.Platform;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
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
    private boolean plaftormExchangeDurable;

    @Value("${rabbit.exchange.platform.autodelete}")
    private boolean platformExchangeAutodelete;

    @Value("${rabbit.exchange.platform.internal}")
    private boolean platformExchangeInternal;

    @Value("${rabbit.routingKey.platform.creationRequested}")
    private String platformCreationRequestedRoutingKey;

    private Connection connection;

    /**
     * Initialization method.
     */
    @PostConstruct
    private void init() {
        //FIXME check if there is better exception handling in @postconstruct method
        Channel channel = null;
        try {
            ConnectionFactory factory = new ConnectionFactory();

            factory.setHost("localhost"); //todo value from properties

//            factory.setHost(this.rabbitHost);
//            factory.setUsername(this.rabbitUsername);
//            factory.setPassword(this.rabbitPassword);

            this.connection = factory.newConnection();

            channel = this.connection.createChannel();
            channel.exchangeDeclare(this.platformExchangeName,
                    this.platformExchangeType,
                    this.plaftormExchangeDurable,
                    this.platformExchangeAutodelete,
                    this.platformExchangeInternal,
                    null);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } finally {
            closeChannel(channel);
        }
    }

    /**
     * Cleanup method
     */
    @PreDestroy
    private void cleanup() {
        //FIXME check if there is better exception handling in @predestroy method
        try {
            if (this.connection != null && this.connection.isOpen())
                this.connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String exchange, String routingKey, String message) {
        Channel channel = null;

        try {
            channel = this.connection.createChannel();

            channel.basicPublish(exchange, routingKey, null, message.getBytes());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendRpcMessage(String message) {
        Channel channel = null;
        try {
            System.out.println("Sending message...");

            channel = this.connection.createChannel();

            String replyQueueName = channel.queueDeclare().getQueue();

            String correlationId = UUID.randomUUID().toString();
            AMQP.BasicProperties props = new AMQP.BasicProperties()
                    .builder()
                    .correlationId(correlationId)
                    .replyTo(replyQueueName)
                    .build();

            ReplyConsumer consumer = new ReplyConsumer(channel, correlationId);
            channel.basicConsume(replyQueueName, true, consumer);

            channel.basicPublish(this.platformExchangeName, this.platformCreationRequestedRoutingKey, props, message.getBytes());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeChannel(Channel channel) {
        try {
            if (channel != null && channel.isOpen())
                channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to send RPC request to create platform.
     *
     * @param platform platform to be created
     * @return object containing status of requested operation and, if successful, a platform object containing assigned ID
     */
    public void sendPlatformCreationRequest(Platform platform) {
        try {
            String message = null;
            ObjectMapper mapper = new ObjectMapper();
            message = mapper.writeValueAsString(platform);

            sendRpcMessage(message);
//
//            if (response == null)
//                return null;
//
//            PlatformCreationResponse responseObject = mapper.readValue(response, PlatformCreationResponse.class);
//            return responseObject;

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
