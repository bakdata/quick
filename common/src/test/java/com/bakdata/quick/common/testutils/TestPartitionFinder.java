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

package com.bakdata.quick.common.testutils;

import com.bakdata.quick.common.api.client.routing.PartitionFinder;
import com.bakdata.quick.common.exception.InternalErrorException;
import java.util.Queue;

/**
 * A custom PartitionFinder for testing the update mechanism of the PartitionRouter.
 */
public class TestPartitionFinder implements PartitionFinder {

    private final Queue<Integer> partitions;

    /**
     * PartitionFinder that uses a queue as a provider for partitions.
     *
     */
    public TestPartitionFinder(final Queue<Integer> partitionsQueue) {
        this.partitions = partitionsQueue;
    }

    /**
     * Returns an element from the queue as a next partition.
     *
     * @param serializedKey the byte representation of a key for which the partition is sought
     * @param numPartitions the total number of partitions in a topic
     * @return partition number
     */
    @Override
    public int getForSerializedKey(final byte[] serializedKey, final int numPartitions) {
        if (this.partitions.isEmpty()) {
            throw new InternalErrorException("There are no partitions to be served in the queue.");
        }
        return this.partitions.poll();
    }

    /**
     * Adds an integer that represents the next partition to the queue.
     *
     * @param nextPartition next partition
     */
    public void enqueue(final int nextPartition) {
        this.partitions.add(nextPartition);
    }
}
