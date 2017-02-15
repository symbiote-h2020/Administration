package eu.h2020.symbiote;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.communication.EmptyConsumerReturnListener;
import eu.h2020.symbiote.communication.IRpcResponseListener;
import eu.h2020.symbiote.communication.ReplyConsumer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ReplyConsumerTests {
    @Test
    public void testReplyConsumer_wrongCorrelationId() {
        try {
            IRpcResponseListener responseListener = mock(IRpcResponseListener.class);

            AMQP.BasicProperties props = new AMQP.BasicProperties()
                    .builder()
                    .correlationId("notTestCorrelationId")
                    .build();
            Envelope envelope = new Envelope(0, false, "", "testReplyQueueName");

            ReplyConsumer replyConsumer = new ReplyConsumer(null, "testCorrelationId", responseListener, null);
            replyConsumer.handleDelivery(null, envelope, props, null);

            verify(responseListener, never()).onRpcResponseReceive(any());

        } catch (IOException e) {
            fail();
        }

    }

    @Test
    public void testReplyConsumer_nullResponseListener() {
        try {
            IRpcResponseListener responseListener = null;
            EmptyConsumerReturnListener emptyConsumerReturnListener = spy(EmptyConsumerReturnListener.class);
            emptyConsumerReturnListener.addListener("testReplyQueueName", "testCorrelationId", responseListener);

            String jsonResponse = "{" +
                    "\"status\":200," +
                    "\"platform\":{}" +
                    "}";

            assertEquals(1, emptyConsumerReturnListener.getListenerMap().size());

            AMQP.BasicProperties props = new AMQP.BasicProperties()
                    .builder()
                    .correlationId("testCorrelationId")
                    .build();
            Envelope envelope = new Envelope(0, false, "", "testReplyQueueName");

            ReplyConsumer replyConsumer = new ReplyConsumer(null, "testCorrelationId", responseListener, emptyConsumerReturnListener);
            replyConsumer.handleDelivery(null, envelope, props, jsonResponse.getBytes());

            assertEquals(0, emptyConsumerReturnListener.getListenerMap().size());

        } catch (IOException e) {
            fail();
        }

    }

    @Test
    public void testReplyConsumer_nullEmptyConsumerReturnListener() {
        try {
            IRpcResponseListener responseListener = mock(IRpcResponseListener.class);
            EmptyConsumerReturnListener emptyConsumerReturnListener = null;

            String jsonResponse = "{" +
                    "\"status\":200," +
                    "\"platform\":{}" +
                    "}";

            AMQP.BasicProperties props = new AMQP.BasicProperties()
                    .builder()
                    .correlationId("testCorrelationId")
                    .build();
            Envelope envelope = new Envelope(0, false, "", "testReplyQueueName");

            ReplyConsumer replyConsumer = new ReplyConsumer(null, "testCorrelationId", responseListener, emptyConsumerReturnListener);
            replyConsumer.handleDelivery(null, envelope, props, jsonResponse.getBytes());

            verify(responseListener, times(1)).onRpcResponseReceive(any());
        } catch (IOException e) {
            fail();
        }

    }

    @Test
    public void testReplyConsumer_nullBody() {
        try {
            IRpcResponseListener responseListener = mock(IRpcResponseListener.class);
            EmptyConsumerReturnListener emptyConsumerReturnListener = null;

            AMQP.BasicProperties props = new AMQP.BasicProperties()
                    .builder()
                    .correlationId("testCorrelationId")
                    .build();
            Envelope envelope = new Envelope(0, false, "", "testReplyQueueName");

            ReplyConsumer replyConsumer = new ReplyConsumer(null, "testCorrelationId", responseListener, emptyConsumerReturnListener);
            replyConsumer.handleDelivery(null, envelope, props, null);

            verify(responseListener, times(1)).onRpcResponseReceive(any());
        } catch (IOException e) {
            fail();
        }

    }

    @Test
    public void testReplyConsumer_malformedBody() {
        try {
            IRpcResponseListener responseListener = mock(IRpcResponseListener.class);
            EmptyConsumerReturnListener emptyConsumerReturnListener = null;

            String jsonResponse = "{not a valid json}";

            AMQP.BasicProperties props = new AMQP.BasicProperties()
                    .builder()
                    .correlationId("testCorrelationId")
                    .build();
            Envelope envelope = new Envelope(0, false, "", "testReplyQueueName");

            ReplyConsumer replyConsumer = new ReplyConsumer(null, "testCorrelationId", responseListener, emptyConsumerReturnListener);
            replyConsumer.handleDelivery(null, envelope, props, jsonResponse.getBytes());

            verify(responseListener, times(1)).onRpcResponseReceive(any());
        } catch (IOException e) {
            fail();
        }

    }

    @Test
    public void testReplyConsumer_full() {
        try {
            IRpcResponseListener responseListener = mock(IRpcResponseListener.class);
            EmptyConsumerReturnListener emptyConsumerReturnListener = spy(EmptyConsumerReturnListener.class);
            emptyConsumerReturnListener.addListener("testReplyQueueName", "testCorrelationId", responseListener);

            String jsonResponse = "{" +
                    "\"status\":200," +
                    "\"platform\":{}" +
                    "}";

            assertEquals(1, emptyConsumerReturnListener.getListenerMap().size());

            AMQP.BasicProperties props = new AMQP.BasicProperties()
                    .builder()
                    .correlationId("testCorrelationId")
                    .build();
            Envelope envelope = new Envelope(0, false, "", "testReplyQueueName");

            ReplyConsumer replyConsumer = new ReplyConsumer(null, "testCorrelationId", responseListener, emptyConsumerReturnListener);
            replyConsumer.handleDelivery(null, envelope, props, jsonResponse.getBytes());

            assertEquals(0, emptyConsumerReturnListener.getListenerMap().size());
            verify(responseListener, times(1)).onRpcResponseReceive(any());

        } catch (IOException e) {
            fail();
        }

    }

}
