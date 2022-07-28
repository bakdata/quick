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

/**
 * Represents logic for finding the partition for a specific key.
 */
public interface PartitionFinder {

    /**
     * Calculates the partition for a specific serialized key.
     *
     * @param serializedKey the byte representation of a key for which the partition is sought
     * @param numPartitions the total number of partitions in a topic
     * @return an int that represents a partition
     */
    int getForSerializedKey(final byte[] serializedKey, final int numPartitions);
}
