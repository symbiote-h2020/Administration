package eu.h2020.symbiote.communication;


import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Class used to manage internal communication using RabbitMQ.
 * It is responsible for declaring exchanges and using routing keys from centralized config server.
 * Class is accessed using singleton design pattern.
 */
@Component
public class RabbitManager {

    @Value("${rabbit.host}")
    private String rabbitHost;

    @Value("${rabbit.exchange.platform.name}")
    private String platformExchangeName;

    @Value("${rabbit.exchange.platform.type}")
    private String platformExchangeType;

    @Value("${rabbit.exchange.platform.durable}")
    private boolean plaftormExchangeDurabble;

    @Value("${rabbit.exchange.platform.autodelete}")
    private boolean platformExchangeAutodelete;

    @Value("${rabbit.exchange.platform.internal}")
    private boolean platformExchangeInternal;

    private Connection connection;

    /**
     * Initialization method.
     */
    @PostConstruct
    private void init() {
        //FIXME check if there is better exception handling in @postconstruct method
        System.out.println(this.rabbitHost);
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(this.rabbitHost);
            this.connection = factory.newConnection();
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
        //FIXME check if there is better exception handling in @predestroy method
        try {
            if (this.connection != null && this.connection.isOpen())
                this.connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String exchange, String routingKey, String message){
        try {
            Channel channel = this.connection.createChannel();

            channel.basicPublish(exchange, routingKey, null, message.getBytes());

            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    private void sendRpcMessage(String exchange, String routingKey, String message){
        //TODO implement
    }

}
