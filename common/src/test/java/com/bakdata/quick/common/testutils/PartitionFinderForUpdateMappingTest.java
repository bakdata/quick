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

/**
 * A custom PartitionFinder for testing the update mechanism of the PartitionRouter.
 */
public class PartitionFinderForUpdateMappingTest implements PartitionFinder {

    private boolean firstCall = true;

    /**
     * When it is called for the first time, the partition 2 is returned because there are
     * two items in the corresponding test (see: PartitionedMirrorClientTest). On consecutive calls,
     * it returns 3 in order to test whether the partitionToHost mapping has been updated properly.
     *
     * @param serializedKey the byte representation of a key for which the partition is sought
     * @param numPartitions the total number of partitions in a topic
     * @return partition number
     */
    @Override
    public int getForSerializedKey(final byte[] serializedKey, final int numPartitions) {
        return getNextPartition();
    }

    private int getNextPartition() {
        if (firstCall) {
            firstCall = false;
            return 2;
        }
        return 3;
    }
}
