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

package com.bakdata.quick.mirror.range;

import com.bakdata.quick.common.exception.MirrorTopologyException;
import com.bakdata.quick.common.resolver.TypeResolver;
import com.bakdata.quick.common.type.ConversionProvider;
import com.bakdata.quick.common.type.QuickTopicData.QuickData;
import com.bakdata.quick.common.type.QuickTopicType;
import com.bakdata.quick.mirror.range.extractor.type.FieldTypeExtractor;
import com.bakdata.quick.mirror.range.extractor.value.FieldValueExtractor;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import org.apache.kafka.common.serialization.Serde;

/**
 * Extracts the type and the value from a field in a complex type.
 */
public final class KeySelector<R, V> {
    private final QuickTopicType keyType;
    private final Class<? extends R> classType;
    private final FieldValueExtractor<? super V> fieldValueExtractor;
    private final ConversionProvider conversionProvider;

    private KeySelector(final QuickTopicType keyType,
        final Class<? extends R> classType,
        final FieldValueExtractor<? super V> fieldValueExtractor,
        final ConversionProvider conversionProvider) {
        this.keyType = keyType;
        this.classType = classType;
        this.fieldValueExtractor = fieldValueExtractor;
        this.conversionProvider = conversionProvider;
    }

    /**
     * Static factory to create a key selector.
     */
    public static <R, V> KeySelector<R, V> create(final FieldTypeExtractor fieldTypeExtractor,
        final FieldValueExtractor<V> fieldValueExtractor,
        final ConversionProvider conversionProvider,
        final ParsedSchema parsedSchema,
        final String rangeKey) {
        final QuickTopicType keyType = fieldTypeExtractor.extract(parsedSchema, rangeKey);
        final Class<R> classType = conversionProvider.getClassType(keyType);
        return new KeySelector<>(keyType, classType, fieldValueExtractor, conversionProvider);
    }

    /**
     * extracts the value of a given field.
     */
    public R getRangeKeyValue(final String fieldName, final V value) {
        if (value != null) {
            return this.fieldValueExtractor.extract(value, fieldName, this.classType);
        }
        throw new MirrorTopologyException("The value should not be null. Check you input topic data.");
    }

    /**
     * Gets the repartitioned key data.
     */
    public <T> QuickData<T> getRepartitionedKeyData() {
        final Serde<T> repartitionedKeySerde = this.conversionProvider.getSerde(this.keyType, true);
        final TypeResolver<T> typeResolver = this.conversionProvider.getTypeResolver(this.keyType, null);
        return new QuickData<>(this.keyType, repartitionedKeySerde, typeResolver, null);
    }
}
