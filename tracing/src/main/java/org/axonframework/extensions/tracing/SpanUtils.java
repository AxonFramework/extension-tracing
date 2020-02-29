package org.axonframework.extensions.tracing;

import io.opentracing.Tracer;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.messaging.Message;
import org.axonframework.queryhandling.QueryMessage;

/**
 * Utility class providing methods useful for attaching information to Spans
 */
public class SpanUtils {
    private static final String TAG_AXON_PAYLOAD_TYPE = "axon.message.payloadtype";
    private static final String TAG_AXON_ID = "axon.message.id";
    private static final String TAG_AXON_AGGEGRATE_ID = "axon.message.aggregateIdentifier";
    private static final String TAG_AXON_MSG_TYPE = "axon.message.type";
    private static final String TAG_AXON_COMMAND_NAME = "axon.message.commandname";

    /**
     * Registers message-specific tags to the given {@code spanBuilder} based on the given {@code message}.
     *
     * @param spanBuilder The Span Builder to register the tags with
     * @param message     The message to retrieve details from
     * @return a builder with tags attached
     */
    public static Tracer.SpanBuilder withMessageTags(Tracer.SpanBuilder spanBuilder, Message<?> message) {
        Tracer.SpanBuilder sb = spanBuilder.withTag(TAG_AXON_ID, message.getIdentifier())
                                           .withTag(TAG_AXON_MSG_TYPE, messageName(message))
                                           .withTag(TAG_AXON_PAYLOAD_TYPE, message.getPayloadType().getName());
        if (message instanceof CommandMessage) {
            return sb.withTag(TAG_AXON_COMMAND_NAME, ((CommandMessage<?>) message).getCommandName());
        } else if (message instanceof DomainEventMessage) {
            return sb.withTag(TAG_AXON_AGGEGRATE_ID, ((DomainEventMessage<?>) message).getAggregateIdentifier());
        }
        return sb;
    }

    /**
     * Resolves the type of message as a String, for use in description of operations.
     * <p>
     * This method will check if the message is a
     * <ul>
     * <li>{@link QueryMessage}, returning  the queryName",</li>
     * <li>{@link CommandMessage}, returning the commandName",</li>
     * <li>{@link EventMessage}, returning the event type,</li>
     * <li>otherwise returns "{@code Message}"</li>
     * </ul>
     *
     * @param message The message to resolve the type of
     * @return a String describing the type of message
     */
    public static String messageName(Message message) {
        if (message instanceof QueryMessage && !message.getPayloadType().getName().equals(((QueryMessage) message).getQueryName())) {
            return ((QueryMessage) message).getQueryName();
        } else if (message instanceof CommandMessage && !message.getPayloadType().getName().equals(((CommandMessage) message).getCommandName())) {
            return ((CommandMessage) message).getCommandName();
        }
        return message.getPayloadType().getSimpleName();
    }

    public static <Q> String messageName(Q query, String queryName) {
        if (!query.getClass().getName().equals(queryName)) {
            return queryName;
        }
        return query.getClass().getSimpleName();
    }
}
