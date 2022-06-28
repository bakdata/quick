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