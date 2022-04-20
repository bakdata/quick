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

package com.bakdata.quick.common.resolver;

import com.bakdata.quick.common.type.QuickTopicType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import tech.allegro.schema.json2avro.converter.JsonAvroConverter;

/**
 * Resolver for avro objects.
 */
public class GenericAvroResolver implements TypeResolver<GenericRecord> {

    private final JsonAvroConverter converter;
    private final JavaType elementType;
    @Nullable
    private Schema schema = null;

    public GenericAvroResolver() {
        this.converter = new JsonAvroConverter();
        this.elementType = TypeFactory.defaultInstance().constructType(new TypeReference<Map<String, Object>>() {});
    }

    @Override
    public void configure(final Schema schema) {
        this.schema = schema;
    }

    @Override
    @SuppressWarnings("unchecked")
    public GenericRecord fromObject(final Object object) {
        if (object instanceof Map) {
            Objects.requireNonNull(this.schema, "Schema is not yet set.");
            final GenericRecordBuilder genericRecordBuilder = new GenericRecordBuilder(this.schema);
            for (final Entry<String, ?> entry : ((Map<String, ?>) object).entrySet()) {
                genericRecordBuilder.set(entry.getKey(), entry.getValue());
            }
            return genericRecordBuilder.build();
        } else if (object instanceof GenericRecord) {
            return (GenericRecord) object;
        }
        throw new RuntimeException(
            String.format("The requested type of %s is not supported.", object.getClass().getName()));
    }

    @Override
    public GenericRecord fromString(final String value) {
        Objects.requireNonNull(this.schema, "Configure the resolver before using it");
        return this.converter.convertToGenericDataRecord(value.getBytes(StandardCharsets.UTF_8), this.schema);
    }

    @Override
    public String toString(final GenericRecord value) {
        Objects.requireNonNull(this.schema, "Configure the resolver before using it");
        return new String(this.converter.convertToJson(value), StandardCharsets.UTF_8);
    }

    @Override
    public QuickTopicType getType() {
        return QuickTopicType.SCHEMA;
    }

    @Override
    public JavaType getElementType() {
        return this.elementType;
    }
}
