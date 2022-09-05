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

package com.bakdata.quick.common.type;

import com.bakdata.quick.common.api.model.TopicWriteType;
import com.bakdata.quick.common.resolver.TypeResolver;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import lombok.Value;
import org.apache.kafka.common.serialization.Serde;
import org.jetbrains.annotations.Nullable;

/**
 * POJO for topic data.
 *
 * @param <K> key type
 * @param <V> value type
 */
@Value
public class QuickTopicData<K, V> {
    String name;
    TopicWriteType writeType;
    QuickData<K> keyData;
    QuickData<V> valueData;

    /**
     * Topic Data for key or value.
     *
     * @param <T> type
     */
    @Value
    public static class QuickData<T> {
        QuickTopicType type;
        Serde<T> serde;
        TypeResolver<T> resolver;
        @Nullable
        ParsedSchema parsedSchema;
    }
}
