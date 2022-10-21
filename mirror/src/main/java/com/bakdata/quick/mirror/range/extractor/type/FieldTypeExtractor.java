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

package com.bakdata.quick.mirror.range.extractor.type;

import com.bakdata.quick.common.exception.MirrorTopologyException;
import com.bakdata.quick.common.type.QuickTopicType;
import com.bakdata.quick.mirror.range.padder.EndRange;
import com.bakdata.quick.mirror.range.padder.IntPadder;
import com.bakdata.quick.mirror.range.padder.LongPadder;
import com.bakdata.quick.mirror.range.padder.ZeroPadder;
import io.confluent.kafka.schemaregistry.ParsedSchema;

/**
 * Extracts the {@link QuickTopicType} of given field name in the {@link ParsedSchema}.
 */
@FunctionalInterface
public interface FieldTypeExtractor {
    QuickTopicType extractType(final ParsedSchema parsedSchema, final String fieldName);

    /**
     * Returns the {@link ZeroPadder} for a given {@link QuickTopicType}.
     */
    @SuppressWarnings("unchecked")
    default <F> ZeroPadder<F> getZeroPadder(final QuickTopicType topicType) {
        if (topicType == QuickTopicType.INTEGER) {
            return (ZeroPadder<F>) new IntPadder(EndRange.EXCLUSIVE);
        } else if (topicType == QuickTopicType.LONG) {
            return (ZeroPadder<F>) new LongPadder(EndRange.EXCLUSIVE);
        }
        throw new MirrorTopologyException("Range field should be either of type integer or long");
    }
}
