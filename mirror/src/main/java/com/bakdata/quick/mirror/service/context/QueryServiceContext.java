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

import com.bakdata.quick.common.exception.MirrorException;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.mirror.service.MirrorIndexType;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.micronaut.http.HttpStatus;
import java.util.Map;
import lombok.Value;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.state.HostInfo;

/**
 * Context for holding information about Kafka state.
 */
@Value
public class QueryServiceContext {
    KafkaStreams streams;
    HostInfo hostInfo;
    Map<MirrorIndexType, IndexProperties> rangePropertiesMap;
    QuickTopicData<?, ?> topicData;

    @SuppressWarnings("unchecked")
    public <K, V> QuickTopicData<K, V> getTopicData() {
        return (QuickTopicData<K, V>) this.topicData;
    }

    /**
     * Gets the state store name for a given {@link MirrorIndexType}.
     *
     * @param mirrorIndexType Type of the index
     * @return Name of the state store
     */
    public String getStoreNameOfType(final MirrorIndexType mirrorIndexType) {
        if (this.rangePropertiesMap.containsKey(mirrorIndexType)) {
            return this.rangePropertiesMap.get(mirrorIndexType).getStoreName();
        } else {
            throw new MirrorException("Could not retrieve index properties", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
