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

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.quick.avro.ChartRecord;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;

class GenericAvroResolverTest {

    private static final long EXPECTED_FIELD_ID = 5L;
    private static final long EXPECTED_COUNT_PLAYS = 10L;
    private static final String JSON_RECORD = "{\"fieldId\":5,\"countPlays\":10}";
    private final ChartRecord chartRecord = ChartRecord.newBuilder().setFieldId(5L).setCountPlays(10L).build();

    @Test
    void shouldReadAvroFromString() {
        final GenericAvroResolver resolver = new GenericAvroResolver(ChartRecord.getClassSchema());
        final GenericRecord genericRecord = resolver.fromString(JSON_RECORD);
        assertThat(genericRecord.getSchema()).isEqualTo(this.chartRecord.getSchema());
        assertThat(genericRecord.get("fieldId")).isEqualTo(EXPECTED_FIELD_ID);
        assertThat(genericRecord.get("countPlays")).isEqualTo(EXPECTED_COUNT_PLAYS);
    }
}
