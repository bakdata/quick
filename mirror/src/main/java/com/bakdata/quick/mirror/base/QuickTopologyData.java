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

package com.bakdata.quick.mirror.base;

import com.bakdata.quick.common.type.QuickTopicData;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Data for Kafka Streams topologies.
 *
 * @param <K> key type
 * @param <V> value type
 */
@Getter
public class QuickTopologyData<K, V> {
    final List<String> inputTopics;
    @Nullable final String outputTopic;
    @Nullable final String errorTopic;
    final QuickTopicData<K, V> topicData;

    /**
     * Constructor used by builder.
     */
    @Builder
    public QuickTopologyData(final List<String> inputTopics, @Nullable final String outputTopic,
        @Nullable final String errorTopic, final QuickTopicData<K, V> topicData) {
        this.inputTopics = inputTopics;
        this.outputTopic = outputTopic;
        this.errorTopic = errorTopic;
        this.topicData = topicData;
    }
}
