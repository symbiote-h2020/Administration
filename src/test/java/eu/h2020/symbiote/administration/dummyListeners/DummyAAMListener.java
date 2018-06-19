package eu.h2020.symbiote.administration.dummyListeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.model.mim.Federation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vasgl on 7/17/2017.
 */
@Component
public class DummyAAMListener {
    private static Log log = LogFactory.getLog(DummyAAMListener.class);

    private List<Federation> federationCreated = new ArrayList<>();
    private List<Federation> federationUpdated = new ArrayList<>();
    private List<String> federationDeleted = new ArrayList<>();
    private ObjectMapper mapper = new ObjectMapper();

    public List<Federation> getFederationCreated() { return federationCreated; }
    public List<Federation> getFederationUpdated() { return federationUpdated; }
    public List<String> getFederationDeleted() { return federationDeleted; }

    public int federationMessagesCreated() { return federationCreated.size(); }
    public int federationMessagesUpdated() { return federationUpdated.size(); }
    public int federationMessagesDeleted() { return federationDeleted.size(); }

    public void clearMessagesReceivedByListener() {
        federationCreated.clear();
        federationUpdated.clear();
        federationDeleted.clear();
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "rabbit.queueName.federation.created", durable = "true", autoDelete = "true", exclusive = "false"),
            exchange = @Exchange(value = "${rabbit.exchange.federation.name}", ignoreDeclarationExceptions = "true",
                    durable = "${rabbit.exchange.federation.durable}", autoDelete  = "${rabbit.exchange.federation.autodelete}",
                    internal = "${rabbit.exchange.federation.internal}", type = "${rabbit.exchange.federation.type}"),
            key = "${rabbit.routingKey.federation.created}")
    )
    public void federationCreatedListener(Federation federation) {
        federationCreated.add(federation);

        try {
            String responseInString = mapper.writeValueAsString(federation);
            log.info("federationCreatedListener received update request: " + responseInString);
            log.info("federationCreatedListener.size() = " + federationMessagesCreated());
        } catch (JsonProcessingException e) {
            log.info(e.toString());
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "rabbit.queueName.federation.changed", durable = "true", autoDelete = "true", exclusive = "false"),
            exchange = @Exchange(value = "${rabbit.exchange.federation.name}", ignoreDeclarationExceptions = "true",
                    durable = "${rabbit.exchange.federation.durable}", autoDelete  = "${rabbit.exchange.federation.autodelete}",
                    internal = "${rabbit.exchange.federation.internal}", type = "${rabbit.exchange.federation.type}"),
            key = "${rabbit.routingKey.federation.changed}")
    )
    public void federationUpdatedListener(Federation federation) {
        federationUpdated.add(federation);

        try {
            String responseInString = mapper.writeValueAsString(federation);
            log.info("federationUpdatedListener received update request: " + responseInString);
            log.info("federationUpdatedListener.size() = " + federationMessagesUpdated());
        } catch (JsonProcessingException e) {
            log.info(e.toString());
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "rabbit.queueName.federation.deleted", durable = "true", autoDelete = "true", exclusive = "false"),
            exchange = @Exchange(value = "${rabbit.exchange.federation.name}", ignoreDeclarationExceptions = "true",
                    durable = "${rabbit.exchange.federation.durable}", autoDelete  = "${rabbit.exchange.federation.autodelete}",
                    internal = "${rabbit.exchange.federation.internal}", type = "${rabbit.exchange.federation.type}"),
            key = "${rabbit.routingKey.federation.deleted}")
    )
    public void federationDeletedListener(String id) {
        federationDeleted.add(id);

        log.info("federationDeletedListener received delete request for federation: " + id);
        log.info("federationDeletedListener.size() = " + federationMessagesDeleted());

    }
}
