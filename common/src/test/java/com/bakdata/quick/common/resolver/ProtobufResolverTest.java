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

import com.bakdata.quick.testutil.ProtoTestRecord;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import org.junit.jupiter.api.Test;

class ProtobufResolverTest {

    private static final String EXPECTED_FIELD_ID = "test";
    private static final int EXPECTED_COUNT_PLAYS = 10;
    private static final String JSON_RECORD = "{\"id\":\"test\",\"value\":10}";

    @Test
    void shouldReadProtoFromString() {
        final ProtoTestRecord record = ProtoTestRecord.newBuilder().setId("test").setValue(10).build();
        final Descriptors.Descriptor descriptor = record.getDescriptorForType();
        final ProtobufResolver resolver = new ProtobufResolver(descriptor);

        final Message genericRecord = resolver.fromString(JSON_RECORD);
        final Descriptors.FieldDescriptor idField =
            descriptor.findFieldByNumber(ProtoTestRecord.ID_FIELD_NUMBER);
        final Descriptors.FieldDescriptor valueField =
            descriptor.findFieldByNumber(ProtoTestRecord.VALUE_FIELD_NUMBER);
        assertThat(genericRecord.getField(idField)).isEqualTo(EXPECTED_FIELD_ID);
        assertThat(genericRecord.getField(valueField)).isEqualTo(EXPECTED_COUNT_PLAYS);
    }

}
