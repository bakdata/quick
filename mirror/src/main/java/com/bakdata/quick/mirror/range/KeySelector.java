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
import com.bakdata.quick.mirror.context.MirrorContext;
import com.bakdata.quick.mirror.range.extractor.type.FieldTypeExtractor;
import com.bakdata.quick.mirror.range.extractor.value.FieldValueExtractor;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import lombok.Getter;
import org.apache.kafka.common.serialization.Serde;

public final class KeySelector<V> {
    @Getter
    private final QuickTopicType keyType;
    private final FieldValueExtractor<? super V> fieldValueExtractor;
    private final ConversionProvider conversionProvider;

    private KeySelector(final QuickTopicType keyType,
        final FieldValueExtractor<? super V> fieldValueExtractor,
        final ConversionProvider conversionProvider) {
        this.keyType = keyType;
        this.fieldValueExtractor = fieldValueExtractor;
        this.conversionProvider = conversionProvider;
    }

    public static <V> KeySelector<V> create(final MirrorContext<?, V> mirrorContext, final ParsedSchema parsedSchema,
        final String rangeKey) {
        final FieldTypeExtractor fieldTypeExtractor = mirrorContext.getFieldTypeExtractor();
        final FieldValueExtractor<V> fieldValueExtractor = mirrorContext.getFieldValueExtractor();
        final QuickTopicType keyType = fieldTypeExtractor.extract(parsedSchema, rangeKey);
        final ConversionProvider conversionProvider = mirrorContext.getConversionProvider();
        return new KeySelector<>(keyType, fieldValueExtractor, conversionProvider);
    }

    public <T> T getRangeKeyValue(final String fieldName, final V value) {
        if (value != null) {
            return this.fieldValueExtractor.extract(value, fieldName, this.keyType.getClassType());
        }
        throw new MirrorTopologyException("The value should not be null. Check you input topic data.");
    }

    public <T> QuickData<T> getRepartitionedKeyData() {
        final Serde<T> repartitionedKeySerde = this.conversionProvider.getSerde(this.keyType, true);
        final TypeResolver<T> typeResolver = this.conversionProvider.getTypeResolver(this.keyType, null);
        return new QuickData<>(this.keyType, repartitionedKeySerde, typeResolver, null);
    }
}
