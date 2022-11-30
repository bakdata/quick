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

package com.bakdata.quick.mirror.context;

import com.bakdata.quick.common.type.QuickTopicData.QuickData;
import lombok.Value;
import org.apache.kafka.streams.kstream.KStream;

/**
 * Contains the key and value data along with the stream.
 *
 * @param <K> Type of the key
 * @param <V> Type of the value
 */
@Value
public class IndexInputStream<K, V> {
    QuickData<K> keyData;
    QuickData<V> valueData;
    KStream<K, V> stream;
}
