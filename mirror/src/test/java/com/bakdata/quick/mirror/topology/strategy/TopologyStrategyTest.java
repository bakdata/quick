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

package com.bakdata.quick.mirror.topology.strategy;

import com.bakdata.quick.common.TestTypeUtils;
import com.bakdata.quick.common.api.model.TopicWriteType;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.mirror.StoreType;
import com.bakdata.quick.mirror.base.QuickTopologyData;
import com.bakdata.quick.mirror.context.RangeIndexProperties;
import com.bakdata.quick.mirror.context.RetentionTimeProperties;
import com.bakdata.quick.mirror.context.MirrorContext;
import java.time.Duration;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class TopologyStrategyTest {
    private static final String POINT_STORE = "test-mirror-store";
    private static final String RANGE_STORE = "test-mirror-range-store";
    private static final String RETENTION_STORE = "test-mirror-range-store";

    @Test
    void shouldAlwaysApplyPointTopology() {
        final TopologyStrategy pointTopology = new PointTopology();
        final MirrorContext<?, ?> mirrorContext =
            createTopologyContext(new RangeIndexProperties(RANGE_STORE, null),
                new RetentionTimeProperties(RETENTION_STORE, null));

        Assertions.assertThat(pointTopology.isApplicable(mirrorContext)).isTrue();
    }

    @Test
    void shouldApplyRangeTopologyWhenRangeFieldIsSetAndRetentionIsNotSet() {
        final TopologyStrategy rangeTopology = new RangeTopology();
        final MirrorContext<?, ?> mirrorContext =
            createTopologyContext(new RangeIndexProperties(RANGE_STORE, "test-field"),
                new RetentionTimeProperties(RETENTION_STORE, null));

        Assertions.assertThat(rangeTopology.isApplicable(mirrorContext)).isTrue();
    }

    @Test
    void shouldApplyRetentionTopologyWhenRangeFieldIsSet() {
        final TopologyStrategy retentionTopology = new RetentionTopology();
        final MirrorContext<?, ?> mirrorContext =
            createTopologyContext(new RangeIndexProperties(RANGE_STORE, null),
                new RetentionTimeProperties(RETENTION_STORE, Duration.ZERO));

        Assertions.assertThat(retentionTopology.isApplicable(mirrorContext)).isTrue();
    }

    private static MirrorContext<Integer, Integer> createTopologyContext(
        final RangeIndexProperties rangeIndexProperties,
        final RetentionTimeProperties retentionTimeProperties) {

        final QuickTopicData<Integer, Integer> data =
            new QuickTopicData<>("input-topic", TopicWriteType.MUTABLE,
                TestTypeUtils.newIntegerData(),
                TestTypeUtils.newIntegerData());

        final QuickTopologyData<Integer, Integer> topologyInfo =
            QuickTopologyData.<Integer, Integer>builder()
                .inputTopics(List.of("input-topic"))
                .topicData(data)
                .build();

        return MirrorContext.<Integer, Integer>builder()
            .quickTopologyData(topologyInfo)
            .pointStoreName(POINT_STORE)
            .storeType(StoreType.INMEMORY)
            .rangeIndexProperties(rangeIndexProperties)
            .retentionTimeProperties(retentionTimeProperties)
            .isCleanup(false)
            .build();
    }
}