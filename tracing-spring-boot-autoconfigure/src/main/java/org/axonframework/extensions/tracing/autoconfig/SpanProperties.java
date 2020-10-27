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

package org.axonframework.extensions.tracing.autoconfig;

import org.axonframework.extensions.tracing.MessageTag;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Class containing properties defining the {@link io.opentracing.Span} build by this extension.
 *
 * @author Corrado Musumeci
 * @author Steven van Beelen
 * @since 4.4
 */
@ConfigurationProperties(prefix = "axon.extension.tracing.span")
public class SpanProperties {

    /**
     * A list of {@link MessageTag} instances defining the tags to be added to a {@link io.opentracing.Span} when
     * dealing with a {@link org.axonframework.commandhandling.CommandMessage}.
     * <p>
     * Defaults to the tags {@link MessageTag#MESSAGE_ID}, {@link MessageTag#MESSAGE_TYPE}, {@link
     * MessageTag#PAYLOAD_TYPE} and {@link MessageTag#MESSAGE_NAME}.
     */
    private List<MessageTag> commandTags =
            asList(MessageTag.MESSAGE_ID, MessageTag.MESSAGE_TYPE, MessageTag.PAYLOAD_TYPE, MessageTag.MESSAGE_NAME);
    /**
     * A list of {@link MessageTag} instances defining the tags to be added to a {@link io.opentracing.Span} when
     * dealing with a {@link org.axonframework.eventhandling.EventMessage}.
     * <p>
     * Defaults to the tags {@link MessageTag#MESSAGE_ID}, {@link MessageTag#AGGREGATE_ID}, {@link
     * MessageTag#MESSAGE_TYPE} and {@link MessageTag#PAYLOAD_TYPE}.
     */
    private List<MessageTag> eventTags =
            asList(MessageTag.MESSAGE_ID, MessageTag.AGGREGATE_ID, MessageTag.MESSAGE_TYPE, MessageTag.PAYLOAD_TYPE);
    /**
     * A list of {@link MessageTag} instances defining the tags to be added to a {@link io.opentracing.Span} when
     * dealing with a {@link org.axonframework.queryhandling.QueryMessage}.
     * <p>
     * Defaults to the tags {@link MessageTag#MESSAGE_ID}, {@link MessageTag#MESSAGE_TYPE}, {@link
     * MessageTag#PAYLOAD_TYPE} and {@link MessageTag#MESSAGE_NAME}.
     */
    private List<MessageTag> queryTags =
            asList(MessageTag.MESSAGE_ID, MessageTag.MESSAGE_TYPE, MessageTag.PAYLOAD_TYPE, MessageTag.MESSAGE_NAME);

    public List<MessageTag> getCommandTags() {
        return commandTags;
    }

    public void setCommandTags(List<MessageTag> commandTags) {
        this.commandTags = commandTags;
    }

    public List<MessageTag> getEventTags() {
        return eventTags;
    }

    public void setEventTags(List<MessageTag> eventTags) {
        this.eventTags = eventTags;
    }

    public List<MessageTag> getQueryTags() {
        return queryTags;
    }

    public void setQueryTags(List<MessageTag> queryTags) {
        this.queryTags = queryTags;
    }
}
