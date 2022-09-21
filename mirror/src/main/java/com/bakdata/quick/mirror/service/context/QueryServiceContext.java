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

package com.bakdata.quick.mirror.service.context;

import com.bakdata.quick.common.type.QuickTopicData;
import lombok.Builder;
import lombok.Value;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.state.HostInfo;

/**
 * Context for holding information about Kafka state.
 */
@Builder
@Value
public class QueryServiceContext {
    KafkaStreams streams;
    HostInfo hostInfo;
    String pointStoreName;
    RangeIndexProperties rangeIndexProperties;
    QuickTopicData<?, ?> quickTopicData;

    @SuppressWarnings("unchecked")
    public <K, V> QuickTopicData<K, V> getQuickTopicData() {
        return (QuickTopicData<K, V>) this.quickTopicData;
    }
}
