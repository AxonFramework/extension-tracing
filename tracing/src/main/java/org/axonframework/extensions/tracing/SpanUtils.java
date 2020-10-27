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

/**
 * Utility class providing useful methods for attaching information to Spans.
 *
 * @author Allard Buijze
 * @author Lucas Campos
 * @author Steven van Beelen
 * @since 4.0
 */
public class SpanUtils {

    private static final String TAG_AXON_ID = "axon.message.id";
    private static final String TAG_AXON_AGGREGATE_ID = "axon.message.aggregate-identifier";
    private static final String TAG_AXON_MESSAGE_TYPE = "axon.message.type";
    private static final String TAG_AXON_PAYLOAD_TYPE = "axon.message.payload-type";
    private static final String TAG_AXON_MESSAGE_NAME = "axon.message.message-name";

    /**
     * Registers message-specific tags to the given {@code spanBuilder} based on the given {@code message}.
     *
     * @param spanBuilder the Span Builder to register the tags with
     * @param message     the message to retrieve details from
     * @return a builder with tags attached
     * @deprecated in favor of {@link MessageTagBuilderService#withMessageTags(Tracer.SpanBuilder, Message)}
     */
    @Deprecated
    public static Tracer.SpanBuilder withMessageTags(Tracer.SpanBuilder spanBuilder, Message<?> message) {
        Tracer.SpanBuilder sb = spanBuilder.withTag(TAG_AXON_ID, message.getIdentifier())
                                           .withTag(TAG_AXON_MESSAGE_TYPE, resolveMessageType(message))
                                           .withTag(TAG_AXON_PAYLOAD_TYPE, message.getPayloadType().getName());
        if (message instanceof CommandMessage || message instanceof QueryMessage) {
            return sb.withTag(TAG_AXON_MESSAGE_NAME, messageName(message));
        } else if (message instanceof DomainEventMessage) {
            return sb.withTag(TAG_AXON_AGGREGATE_ID, ((DomainEventMessage<?>) message).getAggregateIdentifier());
        }
        return sb;
    }

    /**
     * Registers query-specific tags to the given {@code spanBuilder} based on the given {@code query}.
     *
     * @param spanBuilder  the Span Builder to register the tags with
     * @param queryMessage the query to retrieve details from
     * @param queryName    the name provided by the {@link org.axonframework.queryhandling.QueryGateway} caller
     * @return a builder with tags attached
     * @deprecated in favor of {@link MessageTagBuilderService#withQueryMessageTags(Tracer.SpanBuilder, QueryMessage)}
     */
    @Deprecated
    public static Tracer.SpanBuilder withQueryMessageTags(Tracer.SpanBuilder spanBuilder,
                                                          Message<?> queryMessage,
                                                          String queryName) {
        return spanBuilder.withTag(TAG_AXON_ID, queryMessage.getIdentifier())
                          .withTag(TAG_AXON_MESSAGE_TYPE, QueryMessage.class.getSimpleName())
                          .withTag(TAG_AXON_PAYLOAD_TYPE, queryMessage.getPayloadType().getName())
                          .withTag(TAG_AXON_MESSAGE_NAME, queryName);
    }

    /**
     * Resolves the type of message as a String, for use in description of operations.
     * <p>
     * This method will check if the message is a
     * <ul>
     * <li>{@link QueryMessage}, returning {@code "QueryMessage"},</li>
     * <li>{@link CommandMessage}, returning {@code "CommandMessage"},</li>
     * <li>{@link EventMessage}, returning {@code "EventMessage"},</li>
     * <li>otherwise returns {@code "Message"}</li>
     * </ul>
     *
     * @param message the message to resolve the type of
     * @return a String describing the type of message
     */
    public static String resolveMessageType(Message<?> message) {
        Class<?> clazz = Message.class;
        if (message instanceof QueryMessage) {
            clazz = QueryMessage.class;
        } else if (message instanceof CommandMessage) {
            clazz = CommandMessage.class;
        } else if (message instanceof EventMessage) {
            clazz = EventMessage.class;
        }
        return clazz.getSimpleName();
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
     * @param message the message to resolve the type of
     * @return a String describing the type of message
     */
    public static String messageName(Message<?> message) {
        if (message instanceof CommandMessage) {
            //noinspection rawtypes
            return messageName(message.getPayloadType(), ((CommandMessage) message).getCommandName());
        } else if (message instanceof QueryMessage) {
            //noinspection rawtypes
            return messageName(message.getPayloadType(), ((QueryMessage) message).getQueryName());
        }
        return message.getPayloadType().getSimpleName();
    }

    static String messageName(Class<?> payloadType, String name) {
        if (!payloadType.getName().equals(name)) {
            return name;
        }
        return payloadType.getSimpleName();
    }
}
