package com.bakdata.quick.common.testutils;

import com.bakdata.quick.common.api.client.routing.PartitionFinder;
import com.bakdata.quick.common.api.model.TopicData;
import com.bakdata.quick.common.api.model.TopicWriteType;
import com.bakdata.quick.common.api.model.mirror.MirrorValue;
import com.bakdata.quick.common.type.QuickTopicType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

/**
 * A handful of utility methods for testing.
 */
public class TestUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestUtils() {}

    public static TopicData createTopicData(final String name) {
        return new TopicData(name, TopicWriteType.IMMUTABLE, QuickTopicType.LONG, QuickTopicType.STRING, null);
    }

    public static String generateBody(final Object mirrorValue) throws JsonProcessingException {
        return MAPPER.writeValueAsString(new MirrorValue<>(mirrorValue));
    }

    public static String generateBodyForRouter() throws JsonProcessingException {
        final Map<Integer, String> elements = Map.of(1, "1", 2, "2");
        return generateBodyForRouterWith(elements);
    }

    public static String generateBodyForRouterWith(final Map<Integer, String> elements) throws JsonProcessingException {
        return MAPPER.writeValueAsString(elements);
    }

    public static PartitionFinder getMockPartitionFinder() {
        return (serializedKey, numPartitions) -> 1;
    }
}
