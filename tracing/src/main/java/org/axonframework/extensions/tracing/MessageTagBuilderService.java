/*
 * Copyright (c) 2010-2020. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.extensions.tracing;

import io.opentracing.Tracer;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.messaging.Message;
import org.axonframework.queryhandling.QueryMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;

import static java.util.Arrays.asList;
import static org.axonframework.common.BuilderUtils.assertNonNull;
import static org.axonframework.extensions.tracing.SpanUtils.messageName;
import static org.axonframework.extensions.tracing.SpanUtils.resolveMessageType;

/**
 * Service constructing the necessary message tags to be used on a {@link Tracer.SpanBuilder}, based on a given {@link
 * Message}. Allows further configuration of which components of the {@code Message} should be inserted on the {@link
 * io.opentracing.Span} through the {@link Builder} by providing the desired {@link MessageTag}s.
 *
 * @author Corrado Musumeci
 * @author Steven van Beelen
 * @since 4.4
 */
public class MessageTagBuilderService {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final MessageTagBuilder commandMessageTags;
    private final MessageTagBuilder eventMessageTags;
    private final MessageTagBuilder queryMessageTags;

    /**
     * Instantiate a Builder to be able to create a {@link MessageTagBuilderService}.
     * <p>
     * A {@link CommandMessage}'s tags are defaulted to using the {@link MessageTag#MESSAGE_ID}, {@link
     * MessageTag#MESSAGE_TYPE}, {@link MessageTag#PAYLOAD_TYPE} and {@link MessageTag#MESSAGE_NAME}.
     * <p>
     * The {@link EventMessage}'s tags are defaulted to using the {@link MessageTag#MESSAGE_ID}, {@link
     * MessageTag#AGGREGATE_ID} (which will only be set in case of a {@link DomainEventMessage}), {@link
     * MessageTag#MESSAGE_TYPE} and {@link MessageTag#PAYLOAD_TYPE}.
     * <p>
     * Lastly, {@link QueryMessage} tags are defaulted to using the {@link MessageTag#MESSAGE_ID}, {@link
     * MessageTag#MESSAGE_TYPE}, {@link MessageTag#PAYLOAD_TYPE} and {@link MessageTag#MESSAGE_NAME}.
     *
     * @return a Builder to be able to create a {@link MessageTagBuilderService}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Instantiate a default {@link MessageTagBuilderService}.
     *
     * @return a default {@link MessageTagBuilderService}
     */
    public static MessageTagBuilderService defaultService() {
        return builder().build();
    }

    /**
     * Instantiate a {@link MessageTagBuilderService} based on the configuration contained in the {@link Builder}.
     *
     * @param builder the {@link Builder} used to instantiate a {@link MessageTagBuilderService} instance
     */
    protected MessageTagBuilderService(Builder builder) {
        this.commandMessageTags = builder.commandMessageTags;
        this.eventMessageTags = builder.eventMessageTags;
        this.queryMessageTags = builder.queryMessageTags;
    }

    private static Tracer.SpanBuilder withMessageId(Tracer.SpanBuilder spanBuilder, Message<?> message) {
        return spanBuilder.withTag(MessageTag.MESSAGE_ID.getTagKey(), message.getIdentifier());
    }

    private static Tracer.SpanBuilder withAggregateId(Tracer.SpanBuilder spanBuilder, Message<?> message) {
        if (message instanceof DomainEventMessage) {
            return spanBuilder.withTag(
                    MessageTag.AGGREGATE_ID.getTagKey(), ((DomainEventMessage<?>) message).getAggregateIdentifier()
            );
        }
        return spanBuilder;
    }

    private static Tracer.SpanBuilder withMessageType(Tracer.SpanBuilder spanBuilder, Message<?> message) {
        return spanBuilder.withTag(MessageTag.MESSAGE_TYPE.getTagKey(), resolveMessageType(message));
    }

    private static Tracer.SpanBuilder withPayloadType(Tracer.SpanBuilder spanBuilder, Message<?> message) {
        return spanBuilder.withTag(MessageTag.PAYLOAD_TYPE.getTagKey(), message.getPayloadType().getName());
    }

    private static Tracer.SpanBuilder withMessageName(Tracer.SpanBuilder spanBuilder, Message<?> message) {
        return spanBuilder.withTag(MessageTag.MESSAGE_NAME.getTagKey(), messageName(message));
    }

    private static Tracer.SpanBuilder withPayload(Tracer.SpanBuilder spanBuilder, Message<?> message) {
        return spanBuilder.withTag(MessageTag.PAYLOAD.getTagKey(), message.getPayload().toString());
    }

    /**
     * Add the given {@code message}'s information as tags to the given {@code spanBuilder}. Will validate if the given
     * {@code message} is of type {@link CommandMessage}, {@link EventMessage} or {@link QueryMessage} and based on that
     * invokes {@link #withCommandMessageTags(Tracer.SpanBuilder, CommandMessage)} , {@link
     * #withEvenMessageTags(Tracer.SpanBuilder, EventMessage)} or {@link #withQueryMessageTags(Tracer.SpanBuilder,
     * QueryMessage)} respectively. If the {@code message} is none of these types, the {@code spanBuilder} will be
     * returned as is.
     *
     * @param spanBuilder the {@link Tracer.SpanBuilder} to attach the given {@code message}'s information to as tags
     * @param message     the {@link Message} to retrieve the tag information from
     * @return the {@code spanBuilder} with added tags from the given {@code message}
     */
    public Tracer.SpanBuilder withMessageTags(Tracer.SpanBuilder spanBuilder, Message<?> message) {
        if (message instanceof CommandMessage) {
            return withCommandMessageTags(spanBuilder, (CommandMessage<?>) message);
        }
        if (message instanceof EventMessage) {
            return withEvenMessageTags(spanBuilder, (EventMessage<?>) message);
        }
        if (message instanceof QueryMessage) {
            return withQueryMessageTags(spanBuilder, (QueryMessage<?, ?>) message);
        }
        logger.warn("Message of type [{}] cannot be handled", message.getClass());
        return spanBuilder;
    }

    /**
     * Add the given {@code commandMessage}'s information as tags to the given {@code spanBuilder}.
     *
     * @param spanBuilder    the {@link Tracer.SpanBuilder} to attach the given {@code commandMessage}'s information to
     *                       as tags
     * @param commandMessage the {@link CommandMessage} to retrieve the tag information from
     * @return the {@code spanBuilder} with added tags from the given {@code commandMessage}
     */
    public Tracer.SpanBuilder withCommandMessageTags(Tracer.SpanBuilder spanBuilder, CommandMessage<?> commandMessage) {
        return commandMessageTags.build(spanBuilder, commandMessage);
    }

    /**
     * Add the given {@code eventMessage}'s information as tags to the given {@code spanBuilder}.
     *
     * @param spanBuilder  the {@link Tracer.SpanBuilder} to attach the given {@code eventMessage}'s information to as
     *                     tags
     * @param eventMessage the {@link EventMessage} to retrieve the tag information from
     * @return the {@code spanBuilder} with added tags from the given {@code eventMessage}
     */
    public Tracer.SpanBuilder withEvenMessageTags(Tracer.SpanBuilder spanBuilder, EventMessage<?> eventMessage) {
        return eventMessageTags.build(spanBuilder, eventMessage);
    }

    /**
     * Add the given {@code queryMessage}'s information as tags to the given {@code spanBuilder}.
     *
     * @param spanBuilder  the {@link Tracer.SpanBuilder} to attach the given {@code queryMessage}'s information to as
     *                     tags
     * @param queryMessage the {@link QueryMessage} to retrieve the tag information from
     * @return the {@code spanBuilder} with added tags from the given {@code queryMessage}
     */
    public Tracer.SpanBuilder withQueryMessageTags(Tracer.SpanBuilder spanBuilder, QueryMessage<?, ?> queryMessage) {
        return queryMessageTags.build(spanBuilder, queryMessage);
    }

    /**
     * Builder class to instantiate a {@link MessageTagBuilderService}.
     * <p>
     * A {@link CommandMessage}'s tags are defaulted to using the {@link MessageTag#MESSAGE_ID}, {@link
     * MessageTag#MESSAGE_TYPE}, {@link MessageTag#PAYLOAD_TYPE} and {@link MessageTag#MESSAGE_NAME}.
     * <p>
     * The {@link EventMessage}'s tags are defaulted to using the {@link MessageTag#MESSAGE_ID}, {@link
     * MessageTag#AGGREGATE_ID} (which will only be set in case of a {@link DomainEventMessage}), {@link
     * MessageTag#MESSAGE_TYPE} and {@link MessageTag#PAYLOAD_TYPE}.
     * <p>
     * Lastly, {@link QueryMessage} tags are defaulted to using the {@link MessageTag#MESSAGE_ID}, {@link
     * MessageTag#MESSAGE_TYPE}, {@link MessageTag#PAYLOAD_TYPE} and {@link MessageTag#MESSAGE_NAME}.
     */
    public static class Builder {

        private static final MessageTagBuilder MESSAGE_ID = MessageTagBuilderService::withMessageId;
        private static final MessageTagBuilder AGGREGATE_ID = MessageTagBuilderService::withAggregateId;
        private static final MessageTagBuilder MESSAGE_TYPE = MessageTagBuilderService::withMessageType;
        private static final MessageTagBuilder PAYLOAD_TYPE = MessageTagBuilderService::withPayloadType;
        private static final MessageTagBuilder MESSAGE_NAME = MessageTagBuilderService::withMessageName;
        private static final MessageTagBuilder PAYLOAD = MessageTagBuilderService::withPayload;

        private MessageTagBuilder commandMessageTags =
                (spanBuilder, message) -> MessageTagBuilder.builder()
                                                           .with(MESSAGE_ID)
                                                           .with(MESSAGE_TYPE)
                                                           .with(PAYLOAD_TYPE)
                                                           .with(MESSAGE_NAME)
                                                           .build(spanBuilder, message);
        private MessageTagBuilder eventMessageTags =
                (spanBuilder, message) -> MessageTagBuilder.builder()
                                                           .with(MESSAGE_ID)
                                                           .with(AGGREGATE_ID)
                                                           .with(MESSAGE_TYPE)
                                                           .with(PAYLOAD_TYPE)
                                                           .build(spanBuilder, message);
        private MessageTagBuilder queryMessageTags =
                (spanBuilder, message) -> MessageTagBuilder.builder()
                                                           .with(MESSAGE_ID)
                                                           .with(MESSAGE_TYPE)
                                                           .with(PAYLOAD_TYPE)
                                                           .with(MESSAGE_NAME)
                                                           .build(spanBuilder, message);

        /**
         * Sets the given collection of {@code commandMessageTags} to drive the information to add to a {@link
         * Tracer.SpanBuilder} for {@link CommandMessage} instances. Defaults to the tags {@link MessageTag#MESSAGE_ID},
         * {@link MessageTag#MESSAGE_TYPE}, {@link MessageTag#PAYLOAD_TYPE} and {@link MessageTag#MESSAGE_NAME}.
         *
         * @param commandMessageTags a collection of {@link MessageTag}s driving the information to add to a {@link
         *                           Tracer.SpanBuilder}
         * @return the current Builder instance, for fluent interfacing
         */
        public Builder commandMessageTags(MessageTag... commandMessageTags) {
            return commandMessageTags(asList(commandMessageTags));
        }

        /**
         * Sets the given {@link List} of {@link MessageTag}s to drive the information to add {@link Tracer.SpanBuilder}
         * for {@link CommandMessage} instances. Defaults to the tags {@link MessageTag#MESSAGE_ID}, {@link
         * MessageTag#MESSAGE_TYPE}, {@link MessageTag#PAYLOAD_TYPE} and {@link MessageTag#MESSAGE_NAME}.
         *
         * @param commandMessageTags a {@link List} of {@link MessageTag}s driving the information to add to a {@link
         *                           Tracer.SpanBuilder}
         * @return the current Builder instance, for fluent interfacing
         */
        public Builder commandMessageTags(List<MessageTag> commandMessageTags) {
            return commandMessageTags(constructMessageTagBuilder(commandMessageTags));
        }

        /**
         * Sets the given {@link MessageTagBuilder} to be used when a {@link CommandMessage} is given to this service.
         * Defaults to the tags {@link MessageTag#MESSAGE_ID}, {@link MessageTag#MESSAGE_TYPE}, {@link
         * MessageTag#PAYLOAD_TYPE} and {@link MessageTag#MESSAGE_NAME}.
         *
         * @param commandMessageTagBuilder a {@link MessageTagBuilder} used to add {@link CommandMessage} specific
         *                                 information to a {@link Tracer.SpanBuilder}
         * @return the current Builder instance, for fluent interfacing
         */
        public Builder commandMessageTags(MessageTagBuilder commandMessageTagBuilder) {
            assertNonNull(commandMessageTagBuilder, "The commandMessageTagBuilder builder may not be null");
            this.commandMessageTags = commandMessageTagBuilder;
            return this;
        }

        /**
         * Sets the given collection of {@code eventMessageTags} to drive the information to add to a {@link
         * Tracer.SpanBuilder} for {@link EventMessage} instances. Defaults to the tags {@link MessageTag#MESSAGE_ID},
         * {@link MessageTag#AGGREGATE_ID} (which will only be set in case of a {@link DomainEventMessage}), {@link
         * MessageTag#MESSAGE_TYPE} and {@link MessageTag#PAYLOAD_TYPE}.
         *
         * @param eventMessageTags a collection of {@link MessageTag}s driving the information to add to a {@link
         *                         Tracer.SpanBuilder}
         * @return the current Builder instance, for fluent interfacing
         */
        public Builder eventMessageTags(MessageTag... eventMessageTags) {
            return eventMessageTags(asList(eventMessageTags));
        }

        /**
         * Sets the given {@link List} of {@link MessageTag}s to drive the information to add {@link Tracer.SpanBuilder}
         * for {@link EventMessage} instances. Defaults to the tags {@link MessageTag#MESSAGE_ID}, {@link
         * MessageTag#AGGREGATE_ID} (which will only be set in case of a {@link DomainEventMessage}), {@link
         * MessageTag#MESSAGE_TYPE} and {@link MessageTag#PAYLOAD_TYPE}.
         *
         * @param eventMessageTags a {@link List} of {@link MessageTag}s driving the information to add to a {@link
         *                         Tracer.SpanBuilder}
         * @return the current Builder instance, for fluent interfacing
         */
        public Builder eventMessageTags(List<MessageTag> eventMessageTags) {
            return eventMessageTags(constructMessageTagBuilder(eventMessageTags));
        }

        /**
         * Sets the given {@link MessageTagBuilder} to be used when a {@link EventMessage} is given to this service.
         * Defaults to the tags {@link MessageTag#MESSAGE_ID}, {@link MessageTag#AGGREGATE_ID} (which will only be set
         * in case of a {@link DomainEventMessage}), {@link MessageTag#MESSAGE_TYPE} and {@link
         * MessageTag#PAYLOAD_TYPE}.
         *
         * @param eventMessageTagBuilder a {@link MessageTagBuilder} used to add {@link EventMessage} specific
         *                               information to a {@link Tracer.SpanBuilder}
         * @return the current Builder instance, for fluent interfacing
         */
        public Builder eventMessageTags(MessageTagBuilder eventMessageTagBuilder) {
            assertNonNull(eventMessageTagBuilder, "The eventMessageTagBuilder builder may not be null");
            this.eventMessageTags = eventMessageTagBuilder;
            return this;
        }

        /**
         * Sets the given collection of {@code eventMessageTags} to drive the information to add to a {@link
         * Tracer.SpanBuilder} for {@link QueryMessage} instances. Defaults to the tags {@link MessageTag#MESSAGE_ID},
         * {@link MessageTag#MESSAGE_TYPE}, {@link MessageTag#PAYLOAD_TYPE} and {@link MessageTag#MESSAGE_NAME}.
         *
         * @param queryMessageTags a collection of {@link MessageTag}s driving the information to add to a {@link
         *                         Tracer.SpanBuilder}
         * @return the current Builder instance, for fluent interfacing
         */
        public Builder queryMessageTags(MessageTag... queryMessageTags) {
            return queryMessageTags(asList(queryMessageTags));
        }

        /**
         * Sets the given {@link List} of {@link MessageTag}s to drive the information to add {@link Tracer.SpanBuilder}
         * for {@link QueryMessage} instances. Defaults to the tags {@link MessageTag#MESSAGE_ID}, {@link
         * MessageTag#MESSAGE_TYPE}, {@link MessageTag#PAYLOAD_TYPE} and {@link MessageTag#MESSAGE_NAME}.
         *
         * @param queryMessageTags a {@link List} of {@link MessageTag}s driving the information to add to a {@link
         *                         Tracer.SpanBuilder}
         * @return the current Builder instance, for fluent interfacing
         */
        public Builder queryMessageTags(List<MessageTag> queryMessageTags) {
            return queryMessageTags(constructMessageTagBuilder(queryMessageTags));
        }

        /**
         * Sets the given {@link MessageTagBuilder} to be used when a {@link QueryMessage} is given to this service.
         * Defaults to the tags {@link MessageTag#MESSAGE_ID}, {@link MessageTag#MESSAGE_TYPE}, {@link
         * MessageTag#PAYLOAD_TYPE} and {@link MessageTag#MESSAGE_NAME}.
         *
         * @param queryMessageTagBuilder a {@link MessageTagBuilder} used to add {@link QueryMessage} specific
         *                               information to a {@link Tracer.SpanBuilder}
         * @return the current Builder instance, for fluent interfacing
         */
        public Builder queryMessageTags(MessageTagBuilder queryMessageTagBuilder) {
            assertNonNull(queryMessageTagBuilder, "The queryMessageTagBuilder builder may not be null");
            this.queryMessageTags = queryMessageTagBuilder;
            return this;
        }

        private MessageTagBuilder constructMessageTagBuilder(List<MessageTag> messageTags) {
            MessageTagBuilder messageTagBuilder = MessageTagBuilder.builder();
            for (MessageTag messageTag : messageTags) {
                switch (messageTag) {
                    case MESSAGE_ID:
                        messageTagBuilder = messageTagBuilder.with(MESSAGE_ID);
                        break;
                    case AGGREGATE_ID:
                        messageTagBuilder = messageTagBuilder.with(AGGREGATE_ID);
                        break;
                    case MESSAGE_TYPE:
                        messageTagBuilder = messageTagBuilder.with(MESSAGE_TYPE);
                        break;
                    case PAYLOAD_TYPE:
                        messageTagBuilder = messageTagBuilder.with(PAYLOAD_TYPE);
                        break;
                    case MESSAGE_NAME:
                        messageTagBuilder = messageTagBuilder.with(MESSAGE_NAME);
                        break;
                    case PAYLOAD:
                        messageTagBuilder = messageTagBuilder.with(PAYLOAD);
                        break;
                    default:
                        logger.warn("Unknown MessageTag [{}] used.", messageTag);
                }
            }
            return messageTagBuilder;
        }

        /**
         * Initializes a {@link MessageTagBuilderService} as specified through this Builder.
         *
         * @return a {@link MessageTagBuilderService} as specified through this Builder
         */
        public MessageTagBuilderService build() {
            return new MessageTagBuilderService(this);
        }
    }
}

