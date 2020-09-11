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
import org.axonframework.messaging.Message;

import static org.axonframework.common.BuilderUtils.assertNonNull;
import static org.axonframework.extensions.tracing.SpanUtils.messageName;
import static org.axonframework.extensions.tracing.SpanUtils.resolveMessageType;

/**
 * @author Corrado Musumeci
 * @author Steven van Beelen
 * @since 4.4
 */
public class MessageTagBuilderService {

    private static final String TAG_AXON_ID = "axon.message.id";
    private static final String TAG_AXON_AGGREGATE_ID = "axon.message.aggregate-identifier";
    private static final String TAG_AXON_MESSAGE_TYPE = "axon.message.type";
    private static final String TAG_AXON_PAYLOAD_TYPE = "axon.message.payload-type";
    private static final String TAG_AXON_MESSAGE_NAME = "axon.message.message-name";

    private final MessageTagBuilder messageId;
    private final MessageTagBuilder aggregateId;
    private final MessageTagBuilder messageType;
    private final MessageTagBuilder payloadType;
    private final MessageTagBuilder messageName;

    /**
     * @return
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @param builder
     */
    public MessageTagBuilderService(Builder builder) {
        this.messageId = builder.messageId;
        this.aggregateId = builder.aggregateId;
        this.messageType = builder.messageType;
        this.payloadType = builder.payloadType;
        this.messageName = builder.messageName;
    }

    private static Tracer.SpanBuilder withMessageId(Tracer.SpanBuilder spanBuilder, Message<?> message) {
        return spanBuilder.withTag(TAG_AXON_ID, message.getIdentifier());
    }

    private static Tracer.SpanBuilder withAggregateId(Tracer.SpanBuilder spanBuilder, Message<?> message) {
        return spanBuilder.withTag(TAG_AXON_AGGREGATE_ID, ((DomainEventMessage<?>) message).getAggregateIdentifier());
    }

    private static Tracer.SpanBuilder withMessageType(Tracer.SpanBuilder spanBuilder, Message<?> message) {
        return spanBuilder.withTag(TAG_AXON_MESSAGE_TYPE, resolveMessageType(message));
    }

    private static Tracer.SpanBuilder withPayloadType(Tracer.SpanBuilder spanBuilder, Message<?> message) {
        return spanBuilder.withTag(TAG_AXON_PAYLOAD_TYPE, message.getPayloadType().getName());
    }

    private static Tracer.SpanBuilder withMessageName(Tracer.SpanBuilder spanBuilder, Message<?> message) {
        return spanBuilder.withTag(TAG_AXON_MESSAGE_NAME, messageName(message));
    }

    /**
     * @param spanBuilder
     * @param message
     * @return
     */
    public Tracer.SpanBuilder withCommandMessageTags(Tracer.SpanBuilder spanBuilder, CommandMessage<?> message) {
        return messageId.with(messageType)
                        .with(payloadType)
                        .with(messageName)
                        .build(spanBuilder, message);
    }

    /**
     * @param spanBuilder
     * @param message
     * @return
     */
    public Tracer.SpanBuilder withEvenMessageTags(Tracer.SpanBuilder spanBuilder, Message<?> message) {
        // TODO: 11-09-20 For some reason messageName is currently disabled for event messages
        return messageId.with(aggregateId)
                        .with(messageType)
                        .with(payloadType)
                        .build(spanBuilder, message);
    }

    /**
     * @param spanBuilder
     * @param message
     * @return
     */
    public Tracer.SpanBuilder withQueryMessageTags(Tracer.SpanBuilder spanBuilder, Message<?> message) {
        return messageId.with(messageType)
                        .with(payloadType)
                        .with(messageName)
                        .build(spanBuilder, message);
    }

    /**
     *
     */
    public static class Builder {

        private MessageTagBuilder messageId = MessageTagBuilderService::withMessageId;
        private MessageTagBuilder aggregateId = MessageTagBuilderService::withAggregateId;
        private MessageTagBuilder messageType = MessageTagBuilderService::withMessageType;
        private MessageTagBuilder payloadType = MessageTagBuilderService::withPayloadType;
        private MessageTagBuilder messageName = MessageTagBuilderService::withMessageName;

        /**
         * @param messageId
         * @return
         */
        public Builder withMessageId(MessageTagBuilder messageId) {
            assertNonNull(messageId, "exception message TODO");
            this.messageId = messageId;
            return this;
        }

        /**
         * @return
         */
        public Builder disableMessageId() {
            this.messageId = MessageTagBuilder.noOp();
            return this;
        }

        /**
         * @param aggregateId
         * @return
         */
        public Builder withAggregateId(MessageTagBuilder aggregateId) {
            assertNonNull(aggregateId, "exception message TODO");
            this.aggregateId = aggregateId;
            return this;
        }

        /**
         * @return
         */
        public Builder disableAggregateId() {
            this.aggregateId = MessageTagBuilder.noOp();
            return this;
        }

        /**
         * @param messageType
         * @return
         */
        public Builder withMessageType(MessageTagBuilder messageType) {
            assertNonNull(messageType, "exception message TODO");
            this.messageType = messageType;
            return this;
        }

        /**
         * @return
         */
        public Builder disableMessageType() {
            this.messageType = MessageTagBuilder.noOp();
            return this;
        }

        /**
         * @param payloadType
         * @return
         */
        public Builder withPayloadType(MessageTagBuilder payloadType) {
            assertNonNull(payloadType, "exception message TODO");
            this.payloadType = payloadType;
            return this;
        }

        /**
         * @return
         */
        public Builder disablePayloadType() {
            this.payloadType = MessageTagBuilder.noOp();
            return this;
        }

        /**
         * @param messageName
         * @return
         */
        public Builder withMessageName(MessageTagBuilder messageName) {
            assertNonNull(messageName, "exception message TODO");
            this.messageName = messageName;
            return this;
        }

        /**
         * @return
         */
        public Builder disableMessageName() {
            this.messageName = MessageTagBuilder.noOp();
            return this;
        }

        /**
         * @return
         */
        public MessageTagBuilderService build() {
            return new MessageTagBuilderService(this);
        }
    }
}

