package eu.h2020.symbiote.communication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import eu.h2020.symbiote.security.communication.payloads.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.core.cci.PlatformRegistryResponse;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryRequest;
import eu.h2020.symbiote.core.internal.ResourceListResponse;
import eu.h2020.symbiote.security.commons.Token;


/**
 * Class used for all internal communication using RabbitMQ AMQP implementation.
 *
 * RabbitManager works as a Spring Bean, and should be used via autowiring.
 * It uses properties taken from CoreConfigServer to set up communication 
 * (exchange parameters, routing keys etc.)
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

    @Value("${rabbit.timeoutMillis}")
    private Long rabbitTimeout;
    

    // ------------ Registry communication ----------------

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

    @Value("${rabbit.routingKey.platform.resourcesRequested}")
    private String platformResourcesRequestedRoutingKey;

    // ------------ Core AAM communication ----------------

    @Value("${rabbit.exchange.aam.name}")
    private String aamExchangeName;

    @Value("${rabbit.exchange.aam.type}")
    private String aamExchangeType;

    @Value("${rabbit.exchange.aam.durable}")
    private boolean aamExchangeDurable;

    @Value("${rabbit.exchange.aam.autodelete}")
    private boolean aamExchangeAutodelete;

    @Value("${rabbit.exchange.aam.internal}")
    private boolean aamExchangeInternal;

    @Value("${rabbit.routingKey.manage.user.request}")
    private String userManagementRequestRoutingKey;

    @Value("${rabbit.routingKey.manage.platform.request}")
    private String platformManageRequestRoutingKey;

    @Value("${rabbit.routingKey.manage.user.request}")
    private String appRegisterRequestRoutingKey;

    @Value("${rabbit.routingKey.getHomeToken.request}")
    private String loginRoutingKey;

    @Value("${rabbit.routingKey.ownedplatformdetails.request}")
    private String detailsRoutingKey;

    // ----------------------------------------------------

    private Connection connection;
    private Channel channel;
    private ObjectMapper mapper;

    /**
     * Default constructor, initializing the JSON mapper.
     */
    public RabbitManager() {

        mapper = new ObjectMapper();
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

            this.channel.exchangeDeclare(this.aamExchangeName,
                    this.aamExchangeType,
                    this.aamExchangeDurable,
                    this.aamExchangeAutodelete,
                    this.aamExchangeInternal,
                    null);

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
     * In this implementation it covers asynchronous Rabbit communication with synchronous one, as it is used by conventional REST facade.
     * Before sending a message, a temporary response queue is declared and its name is passed along with the message.
     * When a consumer handles the message, it returns the result via the response queue.
     * Since this is a synchronous pattern, it uses timeout of 20 seconds. 
     * If the response doesn't come in that time, the method returns with null result.
     *
     * @param exchangeName name of the exchange to send message to
     * @param routingKey   routing key to send message to
     * @param message      message to be sent
     * @return response from the consumer or null if timeout occurs
     */
    public String sendRpcMessage(String exchangeName, String routingKey, String message) {
        QueueingConsumer consumer = new QueueingConsumer(channel);

        try {
            // log.debug("Sending message...");

            String replyQueueName = this.channel.queueDeclare().getQueue();

            String correlationId = UUID.randomUUID().toString();
            AMQP.BasicProperties props = new AMQP.BasicProperties()
                    .builder()
                    .correlationId(correlationId)
                    .contentType("application/json")
                    .replyTo(replyQueueName)
                    .build();


            this.channel.basicConsume(replyQueueName, true, consumer);

            String responseMsg = null;

            this.channel.basicPublish(exchangeName, routingKey, props, message.getBytes());
            while (true) {
                QueueingConsumer.Delivery delivery = consumer.nextDelivery(rabbitTimeout);
                if (delivery == null){
                    log.info("Timeout in response retrieval");
                    return null;
                }

                if (delivery.getProperties().getCorrelationId().equals(correlationId)) {
                    responseMsg = new String(delivery.getBody());
                    break;
                } else {
                    log.info("Wrong correlationID in response message");
                }
            }

            // log.debug("Received response: " + responseMsg);
            return responseMsg;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                this.channel.basicCancel(consumer.getConsumerTag());
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return null;
    }


    /**
     * Helper method that provides JSON marshalling, unmarshalling and RabbitMQ communication with the Registry
     *
     * @param exchangeName name of the exchange to send message to
     * @param routingKey   routing key to send message to
     * @param platform     platform to be sent
     * @return response from the consumer or null if timeout occurs
     */
    public PlatformRegistryResponse sendRegistryPlatformMessage(String exchangeName, String routingKey, Platform platform) throws CommunicationException  {

        log.debug("sendRegistryPlatformMessage");

        try {
            
            String message = mapper.writeValueAsString(platform);

            String responseMsg = this.sendRpcMessage(exchangeName, routingKey, message);

            if (responseMsg == null)
                return null;

            try {
                PlatformRegistryResponse response = mapper.readValue(responseMsg, PlatformRegistryResponse.class);
                log.info("Received response from Registry.");
                return response;

            } catch (Exception e){

                log.error("Error in response from Registry.", e);
                throw new CommunicationException(e);
            }
        
        } catch (IOException e) {
            log.error("Failed (un)marshalling of rpc resource message", e);
        }
        return null;
    }


    /**
     * Method used to send RPC request to create platform.
     *
     * @param platform platform to be created
     */
    public PlatformRegistryResponse sendPlatformCreationRequest(Platform platform) throws CommunicationException  {
        log.debug("sendPlatformCreationRequest");
        return sendRegistryPlatformMessage(this.platformExchangeName, this.platformCreationRequestedRoutingKey, platform);
    }

    /**
     * Method used to send RPC request to remove platform.
     *
     * @param platform platform to be removed
     */
    public PlatformRegistryResponse sendPlatformRemovalRequest(Platform platform) throws CommunicationException  {
        log.debug("sendPlatformRemovalRequest");
        return sendRegistryPlatformMessage(this.platformExchangeName, this.platformRemovalRequestedRoutingKey, platform);
    }

    /**
     * Method used to send RPC request to modify platform.
     *
     * @param platform platform to be modified
     */
    public PlatformRegistryResponse sendPlatformModificationRequest(Platform platform) throws CommunicationException  {
        log.debug("sendPlatformModificationRequest");
        return sendRegistryPlatformMessage(this.platformExchangeName, this.platformModificationRequestedRoutingKey, platform);
    }


    /**
     * Helper method that provides JSON marshalling, unmarshalling and RabbitMQ communication with the Registry for resource list retrieval
     *
     * @param exchangeName name of the exchange to send message to
     * @param routingKey   routing key to send message to
     * @param request      request for resources with id set
     * @return response from the consumer or null if timeout occurs
     */
    public ResourceListResponse sendRegistryResourcesMessage(String exchangeName, String routingKey, CoreResourceRegistryRequest request)
            throws CommunicationException {

        try {
            String message = mapper.writeValueAsString(request);

            String responseMsg = this.sendRpcMessage(exchangeName, routingKey, message);

            if (responseMsg == null)
                return null;

            try {
                ResourceListResponse response = mapper.readValue(responseMsg, ResourceListResponse.class);
                log.info("Received response from Registry.");
                return response;

            } catch (Exception e){

                log.error("Error in response from Registry.", e);
                throw new CommunicationException(e);
            }
        
        } catch (IOException e) {
            log.error("Failed (un)marshalling of rpc resource message", e);
        }
        return null;
    }

    /**
     * Method used to retrieve the list of resources of this platform from the Registry.
     *
     * @param request request for platform resources list
     */
    public ResourceListResponse sendRegistryResourcesRequest(CoreResourceRegistryRequest request) throws CommunicationException  {
    
            return sendRegistryResourcesMessage(this.platformExchangeName, this.platformResourcesRequestedRoutingKey, request);
    }


    /**
     * Method used to send RPC request to register user.
     *
     * @param request  request for registration
     * @return response status
     */
    public ManagementStatus sendUserManagementRequest(UserManagementRequest request) throws CommunicationException {
        log.debug("sendUserManagementRequest");
        try {
            String message = mapper.writeValueAsString(request);

            String responseMsg = this.sendRpcMessage(this.aamExchangeName, this.userManagementRequestRoutingKey, message);

            if (responseMsg == null){

                throw new CommunicationException("Communication Problem with AAM");
            }

            try {
                ManagementStatus response = mapper.readValue(responseMsg, ManagementStatus.class);
                log.info("Received response from AAM.");
                return response;

            } catch (Exception e){

                log.error("Error in response from AAM.", e);
                throw new CommunicationException(e);
            }


        } catch (IOException e) {
            log.error("Failed (un)marshalling of rpc resource message", e);
        }
        return null;
    }


    /**
     * Method used to send RPC request to register platform in AAM.
     *
     * @param request  request for registration
     */
    public PlatformManagementResponse sendPlatformRegistrationRequest(PlatformManagementRequest request)
            throws CommunicationException {

        log.debug("sendPlatformRegistrationRequest");

        try {
            String message = mapper.writeValueAsString(request);

            String responseMsg = this.sendRpcMessage(this.aamExchangeName, this.platformManageRequestRoutingKey, message);

            if (responseMsg == null)
                return null;

            try {
                PlatformManagementResponse response = mapper.readValue(responseMsg, PlatformManagementResponse.class);
                log.info("Received platform registration response from AAM.");
                return response;

            } catch (Exception e){

                log.error("Error in platform registration response from AAM.", e);
                ErrorResponseContainer error = mapper.readValue(responseMsg, ErrorResponseContainer.class);
                throw new CommunicationException(error.getErrorMessage());
            }
        } catch (IOException e) {
            log.error("Failed (un)marshalling of rpc resource message", e);
        }
        return null;
    }


    /**
     * Method used to send RPC request to login user.
     *
     * @param credentials  credentials for login
     */
    public Token sendLoginRequest(Credentials credentials) throws CommunicationException {

        log.debug("sendLoginRequest");

        try {
            String message = mapper.writeValueAsString(credentials);

            String responseMsg = this.sendRpcMessage(this.aamExchangeName, this.loginRoutingKey, message);

            if (responseMsg == null)
                return null;

            try {
                Token response = mapper.readValue(responseMsg, Token.class);
                log.info("Received login response from AAM.");
                return response;

            } catch (Exception e){

                log.error("Error in login response from AAM.", e);
                ErrorResponseContainer error = mapper.readValue(responseMsg, ErrorResponseContainer.class);
                throw new CommunicationException(error.getErrorMessage());
            }
        } catch (IOException e) {
            log.error("Failed (un)marshalling of rpc resource message", e);
        }
        return null;
    }


    /**
     * Method used to send RPC request to get a platform owner's platform details.
     *
     * @param token  token of user
     */
    public OwnedPlatformDetails sendDetailsRequest(String token) throws CommunicationException {

        log.debug("sendDetailsRequest");

        try {
            String message = mapper.writeValueAsString(token);

            String responseMsg = this.sendRpcMessage(this.aamExchangeName, this.detailsRoutingKey, message);

            if (responseMsg == null)
                return null;

            try {
                OwnedPlatformDetails response = mapper.readValue(responseMsg, OwnedPlatformDetails.class);
                log.info("Received platform owner details response from AAM.");
                return response;

            } catch (Exception e){

                log.error("Error in owner details response from AAM.", e);
                ErrorResponseContainer error = mapper.readValue(responseMsg, ErrorResponseContainer.class);
                throw new CommunicationException(error.getErrorMessage());
            }
        } catch (IOException e) {
            log.error("Failed (un)marshalling of rpc resource message.", e);
        }
        return null;
    }


    // /**
    //  * Helper method that provides JSON marshalling, unmarshalling and RabbitMQ communication with Federation Manager
    //  *
    //  * @param exchangeName name of the exchange to send message to
    //  * @param routingKey   routing key to send message to
    //  * @param token  token to be sent
    //  * @return response from the consumer or null if timeout occurs
    //  */
    // public FererationResponse sendFederationMessage(String exchangeName, String routingKey, Federation federation) throws CommunicationException {
    //     try {
    //         String message = mapper.writeValueAsString(federation);

    //         String responseMsg = this.sendRpcMessage(exchangeName, routingKey, message);

    //         if (responseMsg == null)
    //             return null;

    //         try {
    //             FererationResponse response = mapper.readValue(responseMsg, FererationResponse.class);
    //             log.info("Received platform owner details response from AAM.");
    //             return response;

    //         } catch (Exception e){

    //             log.error("Error in owner details response from AAM.", e);
    //             ErrorResponseContainer error = mapper.readValue(responseMsg, ErrorResponseContainer.class);
    //             throw new CommunicationException(error.getErrorMessage());
    //         }
    //     } catch (IOException e) {
    //         log.error("Failed (un)marshalling of rpc resource message.", e);
    //     }
    //     return null;
    // }


    // /**
    //  * Method used to send RPC request to get a platform owner's platform details.
    //  *
    //  * @param token  token of user
    //  */
    // public FererationResponse sendFederationRequest(Federation federation) throws CommunicationException  {
    //     return sendAAMDetailsMessage(this.aamExchangeName, this.detailsRoutingKey, token);
    // }


}