// package eu.h2020.symbiote;

// import java.io.IOException;
// import javafx.util.Pair;

// import com.rabbitmq.client.AMQP;
// import com.rabbitmq.client.Channel;
// import com.rabbitmq.client.Envelope;
// import com.rabbitmq.client.impl.AMQImpl;

// import org.junit.Test;
// import org.junit.runner.RunWith;
// import static org.junit.Assert.*;
// import static org.mockito.Mockito.*;
// import org.mockito.runners.MockitoJUnitRunner;

// import eu.h2020.symbiote.core.model.Platform;
// import eu.h2020.symbiote.communication.RabbitManager;
// import eu.h2020.symbiote.communication.CommunicationException;




// @RunWith(MockitoJUnitRunner.class)
// public class RabbitManagerTests {

//     // @Test
//     // public void testSendRpcMessage_success() throws IOException {
//     //     //Initialising mocks
//     //     EmptyConsumerReturnListener emptyConsumerReturnListener = spy(EmptyConsumerReturnListener.class);

//     //     Channel channel = mock(Channel.class);
//     //     when(channel.queueDeclare()).thenReturn(new AMQImpl.Queue.DeclareOk("testReplyQueueName",0,0));

//     //     IRpcResponseListener rpcResponseListener = mock(IRpcResponseListener.class);

//     //     //Sending message
//     //     RabbitManager rabbitManager = spy(new RabbitManager(emptyConsumerReturnListener, channel));
//     //     rabbitManager.sendRpcMessage("testExchangeName", "testRoutingKey", "testMessage", rpcResponseListener);
//     //     assertEquals(1, emptyConsumerReturnListener.getListenerMap().size());

//     //     //Mocking response received
//     //     String correlationId = null;
//     //     for (Pair<String, String> pair : emptyConsumerReturnListener.getListenerMap().keySet()){
//     //         correlationId = pair.getValue();
//     //     }

//     //     AMQP.BasicProperties props = new AMQP.BasicProperties()
//     //             .builder()
//     //             .correlationId(correlationId)
//     //             .build();
//     //     Envelope envelope = new Envelope(0, false, "", "testReplyQueueName");

//     //     rabbitManager.getLastReplyConsumer().handleDelivery("testConsumerTag", envelope, props, null);
//     //     assertEquals(0, emptyConsumerReturnListener.getListenerMap().size());
//     //     verify(rpcResponseListener, times(1)).onRpcResponseReceive(any());
//     // }

//     // @Test
//     // public void testSendPlatformRpcMessage_success() {

//     // 	RabbitManager rabbitManager = Mockito.mock(RabbitManager.class);
//     //     when(rabbitManager.sendRpcMessage(any(), any(), any())).thenReturn(false);


//     //     RabbitManager rabbitManager = spy(new RabbitManager());
//     //     doNothing().when(rabbitManager).sendRpcMessage(any(), any(), any());

//     //     Platform platform = new Platform();

//     //     try{
//     //     	rabbitManager.sendRegistryMessage("testExchangeName","testRoutingKey", platform);
//     //     } catch(CommunicationException e){

//     //     }
        
//     // }

// }
