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

package com.bakdata.quick.mirror;

import static com.bakdata.quick.common.TestTypeUtils.newIntegerData;
import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.fluent_kafka_streams_tests.junit5.TestTopologyExtension;
import com.bakdata.quick.common.api.model.TopicWriteType;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.mirror.base.QuickTopologyData;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.KeyValueStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class MirrorTopologyTest {
    private static final List<String> INPUT_TOPICS = List.of("input");
    private static final String STORE_NAME = "test-store";
    private static final String RETENTION_STORE_NAME = "retention-test-store";

    @RegisterExtension
    final TestTopologyExtension<Integer, Integer> driver =
        new TestTopologyExtension<>(MirrorTopologyTest::createTopology, testProps())
            .withDefaultSerde(Serdes.Integer(), Serdes.Integer());

    static Topology createTopology(final Properties properties) {
        final String topic = INPUT_TOPICS.get(0);
        final QuickTopicData<Integer, Integer> data =
            new QuickTopicData<>(topic, TopicWriteType.MUTABLE, newIntegerData(), newIntegerData());
        data.getKeyData().getSerde().configure(Maps.fromProperties(properties), true);
        data.getValueData().getSerde().configure(Maps.fromProperties(properties), false);

        final QuickTopologyData<Integer, Integer> topologyInfo =
            QuickTopologyData.<Integer, Integer>builder()
                .inputTopics(INPUT_TOPICS)
                .topicData(data)
                .build();

        final MirrorTopology<Integer, Integer> mirrorTopology = MirrorTopology.<Integer, Integer>builder()
            .topologyData(topologyInfo)
            .storeName(STORE_NAME)
            .retentionStoreName(RETENTION_STORE_NAME)
            .storeType(StoreType.INMEMORY)
            .build();

        final StreamsBuilder builder = new StreamsBuilder();
        return mirrorTopology.createTopology(builder);
    }

    private static Map<String, String> testProps() {
        return Map.of("bootstrap.servers", "test:123", "application.id", "mirror-test");
    }

    @Test
    void shouldAddValue() {
        this.driver.input().add(5, 2);
        final KeyValueStore<Integer, Integer> store = this.driver.getTestDriver().getKeyValueStore(STORE_NAME);
        assertThat(store.get(5)).isEqualTo(2);
    }

    @Test
    void shouldUpdateValue() {
        this.driver.input().add(5, 2);
        final KeyValueStore<Integer, Integer> store = this.driver.getTestDriver().getKeyValueStore(STORE_NAME);
        assertThat(store.get(5)).isEqualTo(2);

        this.driver.input().add(5, 8);
        assertThat(store.get(5)).isEqualTo(8);
    }

    @Test
    void shouldDeleteKeyWithNullValue() {
        this.driver.input().add(5, 2);
        final KeyValueStore<Integer, Integer> store = this.driver.getTestDriver().getKeyValueStore(STORE_NAME);
        assertThat(store.get(5)).isEqualTo(2);

        this.driver.input().add(5, null);
        assertThat(store.get(5)).isNull();
    }
}
