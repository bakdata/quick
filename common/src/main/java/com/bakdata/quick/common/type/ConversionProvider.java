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

import com.bakdata.quick.common.resolver.TypeResolver;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import java.util.Map;
import org.apache.kafka.common.serialization.Serde;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A provider for (de)-serialization operations in Quick.
 */
public interface ConversionProvider {

    /**
     * Returns a type resolver for this type.
     *
     * @param type         the underlying topic type
     * @param parsedSchema schema for type resolver that is required for complex types.
     * @param <K>          inner type of the type resolver
     * @return type resolver for conversion from strings
     */
    <K> TypeResolver<K> getTypeResolver(final QuickTopicType type, @Nullable final ParsedSchema parsedSchema);

    /**
     * Returns a configured serde for this type.
     *
     * @param type    the underlying topic type
     * @param configs serde configuration
     * @param isKey   true if serde is used for keys
     * @param <K>     type to be serialized from and deserialized to
     * @return configured serde
     */
    <K> Serde<K> getSerde(final QuickTopicType type, final Map<String, ?> configs, final boolean isKey);
}
