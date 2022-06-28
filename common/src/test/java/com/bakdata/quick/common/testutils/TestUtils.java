package com.bakdata.quick.common.testutils;

import com.bakdata.quick.common.api.model.TopicData;
import com.bakdata.quick.common.api.model.TopicWriteType;
import com.bakdata.quick.common.api.model.mirror.MirrorValue;
import com.bakdata.quick.common.type.QuickTopicType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;

import java.util.Map;

public class TestUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestUtils() {}

    public static TopicData createTopicData(final String name) {
        return new TopicData(name, TopicWriteType.IMMUTABLE, QuickTopicType.LONG, QuickTopicType.STRING, null);
    }

    public static String generateBody(final Object mirrorValue) throws JsonProcessingException {
        return MAPPER.writeValueAsString(new MirrorValue<>(mirrorValue));
    }

    public static MockResponse generateMockResponseForRouter() throws JsonProcessingException {
        Map<Integer, String> body = Map.of(1, "1", 2, "2");
        return generateMockResponseForRouterWithMap(body);
    }

    public static MockResponse generateMockResponseForRouterWithMap(Map<Integer, String> body) throws JsonProcessingException {
        String json = MAPPER.writeValueAsString(body);
        return new MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .setBody(json);
    }
}
