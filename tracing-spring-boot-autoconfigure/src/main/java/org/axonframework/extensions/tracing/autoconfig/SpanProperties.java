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

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Corrado Musumeci
 * @author Steven van Beelen
 * @since 4.4
 */
@ConfigurationProperties(prefix = "axon.extension.tracing.span")
public class SpanProperties {

    /**
     *
     */
    private List<String> commandTags = new ArrayList<>();
    /**
     *
     */
    private List<String> eventTags = new ArrayList<>();
    /**
     *
     */
    private List<String> queryTags = new ArrayList<>();

    public List<String> getCommandTags() {
        return commandTags;
    }

    public void setCommandTags(List<String> commandTags) {
        this.commandTags = commandTags;
    }

    public List<String> getEventTags() {
        return eventTags;
    }

    public void setEventTags(List<String> eventTags) {
        this.eventTags = eventTags;
    }

    public List<String> getQueryTags() {
        return queryTags;
    }

    public void setQueryTags(List<String> queryTags) {
        this.queryTags = queryTags;
    }
}
