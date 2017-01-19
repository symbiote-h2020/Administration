package eu.h2020.symbiote.communication;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ReturnListener;
import eu.h2020.symbiote.model.PlatformCreationResponse;
import javafx.util.Pair;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Class responsible for handling RPC messages send to an exchange without bound consumers.
 */
public class EmptyConsumerReturnListener implements ReturnListener {
    private Map<Pair, IPlatformCreationResponseListener> listenerMap;

    public EmptyConsumerReturnListener(){
        this.listenerMap = new HashMap<>();
    }

    @Override
    public void handleReturn(int replyCode, String replyText, String exchange, String routingKey, AMQP.BasicProperties properties, byte[] body) throws IOException {
        System.out.println("Return listener fired");
        if (properties == null)
            return;

        String queueName = properties.getReplyTo();
        if (queueName == null)
            return;

        String correlationId = properties.getCorrelationId();
        if (correlationId == null)
            return;

        System.out.println("Has queue name and correlationID");

        Pair<String, String> listenerKey = new Pair<>(queueName, correlationId);
        IPlatformCreationResponseListener listener = this.listenerMap.get(listenerKey);
        if (listener == null)
            return;

        System.out.println("Has listener to fire");

        PlatformCreationResponse response = new PlatformCreationResponse();
        response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);

        listener.onPlatformCreationResponseReceive(response);
        this.removeListener(queueName, correlationId);
    }

    public void addListener(String queueName, String correlationId, IPlatformCreationResponseListener listener){
        System.out.println("Adding return listener");
        Pair<String, String> listenerKey = new Pair<>(queueName, correlationId);
        this.listenerMap.put(listenerKey, listener);
    }

    public void removeListener(String queueName, String correlationId){
        System.out.println("Removing return listener");
        Pair<String, String> listenerKey = new Pair<>(queueName, correlationId);
        this.listenerMap.remove(listenerKey);
    }
}
