package org.axonframework.extensions.tracing;

import io.opentracing.Tracer;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.common.AxonConfigurationException;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericDomainEventMessage;
import org.axonframework.eventhandling.GenericEventMessage;
import org.axonframework.messaging.GenericMessage;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.GenericQueryMessage;
import org.axonframework.queryhandling.QueryMessage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.invocation.*;
import org.mockito.junit.jupiter.*;

import static org.axonframework.extensions.tracing.SpanUtils.messageName;
import static org.axonframework.extensions.tracing.SpanUtils.resolveMessageType;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class validating the {@link MessageTagBuilderService}.
 *
 * @author Corrado Musumeci
 * @author Steven van Beelen
 */
@ExtendWith(MockitoExtension.class)
class MessageTagBuilderServiceTest {

    private Tracer.SpanBuilder testSpanBuilder;

    private MessageTagBuilderService testSubject;

    @BeforeEach
    void setUp() {
        testSpanBuilder = mock(Tracer.SpanBuilder.class);

        testSubject = MessageTagBuilderService.defaultService();
    }

    @Test
    void testWithMessageTagsForCommandMessage() {
        when(testSpanBuilder.withTag(anyString(), anyString())).thenAnswer(InvocationOnMock::getMock);

        Message<String> message = GenericCommandMessage.asCommandMessage("some-command");

        testSubject.withMessageTags(testSpanBuilder, message);

        verify(testSpanBuilder).withTag(MessageTag.MESSAGE_ID.getTagKey(), message.getIdentifier());
        verify(testSpanBuilder).withTag(MessageTag.MESSAGE_TYPE.getTagKey(), resolveMessageType(message));
        verify(testSpanBuilder).withTag(MessageTag.PAYLOAD_TYPE.getTagKey(), message.getPayloadType().getName());
        verify(testSpanBuilder).withTag(MessageTag.MESSAGE_NAME.getTagKey(), messageName(message));
        verify(testSpanBuilder, times(0)).withTag(eq(MessageTag.AGGREGATE_ID.getTagKey()), anyString());
    }

    @Test
    void testWithMessageTagsForEventMessage() {
        when(testSpanBuilder.withTag(anyString(), anyString())).thenAnswer(InvocationOnMock::getMock);

        Message<String> testMessage = GenericEventMessage.asEventMessage("some-event");

        testSubject.withMessageTags(testSpanBuilder, testMessage);

        verify(testSpanBuilder).withTag(MessageTag.MESSAGE_ID.getTagKey(), testMessage.getIdentifier());
        verify(testSpanBuilder).withTag(MessageTag.MESSAGE_TYPE.getTagKey(), resolveMessageType(testMessage));
        verify(testSpanBuilder).withTag(MessageTag.PAYLOAD_TYPE.getTagKey(), testMessage.getPayloadType().getName());
        verify(testSpanBuilder, times(0)).withTag(eq(MessageTag.AGGREGATE_ID.getTagKey()), anyString());
        verify(testSpanBuilder, times(0)).withTag(eq(MessageTag.MESSAGE_NAME.getTagKey()), anyString());
    }

    @Test
    void testWithMessageTagsForQueryMessage() {
        when(testSpanBuilder.withTag(anyString(), anyString())).thenAnswer(InvocationOnMock::getMock);

        Message<String> testMessage = new GenericQueryMessage<>("some-query", ResponseTypes.instanceOf(String.class));

        testSubject.withMessageTags(testSpanBuilder, testMessage);

        verify(testSpanBuilder).withTag(MessageTag.MESSAGE_ID.getTagKey(), testMessage.getIdentifier());
        verify(testSpanBuilder).withTag(MessageTag.MESSAGE_TYPE.getTagKey(), resolveMessageType(testMessage));
        verify(testSpanBuilder).withTag(MessageTag.PAYLOAD_TYPE.getTagKey(), testMessage.getPayloadType().getName());
        verify(testSpanBuilder).withTag(MessageTag.MESSAGE_NAME.getTagKey(), messageName(testMessage));
        verify(testSpanBuilder, times(0)).withTag(eq(MessageTag.AGGREGATE_ID.getTagKey()), anyString());
    }

    @Test
    void testWithMessageTagsForUnknownMessageType() {
        testSubject.withMessageTags(testSpanBuilder, new GenericMessage<>("some-payload"));

        verifyNoInteractions(testSpanBuilder);
    }

    @Test
    void testWithCommandMessageTags() {
        when(testSpanBuilder.withTag(anyString(), anyString())).thenAnswer(InvocationOnMock::getMock);

        CommandMessage<String> testCommand = GenericCommandMessage.asCommandMessage("some-command");

        testSubject.withCommandMessageTags(testSpanBuilder, testCommand);

        verify(testSpanBuilder).withTag(MessageTag.MESSAGE_ID.getTagKey(), testCommand.getIdentifier());
        verify(testSpanBuilder).withTag(MessageTag.MESSAGE_TYPE.getTagKey(), resolveMessageType(testCommand));
        verify(testSpanBuilder).withTag(MessageTag.PAYLOAD_TYPE.getTagKey(), testCommand.getPayloadType().getName());
        verify(testSpanBuilder).withTag(MessageTag.MESSAGE_NAME.getTagKey(), messageName(testCommand));
        verify(testSpanBuilder, times(0)).withTag(eq(MessageTag.AGGREGATE_ID.getTagKey()), anyString());
    }

    @Test
    void testWithEventMessageTags() {
        when(testSpanBuilder.withTag(anyString(), anyString())).thenAnswer(InvocationOnMock::getMock);

        EventMessage<String> testEvent = GenericEventMessage.asEventMessage("some-event");

        testSubject.withEvenMessageTags(testSpanBuilder, testEvent);

        verify(testSpanBuilder).withTag(MessageTag.MESSAGE_ID.getTagKey(), testEvent.getIdentifier());
        verify(testSpanBuilder).withTag(MessageTag.MESSAGE_TYPE.getTagKey(), resolveMessageType(testEvent));
        verify(testSpanBuilder).withTag(MessageTag.PAYLOAD_TYPE.getTagKey(), testEvent.getPayloadType().getName());
        verify(testSpanBuilder, times(0)).withTag(eq(MessageTag.AGGREGATE_ID.getTagKey()), anyString());
        verify(testSpanBuilder, times(0)).withTag(eq(MessageTag.MESSAGE_NAME.getTagKey()), anyString());
    }

    @Test
    void testWithDomainEventMessageTags() {
        when(testSpanBuilder.withTag(anyString(), anyString())).thenAnswer(InvocationOnMock::getMock);

        DomainEventMessage<String> testEvent =
                new GenericDomainEventMessage<>("some-type", "some-aggregate-id", 42, "some-payload");

        testSubject.withEvenMessageTags(testSpanBuilder, testEvent);

        verify(testSpanBuilder).withTag(MessageTag.MESSAGE_ID.getTagKey(), testEvent.getIdentifier());
        verify(testSpanBuilder).withTag(MessageTag.MESSAGE_TYPE.getTagKey(), resolveMessageType(testEvent));
        verify(testSpanBuilder).withTag(MessageTag.PAYLOAD_TYPE.getTagKey(), testEvent.getPayloadType().getName());
        verify(testSpanBuilder).withTag(MessageTag.AGGREGATE_ID.getTagKey(), testEvent.getAggregateIdentifier());
        verify(testSpanBuilder, times(0)).withTag(eq(MessageTag.MESSAGE_NAME.getTagKey()), anyString());
    }

    @Test
    void testWithQueryMessageTags() {
        when(testSpanBuilder.withTag(anyString(), anyString())).thenAnswer(InvocationOnMock::getMock);

        QueryMessage<String, String> testQuery =
                new GenericQueryMessage<>("some-query", ResponseTypes.instanceOf(String.class));

        testSubject.withQueryMessageTags(testSpanBuilder, testQuery);

        verify(testSpanBuilder).withTag(MessageTag.MESSAGE_ID.getTagKey(), testQuery.getIdentifier());
        verify(testSpanBuilder).withTag(MessageTag.MESSAGE_TYPE.getTagKey(), resolveMessageType(testQuery));
        verify(testSpanBuilder).withTag(MessageTag.PAYLOAD_TYPE.getTagKey(), testQuery.getPayloadType().getName());
        verify(testSpanBuilder).withTag(MessageTag.MESSAGE_NAME.getTagKey(), messageName(testQuery));
        verify(testSpanBuilder, times(0)).withTag(eq(MessageTag.AGGREGATE_ID.getTagKey()), anyString());
    }

    @Test
    void testWithCustomCommandMessageTags() {
        when(testSpanBuilder.withTag(anyString(), anyString())).thenAnswer(InvocationOnMock::getMock);

        CommandMessage<String> testCommand = GenericCommandMessage.asCommandMessage("some-command");

        MessageTagBuilderService testSubjectWithCustomCommandTags =
                MessageTagBuilderService.builder()
                                        .commandMessageTags(MessageTag.MESSAGE_TYPE, MessageTag.MESSAGE_NAME)
                                        .build();

        testSubjectWithCustomCommandTags.withCommandMessageTags(testSpanBuilder, testCommand);

        verify(testSpanBuilder).withTag(MessageTag.MESSAGE_TYPE.getTagKey(), resolveMessageType(testCommand));
        verify(testSpanBuilder).withTag(MessageTag.MESSAGE_NAME.getTagKey(), messageName(testCommand));
        verify(testSpanBuilder, times(0)).withTag(eq(MessageTag.MESSAGE_ID.getTagKey()), anyString());
        verify(testSpanBuilder, times(0)).withTag(eq(MessageTag.PAYLOAD_TYPE.getTagKey()), anyString());
        verify(testSpanBuilder, times(0)).withTag(eq(MessageTag.AGGREGATE_ID.getTagKey()), anyString());
    }

    @Test
    void testWithCustomEventMessageTags() {
        when(testSpanBuilder.withTag(anyString(), anyString())).thenAnswer(InvocationOnMock::getMock);

        EventMessage<String> testEvent = GenericEventMessage.asEventMessage("some-event");

        MessageTagBuilderService testSubjectWithCustomEventTags =
                MessageTagBuilderService.builder()
                                        .eventMessageTags(MessageTag.MESSAGE_ID, MessageTag.PAYLOAD_TYPE)
                                        .build();

        testSubjectWithCustomEventTags.withEvenMessageTags(testSpanBuilder, testEvent);

        verify(testSpanBuilder).withTag(MessageTag.MESSAGE_ID.getTagKey(), testEvent.getIdentifier());
        verify(testSpanBuilder).withTag(MessageTag.PAYLOAD_TYPE.getTagKey(), testEvent.getPayloadType().getName());
        verify(testSpanBuilder, times(0)).withTag(eq(MessageTag.MESSAGE_TYPE.getTagKey()), anyString());
        verify(testSpanBuilder, times(0)).withTag(eq(MessageTag.MESSAGE_NAME.getTagKey()), anyString());
        verify(testSpanBuilder, times(0)).withTag(eq(MessageTag.AGGREGATE_ID.getTagKey()), anyString());
    }

    @Test
    void testWithCustomQueryMessageTags() {
        when(testSpanBuilder.withTag(anyString(), anyString())).thenAnswer(InvocationOnMock::getMock);

        QueryMessage<String, String> testQuery =
                new GenericQueryMessage<>("some-query", ResponseTypes.instanceOf(String.class));

        MessageTagBuilderService testSubjectWithCustomCommandTags =
                MessageTagBuilderService.builder()
                                        .queryMessageTags(MessageTag.MESSAGE_TYPE, MessageTag.PAYLOAD_TYPE)
                                        .build();

        testSubjectWithCustomCommandTags.withQueryMessageTags(testSpanBuilder, testQuery);

        verify(testSpanBuilder).withTag(MessageTag.MESSAGE_TYPE.getTagKey(), resolveMessageType(testQuery));
        verify(testSpanBuilder).withTag(MessageTag.PAYLOAD_TYPE.getTagKey(), testQuery.getPayloadType().getName());
        verify(testSpanBuilder, times(0)).withTag(eq(MessageTag.MESSAGE_ID.getTagKey()), anyString());
        verify(testSpanBuilder, times(0)).withTag(eq(MessageTag.MESSAGE_NAME.getTagKey()), anyString());
        verify(testSpanBuilder, times(0)).withTag(eq(MessageTag.AGGREGATE_ID.getTagKey()), anyString());
    }

    @Test
    void testBuildWithNullCommandMessageTagBuilderThrowsAxonConfigurationException() {
        assertThrows(
                AxonConfigurationException.class,
                () -> MessageTagBuilderService.builder().commandMessageTags((MessageTagBuilder) null)
        );
    }

    @Test
    void testBuildWithNullEventMessageTagBuilderThrowsAxonConfigurationException() {
        assertThrows(
                AxonConfigurationException.class,
                () -> MessageTagBuilderService.builder().eventMessageTags((MessageTagBuilder) null)
        );
    }

    @Test
    void testBuildWithNullQueryMessageTagBuilderThrowsAxonConfigurationException() {
        assertThrows(
                AxonConfigurationException.class,
                () -> MessageTagBuilderService.builder().queryMessageTags((MessageTagBuilder) null)
        );
    }
}