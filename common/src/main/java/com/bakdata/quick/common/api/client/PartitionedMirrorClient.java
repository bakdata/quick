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

package com.bakdata.quick.common.api.client;

import com.bakdata.quick.common.api.client.routing.PartitionFinder;
import com.bakdata.quick.common.api.client.routing.PartitionRouter;
import com.bakdata.quick.common.api.client.routing.Router;
import com.bakdata.quick.common.api.model.mirror.MirrorHost;
import com.bakdata.quick.common.exception.MirrorException;
import com.bakdata.quick.common.resolver.TypeResolver;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.micronaut.http.HttpStatus;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.apache.kafka.common.serialization.Serde;

/**
 * MirrorClient that has access to information about partition-host mapping.
 * This information enables it to efficiently route requests in the case when there is more than one mirror replica.
 *
 * @param <K> key type
 * @param <V> value type
 */
@Slf4j
public class PartitionedMirrorClient<K, V> implements MirrorClient<K, V> {

    private static final TypeReference<Map<Integer, String>> MAP_TYPE_REFERENCE = new TypeReference<>() {
    };

    private final String topicName;
    private final StreamsStateHost streamsStateHost;
    private final HttpClient client;
    private final MirrorValueParser<V> parser;
    private final MirrorRequestManager requestManager;
    private final Router<K> router;
    private final List<MirrorHost> knownHosts;

    /**
     * Next to its default task of instantiation PartitionHost, it takes responsibility for
     * creating several business objects and initializing the PartitionRouter with a mapping
     * retrieved from StreamController.
     *
     * @param mirrorHost      mirror host to use
     * @param client          http client
     * @param keySerde        the serde for the key
     * @param valueResolver   the value's {@link TypeResolver}
     * @param partitionFinder strategy for finding partitions
     */
    public PartitionedMirrorClient(final MirrorHost mirrorHost, final HttpClient client,
                                   final Serde<K> keySerde, final TypeResolver<V> valueResolver,
                                   final PartitionFinder partitionFinder) {
        this.topicName = mirrorHost.getHost();
        this.streamsStateHost = StreamsStateHost.fromMirrorHost(mirrorHost);
        this.client = client;
        this.parser = new MirrorValueParser<>(valueResolver, client.objectMapper());
        this.requestManager = new MirrorRequestManagerWithFallback(client, mirrorHost);
        log.info("Initializing partition router for the mirror at: {} and the topic: {}.",
            this.streamsStateHost.getHost(), this.topicName);
        final Map<Integer, String> response = this.makeRequestForPartitionHostMapping();
        this.router = new PartitionRouter<>(keySerde, this.topicName, partitionFinder, response);
        this.knownHosts = this.router.getAllHosts();
    }

    @Override
    @Nullable
    public V fetchValue(final K key) {
        final MirrorHost currentKeyHost = this.router.findHost(key);
        log.debug("Host {} will answer the request for the key: {}.", currentKeyHost.getHost(), key);
        final ResponseWrapper response = this.requestManager
            .makeRequest(Objects.requireNonNull(currentKeyHost).forKey(key.toString()));
        if (response.isUpdateCacheHeaderSet()) {
            log.debug("The update header has been set. Updating router info.");
            this.updateRouterInfo();
        }
        return this.requestManager.processResponse(response, this.parser::deserialize);
    }

    @Override
    public List<V> fetchAll() {
        final List<V> valuesFromAllHosts = new ArrayList<>();
        log.debug("Fetching the values for all possible keys that are distributed across {} hosts.",
            this.knownHosts.size());
        for (final MirrorHost host : this.knownHosts) {
            log.debug("Fetching the value from the following host: {}", host.getHost());
            final ResponseWrapper response = this.requestManager.makeRequest(host.forAll());
            final List<V> valuesFromSingleHost =
                Objects.requireNonNullElse(this.requestManager.processResponse(response, this.parser::deserializeList),
                    Collections.emptyList());
            valuesFromAllHosts.addAll(valuesFromSingleHost);
            log.debug("Fetched {} values.", valuesFromSingleHost.size());
        }
        return valuesFromAllHosts;
    }

    @Override
    @Nullable
    public List<V> fetchValues(final List<K> keys) {
        log.debug("Fetching values for {} keys.", keys.size());
        return keys.stream().map(this::fetchValue).collect(Collectors.toList());
    }

    @Override
    public boolean exists(final K key) {
        return this.fetchValue(key) != null;
    }

    /**
     * Responsible for fetching the information about the partition - host mapping from
     * the mirror.
     *
     * @return a mapping between a partition (a number) and a corresponding host
     */
    private Map<Integer, String> makeRequestForPartitionHostMapping() {
        final String url = this.streamsStateHost.getPartitionToHostUrl();
        try (final ResponseBody responseBody = Objects.requireNonNull(
            this.requestManager.makeRequest(url)).getResponseBody()) {
            if (responseBody == null) {
                throw new MirrorException("Response body was null.", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            final Map<Integer, String> partitionHostMappingResponse = this.client.objectMapper()
                .readValue(responseBody.byteStream(), MAP_TYPE_REFERENCE);
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

    private void updateRouterInfo() {
        log.debug("Updating partition - router mapping for the mirror at: {} and the topic: {}.",
            this.streamsStateHost.getHost(), this.topicName);
        final Map<Integer, String> updatedPartitionHostInfo = this.makeRequestForPartitionHostMapping();
        this.router.updateRoutingInfo(updatedPartitionHostInfo);
    }
}
