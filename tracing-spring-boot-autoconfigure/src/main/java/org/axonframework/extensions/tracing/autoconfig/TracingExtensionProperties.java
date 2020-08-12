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

/**
 * @author Corrado Musumeci
 * @since 4.4
 */

package org.axonframework.extensions.tracing.autoconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Defines the properties for the Tracing extension, when automatically configured in the Application Context.
 */
@ConfigurationProperties(prefix = "axon.extension.tracing")
public class TracingExtensionProperties {

    /**
     * Enables Tracing configuration for this application.
     */
    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
