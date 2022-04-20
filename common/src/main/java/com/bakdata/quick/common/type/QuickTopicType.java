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

import com.bakdata.quick.common.resolver.DoubleResolver;
import com.bakdata.quick.common.resolver.GenericAvroResolver;
import com.bakdata.quick.common.resolver.IntegerResolver;
import com.bakdata.quick.common.resolver.LongResolver;
import com.bakdata.quick.common.resolver.StringResolver;
import com.bakdata.quick.common.resolver.TypeResolver;
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde;
import java.util.function.Supplier;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes.DoubleSerde;
import org.apache.kafka.common.serialization.Serdes.IntegerSerde;
import org.apache.kafka.common.serialization.Serdes.LongSerde;
import org.apache.kafka.common.serialization.Serdes.StringSerde;

/**
 * Represents possible types for Quick topics.
 */
@SuppressWarnings({"unchecked", "ImmutableEnumChecker"})
public enum QuickTopicType {
    SCHEMA(GenericAvroSerde::new, GenericAvroResolver::new),
    DOUBLE(DoubleSerde::new, DoubleResolver::new),
    INTEGER(IntegerSerde::new, IntegerResolver::new),
    LONG(LongSerde::new, LongResolver::new),
    STRING(StringSerde::new, StringResolver::new);

    private final Supplier<Serde<?>> serdeSupplier;
    private final Supplier<TypeResolver<?>> resolverSupplier;

    QuickTopicType(final Supplier<Serde<?>> serdeSupplier, final Supplier<TypeResolver<?>> resolverSupplier) {
        this.serdeSupplier = serdeSupplier;
        this.resolverSupplier = resolverSupplier;
    }

    public <T> Serde<T> getSerde() {
        return (Serde<T>) this.serdeSupplier.get();
    }

    public <T> TypeResolver<T> getTypeResolver() {
        return (TypeResolver<T>) this.resolverSupplier.get();
    }

}
