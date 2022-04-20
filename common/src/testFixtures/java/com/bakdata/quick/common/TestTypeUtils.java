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

package com.bakdata.quick.common;

import com.bakdata.quick.common.resolver.DoubleResolver;
import com.bakdata.quick.common.resolver.GenericAvroResolver;
import com.bakdata.quick.common.resolver.IntegerResolver;
import com.bakdata.quick.common.resolver.LongResolver;
import com.bakdata.quick.common.resolver.StringResolver;
import com.bakdata.quick.common.type.QuickTopicData.QuickData;
import com.bakdata.quick.common.type.QuickTopicType;
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.serialization.Serdes;

/**
 * Utils for quick types.
 */
public final class TestTypeUtils {
    private TestTypeUtils() {
    }

    public static QuickData<GenericRecord> newAvroData() {
        return new QuickData<>(QuickTopicType.SCHEMA, new GenericAvroSerde(), new GenericAvroResolver());
    }

    public static QuickData<String> newStringData() {
        return new QuickData<>(QuickTopicType.STRING, Serdes.String(), new StringResolver());
    }

    public static QuickData<Long> newLongData() {
        return new QuickData<>(QuickTopicType.LONG, Serdes.Long(), new LongResolver());
    }

    public static QuickData<Double> newDoubleData() {
        return new QuickData<>(QuickTopicType.DOUBLE, Serdes.Double(), new DoubleResolver());
    }

    public static QuickData<Integer> newIntegerData() {
        return new QuickData<>(QuickTopicType.INTEGER, Serdes.Integer(), new IntegerResolver());
    }

}
