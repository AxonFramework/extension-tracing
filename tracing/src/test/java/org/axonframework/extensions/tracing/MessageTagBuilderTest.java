package org.axonframework.extensions.tracing;

import io.opentracing.Tracer;
import org.axonframework.messaging.GenericMessage;
import org.axonframework.messaging.Message;
import org.junit.jupiter.api.*;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class validating the {@link MessageTagBuilder} functional interface.
 *
 * @author Corrado Musumeci
 * @author Steven van Beelen
 */
class MessageTagBuilderTest {

    private Tracer.SpanBuilder testSpanBuilder;
    private final Message<?> testMessage = GenericMessage.asMessage("our-awesome-message");

    @BeforeEach
    void setUp() {
        testSpanBuilder = mock(Tracer.SpanBuilder.class);
    }

    @Test
    void testWithInvokesBothMessageTagBuilderInstances() {
        AtomicBoolean invokedFirst = new AtomicBoolean(false);
        MessageTagBuilder first = (builder, message) -> {
            invokedFirst.set(true);
            return builder;
        };
        AtomicBoolean invokedSecond = new AtomicBoolean(false);
        MessageTagBuilder second = (builder, message) -> {
            invokedSecond.set(true);
            return builder;
        };

        MessageTagBuilder testSubject = first.with(second);

        testSubject.build(testSpanBuilder, testMessage);
        assertTrue(invokedFirst.get());
        assertTrue(invokedSecond.get());
    }

    @Test
    void testNoOpReturnsUnadjustedSpanBuilder() {
        MessageTagBuilder.builder().build(testSpanBuilder, testMessage);
        verifyNoInteractions(testSpanBuilder);
    }
}