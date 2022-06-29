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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Value;
import org.junit.jupiter.api.Test;

class KnownTypeResolverTest {

    private static final String JSON_RECORD = "{\"fieldId\":5,\"countPlays\":10}";

    @Test
    void shouldReadKnownTypeFromString() {
        final KnownTypeRecord exepected = KnownTypeRecord.builder().fieldId(5L).countPlays(10L).build();
        final TypeResolver<KnownTypeRecord> typeResolver =
            new KnownTypeResolver<>(KnownTypeRecord.class, new ObjectMapper());
        final KnownTypeRecord knownTypeRecord = typeResolver.fromString(JSON_RECORD);
        assertThat(knownTypeRecord).isEqualTo(exepected);
    }

    @Value
    @Builder
    static class KnownTypeRecord {
        long fieldId;
        long countPlays;
    }

}
