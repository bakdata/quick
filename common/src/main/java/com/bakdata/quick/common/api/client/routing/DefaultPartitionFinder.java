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

package com.bakdata.quick.common.api.client.routing;

import org.apache.kafka.common.utils.Utils;

/**
 * PartitionFinder with the basic (default) logic for retrieving partitions.
 */
public class DefaultPartitionFinder implements PartitionFinder {

    @Override
    public int getForSerializedKey(final byte[] serializedKey, final int numPartitions) {
        return Utils.toPositive(Utils.murmur2(serializedKey)) % numPartitions;
    }
}
