package com.bakdata.quick.common.api.client.routing;

import com.bakdata.quick.common.api.model.mirror.MirrorHost;

import java.util.List;

/**
 * Strategy for finding a host that keeps information about a specific key, i.e. a partition of a topic
 * @param <K>
 */
public interface Router<K> {

    /**
     *
     * @param key key
     * @return a replica-host where partitions for a given key are stored
     */
    MirrorHost getHost(K key);

    /**
     * Retrieves all mirror hosts in a specific app
     * @return a list of hosts
     */
    List<MirrorHost> getAllHosts();
}
