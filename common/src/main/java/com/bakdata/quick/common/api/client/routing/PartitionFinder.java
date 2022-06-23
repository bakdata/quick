package com.bakdata.quick.common.api.client.routing;

public interface PartitionFinder {

    int getForSerializedKey(final byte[] serializedKey, final int numPartitions);


}
