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

import org.axonframework.messaging.MetaData;
import org.junit.jupiter.api.*;

import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validate behaviour of the {@link MapExtractor}.
 *
 * @author Christophe Bouhier
 */
class MapExtractorTest {

    @Test
    void testExtractor() {
        MetaData metaData = MetaData.emptyInstance();
        metaData = metaData.and("key", "value");

        MapExtractor extractor = new MapExtractor(metaData);
        Iterator<Map.Entry<String, String>> iterator = extractor.iterator();

        assertTrue(iterator.hasNext());
        assertEquals("value", iterator.next().getValue());
    }

    @Test
    void testMapExtractorPut_throwsUnsupportedOperationException() {
        MetaData metaData = MetaData.emptyInstance();
        metaData = metaData.and("key", "value");

        MapExtractor extractor = new MapExtractor(metaData);

        Assertions.assertThrows(UnsupportedOperationException.class, () -> extractor.put("key1", "value1"));
    }
}