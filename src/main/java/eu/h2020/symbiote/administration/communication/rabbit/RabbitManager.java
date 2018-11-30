package eu.h2020.symbiote.administration.communication.rabbit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import eu.h2020.symbiote.administration.exceptions.rabbit.CommunicationException;
import eu.h2020.symbiote.core.cci.*;
import eu.h2020.symbiote.core.internal.*;
import eu.h2020.symbiote.model.mim.Federation;
import eu.h2020.symbiote.model.mim.Platform;
import eu.h2020.symbiote.model.mim.SmartSpace;
import eu.h2020.symbiote.security.commons.enums.AccountStatus;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import eu.h2020.symbiote.security.commons.enums.OperationType;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.communication.payloads.*;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


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

    @Value("${aam.deployment.owner.username}")
    private String aaMOwnerUsername;
    @Value("${aam.deployment.owner.password}")
    private String aaMOwnerPassword;

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

    @Value("${rabbit.exchange.platform.name}")
    private String informationModelExchangeName;
    @Value("${rabbit.exchange.platform.type}")
    private String informationModelExchangeType;
    @Value("${rabbit.exchange.platform.durable}")
    private boolean informationModelExchangeDurable;
    @Value("${rabbit.exchange.platform.autodelete}")
    private boolean informationModelExchangeAutodelete;
    @Value("${rabbit.exchange.platform.internal}")
    private boolean informationModelExchangeInternal;

    @Value("${rabbit.exchange.mapping.name}")
    private String mappingExchangeName;
    @Value("${rabbit.exchange.mapping.type}")
    private String mappingExchangeType;
    @Value("${rabbit.exchange.mapping.durable}")
    private boolean mappingExchangeDurable;
    @Value("${rabbit.exchange.mapping.autodelete}")
    private boolean mappingExchangeAutodelete;
    @Value("${rabbit.exchange.mapping.internal}")
    private boolean mappingExchangeInternal;

    @Value("${rabbit.exchange.ssp.name}")
    private String sspExchangeName;
    @Value("${rabbit.exchange.ssp.type}")
    private String sspExchangeType;
    @Value("${rabbit.exchange.ssp.durable}")
    private boolean sspExchangeDurable;
    @Value("${rabbit.exchange.ssp.autodelete}")
    private boolean sspExchangeAutodelete;
    @Value("${rabbit.exchange.ssp.internal}")
    private boolean sspExchangeInternal;

    @Value("${rabbit.exchange.resource.name}")
    private String resourceExchangeName;
    @Value("${rabbit.exchange.resource.type}")
    private String resourceExchangeType;
    @Value("${rabbit.exchange.resource.durable}")
    private boolean resourceExchangeDurable;
    @Value("${rabbit.exchange.resource.autodelete}")
    private boolean resourceExchangeAutodelete;
    @Value("${rabbit.exchange.resource.internal}")
    private boolean resourceExchangeInternal;

    @Value("${rabbit.routingKey.platform.creationRequested}")
    private String platformCreationRequestedRoutingKey;
    @Value("${rabbit.routingKey.platform.removalRequested}")
    private String platformRemovalRequestedRoutingKey;
    @Value("${rabbit.routingKey.platform.modificationRequested}")
    private String platformModificationRequestedRoutingKey;
    @Value("${rabbit.routingKey.platform.resourcesRequested}")
    private String platformResourcesRequestedRoutingKey;
    @Value("${rabbit.routingKey.platform.platformDetailsRequested}")
    private String platformDetailsRequestedRoutingKey;

    @Value("${rabbit.routingKey.platform.model.allInformationModelsRequested}")
    private String informationModelsRequestedRoutingKey;
    @Value("${rabbit.routingKey.platform.model.removalRequested}")
    private String informationModelRemovalRequestedRoutingKey;
    @Value("${rabbit.routingKey.platform.model.creationRequested}")
    private String informationModelCreationRequestedRoutingKey;

    @Value("${rabbit.routingKey.mapping.getAllMappingsRequested}")
    private String getAllMappingsRoutingKey;
    @Value("${rabbit.routingKey.mapping.getSingleMappingRequested}")
    private String getSingleMappingRoutingKey;
    @Value("${rabbit.routingKey.mapping.removalRequested}")
    private String mappingRemovalRequestedRoutingKey;
    @Value("${rabbit.routingKey.mapping.creationRequested}")
    private String mappingCreationRequestedRoutingKey;

    @Value("${rabbit.routingKey.resource.clearDataRequested}")
    private String clearPlatformResourcesRoutingKey;

    @Value("${rabbit.routingKey.ssp.creationRequested}")
    private String sspCreationRequestedRoutingKey;
    @Value("${rabbit.routingKey.ssp.removalRequested}")
    private String sspRemovalRequestedRoutingKey;
    @Value("${rabbit.routingKey.ssp.modificationRequested}")
    private String sspModificationRequestedRoutingKey;
    @Value("${rabbit.routingKey.ssp.sspDetailsRequested}")
    private String sspDetailsRequestedRoutingKey;

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
    @Value("${rabbit.routingKey.manage.revocation.request}")
    private String userRevocationRequestRoutingKey;
    @Value("${rabbit.routingKey.manage.platform.request}")
    private String platformManageRequestRoutingKey;
    @Value("${rabbit.routingKey.manage.smartspace.request}")
    private String sspManageRequestRoutingKey;
    @Value("${rabbit.routingKey.manage.user.request}")
    private String userManageRequestRoutingKey;
    @Value("${rabbit.routingKey.get.user.details}")
    private String getUserDetailsRoutingKey;
    @Value("${rabbit.routingKey.ownedservices.request}")
    private String getOwnedPlatformDetailsRoutingKey;

    // ------------ Federations ----------------

    @Value("${rabbit.exchange.federation.name}")
    private String federationExchangeName;
    @Value("${rabbit.exchange.federation.type}")
    private String federationExchangeType;
    @Value("${rabbit.exchange.federation.durable}")
    private boolean federationExchangeDurable;
    @Value("${rabbit.exchange.federation.autodelete}")
    private boolean federationExchangeAutodelete;
    @Value("${rabbit.exchange.federation.internal}")
    private boolean federationExchangeInternal;

    @Value("${rabbit.routingKey.federation.created}")
    private String federationCreatedRoutingKey;
    @Value("${rabbit.routingKey.federation.changed}")
    private String federationUpdatedRoutingKey;
    @Value("${rabbit.routingKey.federation.deleted}")
    private String federationDeletedRoutingKey;

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

            this.channel.exchangeDeclare(this.informationModelExchangeName,
                    this.informationModelExchangeType,
                    this.informationModelExchangeDurable,
                    this.informationModelExchangeAutodelete,
                    this.informationModelExchangeInternal,
                    null);

            this.channel.exchangeDeclare(this.mappingExchangeName,
                    this.mappingExchangeType,
                    this.mappingExchangeDurable,
                    this.mappingExchangeAutodelete,
                    this.mappingExchangeInternal,
                    null);

            this.channel.exchangeDeclare(this.sspExchangeName,
                    this.sspExchangeType,
                    this.sspExchangeDurable,
                    this.sspExchangeAutodelete,
                    this.sspExchangeInternal,
                    null);

            this.channel.exchangeDeclare(this.resourceExchangeName,
                    this.resourceExchangeType,
                    this.resourceExchangeDurable,
                    this.resourceExchangeAutodelete,
                    this.resourceExchangeInternal,
                    null);

            this.channel.exchangeDeclare(this.aamExchangeName,
                    this.aamExchangeType,
                    this.aamExchangeDurable,
                    this.aamExchangeAutodelete,
                    this.aamExchangeInternal,
                    null);

            this.channel.exchangeDeclare(this.federationExchangeName,
                    this.federationExchangeType,
                    this.federationExchangeDurable,
                    this.federationExchangeAutodelete,
                    this.federationExchangeInternal,
                    null);

        } catch (IOException | TimeoutException e) {
            log.error("", e);
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
        } catch (IOException | TimeoutException e) {
            log.error("", e);
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
     * @param contentType  the content type of the message
     * @return response from the consumer or null if timeout occurs
     */
    public String sendRpcMessage(String exchangeName, String routingKey, String message, String contentType) {
        String correlationId = UUID.randomUUID().toString();
        final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);
        DefaultConsumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
                if (properties.getCorrelationId().equals(correlationId)) {
                    response.offer(new String(body, "UTF-8"));
                }
            }
        };

        try {
            // log.debug("Sending message...");

            String replyQueueName = this.channel.queueDeclare().getQueue();

            AMQP.BasicProperties props = new AMQP.BasicProperties()
                    .builder()
                    .correlationId(correlationId)
                    .contentType(contentType)
                    .replyTo(replyQueueName)
                    .build();

            this.channel.basicPublish(exchangeName, routingKey, props, message.getBytes());
            this.channel.basicConsume(replyQueueName, true, consumer);


            return response.poll(rabbitTimeout, TimeUnit.MILLISECONDS);
        } catch (IOException | InterruptedException e) {
            log.warn("", e);
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
     * Method used to publish a message
     *
     * @param exchangeName name of the exchange to send message to
     * @param routingKey   routing key to send message to
     * @param message      message to be sent
     * @param contentType  the content type of the message
     */
    private void publishMessage(String exchangeName, String routingKey, String message, String contentType) {

        try {
            AMQP.BasicProperties props = new AMQP.BasicProperties()
                    .builder()
                    .contentType(contentType)
                    .build();


            this.channel.basicPublish(exchangeName, routingKey, props, message.getBytes());

        } catch (IOException e) {
            log.warn("", e);
        }
    }

    // #################################################
    // Interaction with Registry
    // #################################################

    /**
     * Helper method that provides JSON marshalling, unmarshalling and RabbitMQ communication with the Registry
     *
     * @param exchangeName name of the exchange to send message to
     * @param routingKey   routing key to send message to
     * @param platform     platform to be sent
     * @return response from the consumer or null if timeout occurs
     */
    public PlatformRegistryResponse sendRegistryPlatformMessage(String exchangeName, String routingKey,
                                                                Platform platform) throws CommunicationException {

        log.trace("sendRegistryPlatformMessage");

        try {
            
            String message = mapper.writeValueAsString(platform);

            String responseMsg = this.sendRpcMessage(exchangeName, routingKey, message, "application/json");

            if (responseMsg == null)
                return null;

            try {
                PlatformRegistryResponse response = mapper.readValue(responseMsg, PlatformRegistryResponse.class);
                log.trace("Received response from Registry.");
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
        log.debug("sendPlatformCreationRequest to Registry for: " + ReflectionToStringBuilder.toString(platform));
        return sendRegistryPlatformMessage(this.platformExchangeName, this.platformCreationRequestedRoutingKey, platform);
    }

    /**
     * Method used to send RPC request to remove platform.
     *
     * @param platform platform to be removed
     */
    public PlatformRegistryResponse sendPlatformRemovalRequest(Platform platform) throws CommunicationException  {
        log.debug("sendPlatformRemovalRequest to Registry for: " + ReflectionToStringBuilder.toString(platform));
        return sendRegistryPlatformMessage(this.platformExchangeName, this.platformRemovalRequestedRoutingKey, platform);
    }

    /**
     * Method used to send RPC request to modify platform.
     *
     * @param platform platform to be modified
     */
    public PlatformRegistryResponse sendPlatformModificationRequest(Platform platform) throws CommunicationException  {
        log.debug("sendPlatformModificationRequest to Registry for: " + ReflectionToStringBuilder.toString(platform));
        return sendRegistryPlatformMessage(this.platformExchangeName, this.platformModificationRequestedRoutingKey, platform);
    }

    /**
     * Helper method that provides JSON marshalling, unmarshalling and RabbitMQ communication with the Registry
     * regarding SSPs
     *
     * @param exchangeName name of the exchange to send message to
     * @param routingKey   routing key to send message to
     * @param smartSpace   smartSpace to be sent
     * @return response from the consumer or null if timeout occurs
     */
    public SspRegistryResponse sendRegistrySmartSpaceMessage(String exchangeName, String routingKey,
                                                             SmartSpace smartSpace) throws CommunicationException {

        log.trace("sendRegistrySmartSpaceMessage");

        try {

            String message = mapper.writeValueAsString(smartSpace);

            String responseMsg = this.sendRpcMessage(exchangeName, routingKey, message, "application/json");

            if (responseMsg == null)
                return null;

            try {
                SspRegistryResponse response = mapper.readValue(responseMsg, SspRegistryResponse.class);
                log.trace("Received response from Registry.");
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
     * Method used to send RPC request to create smartSpace.
     *
     * @param smartSpace smartSpace to be created
     */
    public SspRegistryResponse sendSmartSpaceCreationRequest(SmartSpace smartSpace) throws CommunicationException  {
        log.debug("sendSmartSpaceCreationRequest to Registry for: " + ReflectionToStringBuilder.toString(smartSpace));
        return sendRegistrySmartSpaceMessage(this.sspExchangeName, this.sspCreationRequestedRoutingKey, smartSpace);
    }

    /**
     * Method used to send RPC request to remove smartSpace.
     *
     * @param smartSpace smartSpace to be removed
     */
    public SspRegistryResponse sendSmartSpaceRemovalRequest(SmartSpace smartSpace) throws CommunicationException  {
        log.debug("sendSmartSpaceRemovalRequest to Registry for: " + ReflectionToStringBuilder.toString(smartSpace));
        return sendRegistrySmartSpaceMessage(this.sspExchangeName, this.sspRemovalRequestedRoutingKey, smartSpace);
    }

    /**
     * Method used to send RPC request to modify smartSpace.
     *
     * @param smartSpace smartSpace to be modified
     */
    public SspRegistryResponse sendSmartSpaceModificationRequest(SmartSpace smartSpace) throws CommunicationException  {
        log.debug("sendSmartSpaceModificationRequest to Registry for: " + ReflectionToStringBuilder.toString(smartSpace));
        return sendRegistrySmartSpaceMessage(this.sspExchangeName, this.sspModificationRequestedRoutingKey, smartSpace);
    }


    /**
     * Helper method that provides JSON marshalling, unmarshalling and RabbitMQ communication with the Registry
     *
     * @param platformId     id of the platform which want the details
     * @return response from the consumer or null if timeout occurs
     */
    public PlatformRegistryResponse sendGetPlatformDetailsMessage(String platformId) throws CommunicationException {

        log.debug("sendGetPlatformDetailsMessage for platform: " + platformId);

        String responseMsg = this.sendRpcMessage(this.platformExchangeName, this.platformDetailsRequestedRoutingKey,
                platformId, "text/plain");

        if (responseMsg == null)
            return null;

        try {
            PlatformRegistryResponse response = mapper.readValue(responseMsg, PlatformRegistryResponse.class);
            log.trace("Received response from Registry.");
            return response;

        } catch (Exception e){

            log.error("Error in response from Registry.", e);
            throw new CommunicationException(e);
        }
    }

    /**
     * Helper method that provides JSON marshalling, unmarshalling and RabbitMQ communication with the Registry
     *
     * @param sspId     id of the ssp which want the details
     * @return response from the consumer or null if timeout occurs
     */
    public SspRegistryResponse sendGetSSPDetailsMessage(String sspId) throws CommunicationException {

        log.debug("sendGetSSPDetailsMessage for platform: " + sspId);

        String responseMsg = this.sendRpcMessage(this.sspExchangeName, this.sspDetailsRequestedRoutingKey,
                sspId, "text/plain");

        if (responseMsg == null)
            return null;

        try {
            SspRegistryResponse response = mapper.readValue(responseMsg, SspRegistryResponse.class);
            log.trace("Received response from Registry.");
            return response;

        } catch (Exception e){

            log.error("Error in response from Registry.", e);
            throw new CommunicationException(e);
        }
    }

    /**
     * Method used to get all the available information models from the Registry
     *
     */
    public InformationModelListResponse sendListInfoModelsRequest()
            throws CommunicationException {

        log.debug("sendListInfoModelsRequest to Registry");

        try {
            // The message is false to indicate that we do not need the rdf of Information Models
            String responseMsg = this.sendRpcMessage(this.informationModelExchangeName,
                    this.informationModelsRequestedRoutingKey, "false", "text/plain");

            if (responseMsg == null)
                return null;

            try {
                InformationModelListResponse response = mapper.readValue(responseMsg,
                        InformationModelListResponse.class);
                log.trace("Received information model details response from Registry.");
                return response;

            } catch (Exception e){

                log.error("Error in information model details response response from Registry.", e);
                ErrorResponseContainer error = mapper.readValue(responseMsg, ErrorResponseContainer.class);
                throw new CommunicationException(error.getErrorMessage());
            }
        } catch (IOException e) {
            log.error("Failed (un)marshalling of rpc information model request message.", e);
        }
        return null;
    }

    /**
     * Method used to request on an action for an information model
     * @param request contains the information model
     * @return response from registry
     */
    public InformationModelResponse sendInfoModelRequest(String routingKey, InformationModelRequest request)
            throws CommunicationException {

        log.trace("sendInfoModelRequest to Registry");

        try {

            String message = mapper.writeValueAsString(request);

            // The message is false to indicate that we do not need the rdf of Information Models
            String responseMsg = this.sendRpcMessage(this.informationModelExchangeName,
                    routingKey, message, "application/json");

            if (responseMsg == null)
                return null;

            try {
                InformationModelResponse response = mapper.readValue(responseMsg,
                        InformationModelResponse.class);
                log.trace("Received information model response from Registry.");
                return response;

            } catch (Exception e){

                log.error("Error in information model details response from Registry.", e);
                ErrorResponseContainer error = mapper.readValue(responseMsg, ErrorResponseContainer.class);
                throw new CommunicationException(error.getErrorMessage());
            }
        } catch (IOException e) {
            log.error("Failed (un)marshalling of rpc information model request message.", e);
        }
        return null;
    }

    /**
     * Method used request the registration of an information model
     * @param request contains the information model to be registered
     * @return response from registry
     */
    public InformationModelResponse sendRegisterInfoModelRequest(InformationModelRequest request)
            throws CommunicationException {
        log.debug("sendRegisterInfoModelRequest to Registry for info model: " + ReflectionToStringBuilder.toString(request));
        return sendInfoModelRequest(this.informationModelCreationRequestedRoutingKey, request);
    }

    /**
     * Method used request the removal of an information model
     * @param request contains the information model to be deleted
     * @return response from registry
     */
    public InformationModelResponse sendDeleteInfoModelRequest(InformationModelRequest request)
            throws CommunicationException {
        log.debug("sendDeleteInfoModelRequest to Registry for info model: " + ReflectionToStringBuilder.toString(request));
        return sendInfoModelRequest(this.informationModelRemovalRequestedRoutingKey, request);
    }

    /**
     * Method used to get all the available mappings from the Registry
     *
     */
    public MappingListResponse sendGetAllMappingsRequest(GetAllMappings request)
            throws CommunicationException {

        log.debug("sendGetAllMappingsRequest to Registry");

        try {
            String message = mapper.writeValueAsString(request);

            // The message is false to indicate that we do not need the rdf of Information Models
            String responseMsg = this.sendRpcMessage(this.mappingExchangeName,
                    this.getAllMappingsRoutingKey, message, "application/json");

            if (responseMsg == null)
                return null;

            try {
                MappingListResponse response = mapper.readValue(responseMsg,
                        MappingListResponse.class);
                log.trace("Received all the mapping details response from Registry.");
                return response;

            } catch (Exception e){
                log.error("Error in mapping details response response from Registry.", e);
                ErrorResponseContainer error = mapper.readValue(responseMsg, ErrorResponseContainer.class);
                throw new CommunicationException(error.getErrorMessage());
            }
        } catch (IOException e) {
            log.error("Failed (un)marshalling of rpc get all mappings request message.", e);
        }
        return null;
    }

    /**
     * Method used to get a single mapping from the Registry
     *
     */
    public MappingListResponse sendGetSingleMappingsRequest(GetSingleMapping request)
            throws CommunicationException {

        log.debug("sendGetSingleMappingsRequest to Registry");

        try {
            String message = mapper.writeValueAsString(request);

            // The message is false to indicate that we do not need the rdf of Information Models
            String responseMsg = this.sendRpcMessage(this.mappingExchangeName,
                    this.getSingleMappingRoutingKey, message, "application/json");

            if (responseMsg == null)
                return null;

            try {
                MappingListResponse response = mapper.readValue(responseMsg,
                        MappingListResponse.class);
                log.trace("Received mapping details response from Registry for mappingId = " + request.getMappingId());
                return response;

            } catch (Exception e){
                log.error("Error in mapping details response response from Registry.", e);
                ErrorResponseContainer error = mapper.readValue(responseMsg, ErrorResponseContainer.class);
                throw new CommunicationException(error.getErrorMessage());
            }
        } catch (IOException e) {
            log.error("Failed (un)marshalling of rpc get all mappings request message.", e);
        }
        return null;
    }

    /**
     * Method used to request on an action for an information model
     * @param request contains the information model
     * @return response from registry
     */
    public InfoModelMappingResponse sendInfoModelMappingRequest(String routingKey, InfoModelMappingRequest request)
            throws CommunicationException {

        log.trace("sendInfoModelMappingRequest to Registry");

        try {

            String message = mapper.writeValueAsString(request);

            // The message is false to indicate that we do not need the rdf of Information Models
            String responseMsg = this.sendRpcMessage(this.mappingExchangeName,
                    routingKey, message, "application/json");

            if (responseMsg == null)
                return null;

            try {
                InfoModelMappingResponse response = mapper.readValue(responseMsg,
                        InfoModelMappingResponse.class);
                log.trace("Received info model mapping response from Registry.");
                return response;

            } catch (Exception e){

                log.error("Error in info model mapping response from Registry.", e);
                ErrorResponseContainer error = mapper.readValue(responseMsg, ErrorResponseContainer.class);
                throw new CommunicationException(error.getErrorMessage());
            }
        } catch (IOException e) {
            log.error("Failed (un)marshalling of rpc information model request message.", e);
        }
        return null;
    }

    /**
     * Method used request the registration of an information model mapping
     * @param request contains mapping to be registered
     * @return response from registry
     */
    public InfoModelMappingResponse sendRegisterMappingRequest(InfoModelMappingRequest request)
            throws CommunicationException {
        log.debug("sendRegisterMappingRequest to Registry for mapping: " + ReflectionToStringBuilder.toString(request));
        return sendInfoModelMappingRequest(this.mappingCreationRequestedRoutingKey, request);
    }

    /**
     * Method used request the removal of an information model mapping
     * @param request contains the mapping to be deleted
     * @return response from registry
     */
    public InfoModelMappingResponse sendDeleteMappingRequest(InfoModelMappingRequest request)
            throws CommunicationException {
        log.debug("sendDeleteInfoModelRequest to Registry for info model: " + ReflectionToStringBuilder.toString(request));
        return sendInfoModelMappingRequest(this.mappingRemovalRequestedRoutingKey, request);
    }

    /**
     * Helper method that provides JSON marshalling, unmarshalling and RabbitMQ communication with the Registry for resource list retrieval
     *
     * @param request      request for resources with id set
     * @return response from the consumer or null if timeout occurs
     */
    public ResourceListResponse sendRegistryResourcesRequest(CoreResourceRegistryRequest request)
            throws CommunicationException {

        log.trace("sendRegistryResourcesRequest to Registry");

        try {
            String message = mapper.writeValueAsString(request);

            String responseMsg = this.sendRpcMessage(this.platformExchangeName, this.platformResourcesRequestedRoutingKey,
                    message, "application/json");

            if (responseMsg == null)
                return null;

            try {
                ResourceListResponse response = mapper.readValue(responseMsg, ResourceListResponse.class);
                log.trace("Received response from Registry.");
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
     * Method used to request the removal of platform resources from the Administrator
     * @param request contains the information model
     * @return response from registry
     */
    public ClearDataResponse sendClearDataRequest(ClearDataRequest request)
            throws CommunicationException {

        log.trace("sendClearDataRequest to Registry");

        try {

            String message = mapper.writeValueAsString(request);

            // The message is false to indicate that we do not need the rdf of Information Models
            String responseMsg = this.sendRpcMessage(this.resourceExchangeName
                    , clearPlatformResourcesRoutingKey, message, "application/json");

            if (responseMsg == null)
                return null;

            try {
                ClearDataResponse response = mapper.readValue(responseMsg,
                        ClearDataResponse.class);
                log.trace("Received ClearDataResponse from Registry.");
                return response;

            } catch (Exception e){

                log.error("Error in ClearDataResponse from Registry.", e);
                ErrorResponseContainer error = mapper.readValue(responseMsg, ErrorResponseContainer.class);
                throw new CommunicationException(error.getErrorMessage());
            }
        } catch (IOException e) {
            log.error("Failed (un)marshalling of rpc information model request message.", e);
        }
        return null;
    }


    // #################################################
    // Interaction with AAM
    // #################################################

    /**
     * Method used to send RPC request to register user.
     *
     * @param request  request for registration
     * @return response status
     */
    public ManagementStatus sendUserManagementRequest(UserManagementRequest request) throws CommunicationException {
        log.debug("sendUserManagementRequest to AAM: " + ReflectionToStringBuilder.toString(request));
        try {
            String message = mapper.writeValueAsString(request);

            String responseMsg = this.sendRpcMessage(this.aamExchangeName, this.userManagementRequestRoutingKey, message, "application/json");

            if (responseMsg == null) {
                throw new CommunicationException("Communication Problem with AAM");
            }

            try {
                ManagementStatus response = mapper.readValue(responseMsg, ManagementStatus.class);
                log.trace("Received response from AAM.");
                return response;

            } catch (Exception e){

                log.error("Error in response from AAM.", e);
                ErrorResponseContainer error = mapper.readValue(responseMsg, ErrorResponseContainer.class);
                throw new CommunicationException(error.getErrorMessage());
            }


        } catch (IOException e) {
            log.error("Failed (un)marshalling of rpc resource message", e);
        }
        return null;
    }

    /**
     * Method used to send RPC request to register user.
     *
     * @param request  request for revocation
     * @return response status
     */
    public RevocationResponse sendRevocationRequest(RevocationRequest request) throws CommunicationException {
        log.debug("sendRevocationRequest to AAM: " + ReflectionToStringBuilder.toString(request));
        try {
            String message = mapper.writeValueAsString(request);

            String responseMsg = this.sendRpcMessage(this.aamExchangeName, this.userRevocationRequestRoutingKey, message, "application/json");

            if (responseMsg == null) {
                throw new CommunicationException("Communication Problem with AAM");
            }

            try {
                RevocationResponse response = mapper.readValue(responseMsg, RevocationResponse.class);
                log.trace("Received response from AAM.");
                return response;

            } catch (Exception e){

                log.error("Error in response from AAM.", e);
                ErrorResponseContainer error = mapper.readValue(responseMsg, ErrorResponseContainer.class);
                throw new CommunicationException(error.getErrorMessage());
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
    public PlatformManagementResponse sendManagePlatformRequest(PlatformManagementRequest request)
            throws CommunicationException {

        log.debug("sendManagePlatformRequest to AAM: " + ReflectionToStringBuilder.toString(request));

        try {
            String message = mapper.writeValueAsString(request);

            String responseMsg = this.sendRpcMessage(this.aamExchangeName, this.platformManageRequestRoutingKey, message, "application/json");

            if (responseMsg == null)
                return null;

            try {
                PlatformManagementResponse response = mapper.readValue(responseMsg, PlatformManagementResponse.class);
                log.trace("Received ManagePlatformResponse from AAM.");
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
     * Method used to send RPC request to register SSP in AAM.
     *
     * @param request  request for registration
     */
    public SmartSpaceManagementResponse sendManageSSPRequest(SmartSpaceManagementRequest request)
            throws CommunicationException {

        log.debug("sendManageSSPRequest to AAM: " + ReflectionToStringBuilder.toString(request));

        try {
            String message = mapper.writeValueAsString(request);

            String responseMsg = this.sendRpcMessage(this.aamExchangeName,
                    this.sspManageRequestRoutingKey, message, "application/json");

            if (responseMsg == null)
                return null;

            try {
                SmartSpaceManagementResponse response = mapper.readValue(responseMsg, SmartSpaceManagementResponse.class);
                log.trace("Received ManagePlatformResponse from AAM.");
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
     * @param userCredentials  the credentials of the user trying to login
     */
    public UserDetailsResponse sendLoginRequest(Credentials userCredentials) throws CommunicationException {

        log.debug("sendLoginRequest to AAM: " + ReflectionToStringBuilder.toString(userCredentials));
        return sendUserDetailsRequest(userCredentials, OperationType.READ);
    }

    /**
     * Method used to send RPC request to login user.
     *
     * @param username  the username
     */
    public UserDetailsResponse sendForceReadRequest(String username) throws CommunicationException {

        log.debug("sendForceReadRequest to AAM: " + username);
        return sendUserDetailsRequest(new Credentials(username, ""), OperationType.FORCE_READ);
    }

    /**
     * Method used to send RPC request to login user.
     *
     * @param userCredentials   the credentials of the user trying to login
     * @param type              the operation type
     */
    private UserDetailsResponse sendUserDetailsRequest(Credentials userCredentials, OperationType type) throws CommunicationException {

        log.debug("sendUserDetailsRequest to AAM: " + ReflectionToStringBuilder.toString(userCredentials));

        try {
            UserManagementRequest request = new UserManagementRequest(
                    new Credentials(aaMOwnerUsername, aaMOwnerPassword),
                    new Credentials(userCredentials.getUsername(), userCredentials.getPassword()),
                    new UserDetails(
                            new Credentials(userCredentials.getUsername(), userCredentials.getPassword()),
                            "",
                            UserRole.NULL,
                            AccountStatus.ACTIVE,
                            new HashMap<>(),
                            new HashMap<>(),
                            true,
                            true
                    ),
                    type
            );
            String message = mapper.writeValueAsString(request);

            String responseMsg = this.sendRpcMessage(this.aamExchangeName, this.getUserDetailsRoutingKey, message, "application/json");

            if (responseMsg == null)
                return null;

            try {
                UserDetailsResponse response = mapper.readValue(responseMsg, UserDetailsResponse.class);
                log.trace("Received login response from AAM.");
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
     * @param request  request for user management
     */
    public Set<OwnedService> sendOwnedServiceDetailsRequest(UserManagementRequest request)
            throws CommunicationException {

        log.debug("sendOwnedServiceDetailsRequest to AAM: " + ReflectionToStringBuilder.toString(request));

        try {
            String message = mapper.writeValueAsString(request);

            String responseMsg = this.sendRpcMessage(this.aamExchangeName, this.getOwnedPlatformDetailsRoutingKey, message, "application/json");

            if (responseMsg == null)
                return null;

            try {
                Set<OwnedService> response = mapper.readValue(responseMsg,
                        mapper.getTypeFactory().constructCollectionType(Set.class, OwnedService.class));
                log.trace("Received platform owner details response from AAM.");
                return response;

            } catch (Exception e){

                log.error("Error in owner platform details response from AAM.", e);
                ErrorResponseContainer error = mapper.readValue(responseMsg, ErrorResponseContainer.class);
                throw new CommunicationException(error.getErrorMessage());
            }
        } catch (IOException e) {
            log.error("Failed (un)marshalling of rpc resource message.", e);
        }
        return null;
    }

    /**
     * Method used to publish federation creation events
     *
     * @param federation  the created federation
     */
    public void publishFederationCreation(Federation federation) {

        log.debug("Publish federation creation: " + federation);

        String message;
        try {
            message = mapper.writeValueAsString(federation);
            this.publishMessage(this.federationExchangeName, this.federationCreatedRoutingKey, message, "application/json");
        } catch (JsonProcessingException e) {
            log.warn("Failed to publish federation creation due to", e);
        }
    }

    /**
     * Method used to publish federation update events
     *
     * @param federation  the updated federation
     */
    public void publishFederationUpdate(Federation federation) {

        log.debug("Publish federation update: " + federation);

        String message;
        try {
            message = mapper.writeValueAsString(federation);
            this.publishMessage(this.federationExchangeName, this.federationUpdatedRoutingKey, message, "application/json");
        } catch (JsonProcessingException e) {
            log.warn("Failed to publish federation update due to", e);
        }
    }

    /**
     * Method used to publish federation deletion events
     *
     * @param federationId  the deleted federationId
     */
    public void publishFederationDeletion(String federationId) {

        log.debug("Publish federation deletion for: " + federationId);

        try {
            AMQP.BasicProperties props = new AMQP.BasicProperties()
                    .builder()
                    .build();


            this.channel.basicPublish(this.federationExchangeName, this.federationDeletedRoutingKey, props, federationId.getBytes());

        } catch (IOException e) {
            log.warn("", e);
        }
    }

    // Used in testing
    public void setMapper(ObjectMapper mapper) { this.mapper = mapper; }

}