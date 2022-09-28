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

import com.bakdata.quick.common.api.client.HttpClient;
import com.bakdata.quick.common.api.client.mirror.MirrorRequestManager;
import com.bakdata.quick.common.api.client.mirror.ResponseWrapper;
import com.bakdata.quick.common.api.client.mirror.StreamsStateHost;
import com.bakdata.quick.common.api.model.mirror.MirrorHost;
import com.bakdata.quick.common.config.MirrorConfig;
import com.bakdata.quick.common.exception.MirrorException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.micronaut.http.HttpStatus;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.apache.kafka.common.serialization.Serde;

/**
 * A router that leverages the fact that a mirror knows which partitions a specific replica stores (it has a mapping
 * between partitions and a host replica), and thus can use this information to introduce routing based on the specific
 * partition mapping.
 *
 * @param <K> the type of key
 */
@Slf4j
public class PartitionRouter<K> implements Router<K> {
    private final HttpClient client;
    private static final TypeReference<Map<Integer, String>> MAP_TYPE_REFERENCE = new TypeReference<>() {};
    private final StreamsStateHost streamsStateHost;
    private final String topic;
    private final Serde<? super K> keySerde;
    private final PartitionFinder partitionFinder;
    private Map<Integer, MirrorHost> partitionToMirrorHost;
    private List<MirrorHost> distinctMirrorHosts;
    private final MirrorRequestManager requestManager;

    /**
     * A constructor with the default partitioner that is retrieved from a static method.
     *
     * @param keySerde serializer for the key
     * @param partitionFinder strategy for finding partitions
     */
    public PartitionRouter(
        final HttpClient client,
        final MirrorHost mirrorHost,
        final Serde<? super K> keySerde,
        final PartitionFinder partitionFinder,
        final MirrorRequestManager requestManager) {
        this.client = client;
        this.streamsStateHost = StreamsStateHost.fromMirrorHost(mirrorHost);
        this.topic = mirrorHost.getHost();
        this.keySerde = keySerde;
        this.partitionFinder = partitionFinder;
        this.requestManager = requestManager;
        this.partitionToMirrorHost = convertHostStringToMirrorHost(this.makeRequestForPartitionHostMapping());
        this.distinctMirrorHosts = this.findDistinctHosts();
    }


    @Override
    public MirrorHost findHost(final K key) {
        final byte[] serializedKey = this.keySerde.serializer().serialize(this.topic, key);
        final int partition =
            this.partitionFinder.getForSerializedKey(serializedKey, this.partitionToMirrorHost.size());
        if (!this.partitionToMirrorHost.containsKey(partition)) {
            throw new MirrorException(String.format("No MirrorHost found for partition: %d", partition),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        log.debug("Calculated partition is: {}, getting it from {}", partition, this.partitionToMirrorHost);
        return this.partitionToMirrorHost.get(partition);
    }

    @Override
    public List<MirrorHost> getAllHosts() {
        if (this.partitionToMirrorHost.isEmpty()) {
            throw new MirrorException("Partition to MirrorHost mapping is empty.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return this.distinctMirrorHosts;
    }

    @Override
    public void updateRoutingInfo() {
        final Map<Integer, String> updatedPartitionHostInfo = this.makeRequestForPartitionHostMapping();
        log.debug("Updating route info with: {}", updatedPartitionHostInfo);
        this.partitionToMirrorHost = convertHostStringToMirrorHost(updatedPartitionHostInfo);
        this.distinctMirrorHosts = this.findDistinctHosts();
    }

    private List<MirrorHost> findDistinctHosts() {
        final Set<String> distinctHosts = new HashSet<>(this.partitionToMirrorHost.size());
        return this.partitionToMirrorHost.values()
            .stream()
            .filter(mirrorHost -> distinctHosts.add(mirrorHost.getHost()))
            .collect(Collectors.toList());
    }

    private static Map<Integer, MirrorHost> convertHostStringToMirrorHost(final Map<Integer, String> partitionToHost) {
        return partitionToHost.entrySet().stream().collect(
            Collectors.toMap(Map.Entry::getKey, e -> new MirrorHost(e.getValue(), MirrorConfig.directAccess())));
    }

    /**
     * Responsible for fetching the information about the partition - host mapping from the mirror.
     *
     * @return a mapping between a partition (a number) and a corresponding host
     */
    private Map<Integer, String> makeRequestForPartitionHostMapping() {
        final String url = this.streamsStateHost.getPartitionToHostUrl();
        final ResponseWrapper responseWrapper = this.requestManager.makeRequest(url);
        try (final ResponseBody responseBody = Objects.requireNonNull(responseWrapper).getResponseBody()) {
            if (responseBody == null) {
                throw new MirrorException("Response body was null.", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            final Map<Integer, String> partitionHostMappingResponse = this.client.objectMapper()
                .readValue(responseBody.byteStream(), MAP_TYPE_REFERENCE);
            log.debug("Partition to individual Mirror hosts are: {}", partitionHostMappingResponse);
            if (log.isInfoEnabled()) {
                log.info("Collected information about the partitions and hosts."
                        + " There are {} partitions and {} distinct hosts", partitionHostMappingResponse.size(),
                    (int) partitionHostMappingResponse.values().stream().distinct().count());
            }
            return partitionHostMappingResponse;
        } catch (final IOException exception) {
            throw new MirrorException("There was a problem handling the response: ",
                HttpStatus.INTERNAL_SERVER_ERROR, exception);
        }
    }
}
