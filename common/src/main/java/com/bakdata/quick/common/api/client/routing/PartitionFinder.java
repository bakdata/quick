package com.bakdata.quick.common.api.client.routing;

/**
 * Represents logic for finding the partition for a specific key.
 */
public interface PartitionFinder {

    /**
     *
     * @param serializedKey the byte representation of a key for which the partition is sought
     * @param numPartitions the total number of partitions in a topic
     * @return an int that represents a partition
     */
    int getForSerializedKey(final byte[] serializedKey, final int numPartitions);
}
