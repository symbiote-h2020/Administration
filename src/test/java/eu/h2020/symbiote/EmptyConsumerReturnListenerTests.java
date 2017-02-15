package eu.h2020.symbiote;

import com.rabbitmq.client.AMQP;
import eu.h2020.symbiote.communication.EmptyConsumerReturnListener;
import eu.h2020.symbiote.communication.IRpcResponseListener;
import eu.h2020.symbiote.model.RpcPlatformResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EmptyConsumerReturnListenerTests {

    @Test
    public void testEmptyCallback_noProperties(){
        try {
            IRpcResponseListener responseListener = mock(IRpcResponseListener.class);
            EmptyConsumerReturnListener emptyConsumerReturnListener = new EmptyConsumerReturnListener();

            emptyConsumerReturnListener.addListener("testReplyQueueName", "testCorrelationId", responseListener);

            emptyConsumerReturnListener.handleReturn(0,"message","exchange","routingKey", null, null);

            verify(responseListener, never()).onRpcResponseReceive(any());
            assertEquals(1, emptyConsumerReturnListener.getListenerMap().size());
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void testEmptyCallback_noQueueName(){
        try {
            IRpcResponseListener responseListener = mock(IRpcResponseListener.class);
            EmptyConsumerReturnListener emptyConsumerReturnListener = new EmptyConsumerReturnListener();

            emptyConsumerReturnListener.addListener("testReplyQueueName", "testCorrelationId", responseListener);

            AMQP.BasicProperties props = new AMQP.BasicProperties()
                    .builder()
                    .build();

            emptyConsumerReturnListener.handleReturn(0,"message","exchange","routingKey", props, null);

            verify(responseListener, never()).onRpcResponseReceive(any());
            assertEquals(1, emptyConsumerReturnListener.getListenerMap().size());
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void testEmptyCallback_noCorrelationIdName(){
        try {
            IRpcResponseListener responseListener = mock(IRpcResponseListener.class);
            EmptyConsumerReturnListener emptyConsumerReturnListener = new EmptyConsumerReturnListener();

            emptyConsumerReturnListener.addListener("testReplyQueueName", "testCorrelationId", responseListener);

            AMQP.BasicProperties props = new AMQP.BasicProperties()
                    .builder()
                    .replyTo("notTestReplyQueueName")
                    .build();

            emptyConsumerReturnListener.handleReturn(0,"message","exchange","routingKey", props, null);

            verify(responseListener, never()).onRpcResponseReceive(any());
            assertEquals(1, emptyConsumerReturnListener.getListenerMap().size());
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void testEmptyCallback_wrongQueueNameAndCorrelationId(){
        try {
            IRpcResponseListener responseListener = mock(IRpcResponseListener.class);
            EmptyConsumerReturnListener emptyConsumerReturnListener = new EmptyConsumerReturnListener();

            emptyConsumerReturnListener.addListener("testReplyQueueName", "testCorrelationId", responseListener);

            AMQP.BasicProperties props = new AMQP.BasicProperties()
                    .builder()
                    .replyTo("notTestReplyQueueName")
                    .correlationId("notTestCorrelationId")
                    .build();

            emptyConsumerReturnListener.handleReturn(0,"message","exchange","routingKey", props, null);

            verify(responseListener, never()).onRpcResponseReceive(any());
            assertEquals(1, emptyConsumerReturnListener.getListenerMap().size());
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void testEmptyCallback_properQueueNameAndCorrelationId(){
        try {
            IRpcResponseListener responseListener = new IRpcResponseListener() {
                int timesCalled = 0;

                @Override
                public void onRpcResponseReceive(RpcPlatformResponse rpcPlatformResponse) {
                    timesCalled++;
                    if (rpcPlatformResponse.getStatus() != 500)
                        fail();
                    if (rpcPlatformResponse.getPlatform() != null)
                        fail();
                    assertEquals(1,timesCalled);
                }
            };
            EmptyConsumerReturnListener emptyConsumerReturnListener = new EmptyConsumerReturnListener();

            emptyConsumerReturnListener.addListener("testReplyQueueName", "testCorrelationId", responseListener);

            AMQP.BasicProperties props = new AMQP.BasicProperties()
                    .builder()
                    .replyTo("testReplyQueueName")
                    .correlationId("testCorrelationId")
                    .build();

            emptyConsumerReturnListener.handleReturn(0,"message","exchange","routingKey", props, null);
            assertEquals(0, emptyConsumerReturnListener.getListenerMap().size());
        } catch (IOException e) {
            fail();
        }
    }


}