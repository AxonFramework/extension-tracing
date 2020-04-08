package org.axonframework.extensions.tracing;

import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MapInjectorTest {

    @Test
    void testInject() {
        MapInjector injector = new MapInjector();
        injector.put("key", "value");
        Map<String, String> metaData = injector.getMetaData();
        assertEquals("value", metaData.get("key"));
    }

    @Test
    void testMapInjectorIterator_throwsUnsupportedOperationException() {
        MapInjector injector = new MapInjector();
        Assertions.assertThrows(UnsupportedOperationException.class, () ->
                injector.iterator());
    }
}