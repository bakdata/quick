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

import edu.umd.cs.findbugs.annotations.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import tech.allegro.schema.json2avro.converter.JsonAvroConverter;

/**
 * Resolver for avro objects.
 */
public class GenericAvroResolver implements TypeResolver<GenericRecord> {

    private final JsonAvroConverter converter;
    @Nullable
    private Schema schema = null;

    public GenericAvroResolver(final Schema schema) {
        this.schema = schema;
        this.converter = new JsonAvroConverter();
    }

    @Override
    public GenericRecord fromString(final String value) {
        Objects.requireNonNull(this.schema, "Configure the resolver before using it");
        return this.converter.convertToGenericDataRecord(value.getBytes(StandardCharsets.UTF_8), this.schema);
    }

}
