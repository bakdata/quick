/*
 *    Copyright 2022 bakdata GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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

    private TestUtils() {
    }

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
