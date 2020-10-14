package org.axonframework.extensions.tracing;


import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.queryhandling.QueryMessage;

/**
 * Enumeration of the possible {@link org.axonframework.messaging.Message} information to be added as tags to a {@link
 * io.opentracing.Span}.
 *
 * @author Corrado Musumeci
 * @author Steven van Beelen
 * @since 4.4
 */
public enum MessageTag {

    /**
     * Tag referring to the {@link org.axonframework.messaging.Message} identifier.
     */
    MESSAGE_ID("axon.message.id"),
    /**
     * Tag referring to the {@link org.axonframework.messaging.Message} aggregate identifier. Note that this could only
     * be available for {@link org.axonframework.eventhandling.DomainEventMessage} instances.
     */
    AGGREGATE_ID("axon.message.aggregate-identifier"),
    /**
     * Tag referring to the type of {@link org.axonframework.messaging.Message}.
     */
    MESSAGE_TYPE("axon.message.type"),
    /**
     * Tag referring to the payload type contained in the {@link org.axonframework.messaging.Message}.
     */
    PAYLOAD_TYPE("axon.message.payload-type"),
    /**
     * Tag referring to the name of the {@link org.axonframework.messaging.Message}. Might refer to the {@link
     * CommandMessage#getCommandName()} or {@link QueryMessage#getQueryName()} for example. Can be similar to the
     * payload type of a {@code Message} if nothing more specific, like the {@link QueryMessage#getQueryName()}, has
     * been specified.
     */
    MESSAGE_NAME("axon.message.message-name"),
    /**
     * Tag referring to the payload of a {@link org.axonframework.messaging.Message}.
     */
    PAYLOAD("axon.message.payload");

    private final String tagKey;

    MessageTag(String tagName) {
        this.tagKey = tagName;
    }

    /**
     * Retrieve the tag key to be used on the {@link io.opentracing.Span}.
     *
     * @return the tag key to be used on the {@link io.opentracing.Span}
     */
    public String getTagKey() {
        return tagKey;
    }
}
