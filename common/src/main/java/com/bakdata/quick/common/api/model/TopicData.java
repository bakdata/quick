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

package com.bakdata.quick.common.api.model;

import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.type.QuickTopicData.QuickData;
import com.bakdata.quick.common.type.QuickTopicType;
import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.Value;

/**
 * Data for a topic.
 */
@Value
public class TopicData {
    String name;
    TopicWriteType writeType;
    QuickTopicType keyType;
    QuickTopicType valueType;
    @Nullable String schema;

    /**
     * Converts this to QuickTopicData.
     */
    public <K, V> QuickTopicData<K, V> toQuickTopicData() {
        final QuickData<K> keyData = new QuickData<>(
            this.keyType,
            this.keyType.getSerde(),
            this.keyType.getTypeResolver()
        );
        final QuickData<V> valueData = new QuickData<>(
            this.valueType,
            this.valueType.getSerde(),
            this.valueType.getTypeResolver()
        );
        return new QuickTopicData<>(this.name, this.writeType, keyData, valueData);
    }
}
