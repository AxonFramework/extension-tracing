package org.axonframework.extensions.tracing;

import io.opentracing.Tracer;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.messaging.Message;
import org.axonframework.queryhandling.QueryMessage;

/**
 * Utility class providing methods useful for attaching information to Spans.
 *
 * @author Lucas Campos
 * @since 4.0
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
     * <li>{@link QueryMessage}, returning  the queryName if it diverts from the (fully qualified) payload type, otherwise return payload type simple name ",</li>
     * <li>{@link CommandMessage}, returning the commandName if it diverts from the (fully qualified) payload type, otherwise return payload type simple name ",</li>
     * <li>otherwise returns payload type simple name"</li>
     * </ul>
     *
     * @param message The message to resolve the type of
     * @return a String describing the type of message
     */
    public static String messageName(Message message) {
        if (message instanceof QueryMessage) {
            return messageName(message.getPayloadType(), ((QueryMessage) message).getQueryName());
        } else if (message instanceof CommandMessage) {
            return messageName(message.getPayloadType(), ((CommandMessage) message).getCommandName());
        }
        return message.getPayloadType().getSimpleName();
    }

    static String messageName(Class payloadType, String name) {
        if (!payloadType.getName().equals(name)) {
            return name;
        }
        return payloadType.getSimpleName();
    }
}
