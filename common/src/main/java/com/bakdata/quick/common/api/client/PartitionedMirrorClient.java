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
import com.bakdata.quick.common.config.MirrorConfig;
import com.bakdata.quick.common.exception.InternalErrorException;
import com.bakdata.quick.common.resolver.TypeResolver;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.apache.kafka.common.serialization.Serde;

/**
 * MirrorClient that has access to information about partition-host mapping. This enables it to efficiently
 * route requests in case when there is more than one mirror replica.
 *
 * @param <K> key type
 * @param <V> value type
 */
@Slf4j
public class PartitionedMirrorClient<K, V> implements MirrorClient<K, V> {

    private final StreamsStateHost streamsStateHost;
    private final HttpClient client;
    private final Serde<K> keySerde;
    private final String topicName;
    private final PartitionFinder partitionFinder;
    private final MirrorValueParser<V> parser;
    private final MirrorRequestManager requestManager;

    private Router<K> router;
    private List<MirrorHost> knownHosts;

    /**
     * Constructor for the client.
     *
     * @param topicName     name of the topic the mirror is deployed
     * @param client        http client
     * @param mirrorConfig  configuration of the mirror host
     * @param keySerde      serializer for the key
     * @param valueResolver the value's {@link TypeResolver}
     */
    public PartitionedMirrorClient(final String topicName, final HttpClient client, final MirrorConfig mirrorConfig,
                                   final Serde<K> keySerde, final TypeResolver<V> valueResolver) {
        this(topicName, new MirrorHost(topicName, mirrorConfig), client, keySerde, valueResolver);
    }

    /**
     * Constructor that can be used when the mirror client is based on an IP or other non-standard host.
     *
     * @param topicName     the name of the topic
     * @param mirrorHost    host to use
     * @param client        http client
     * @param keySerde      the serde for the key
     * @param valueResolver the value's {@link TypeResolver}
     */
    public PartitionedMirrorClient(final String topicName, final MirrorHost mirrorHost, final HttpClient client,
                                   final Serde<K> keySerde, final TypeResolver<V> valueResolver) {

        this.streamsStateHost = StreamsStateHost.fromMirrorHost(mirrorHost);
        this.client = client;
        this.keySerde = keySerde;
        this.topicName = topicName;
        this.partitionFinder = StreamsStateHost.getDefaultPartitionFinder();
        this.parser = new MirrorValueParser<>(valueResolver, client.objectMapper());
        this.requestManager = new DefaultMirrorRequestManager(client);
        initRouter();
    }

    /**
     * Constructor that can be used when the mirror client is based on an IP or other non-standard host.
     *
     * @param topicName       the name of the topic
     * @param mirrorHost      host to use
     * @param client          http client
     * @param keySerde        the serde for the key
     * @param valueResolver   the value's {@link TypeResolver}
     * @param partitionFinder strategy for finding partitions
     */
    public PartitionedMirrorClient(final String topicName, final MirrorHost mirrorHost, final HttpClient client,
                                   final Serde<K> keySerde, final TypeResolver<V> valueResolver,
                                   final PartitionFinder partitionFinder) {
        this.streamsStateHost = StreamsStateHost.fromMirrorHost(mirrorHost);
        this.client = client;
        this.keySerde = keySerde;
        this.topicName = topicName;
        this.partitionFinder = partitionFinder;
        this.parser = new MirrorValueParser<>(valueResolver, client.objectMapper());
        this.requestManager = new DefaultMirrorRequestManager(client);
        initRouter();
    }

    @Override
    @Nullable
    public V fetchValue(final K key) {
        final MirrorHost currentKeyHost = router.getHost(key);
        return this.requestManager.sendRequest(Objects.requireNonNull(currentKeyHost).forKey(key.toString()),
            this.parser::deserialize);
    }

    @Override
    public List<V> fetchAll() {
        final List<V> valuesFromAllHosts = new ArrayList<>();
        for (final MirrorHost host : this.knownHosts) {
            final List<V> valuesFromSingleHost =
                Objects.requireNonNullElse(this.requestManager.sendRequest(host.forAll(), this.parser::deserializeList),
                    Collections.emptyList());
            valuesFromAllHosts.addAll(valuesFromSingleHost);
        }
        return valuesFromAllHosts;
    }

    @Override
    @Nullable
    public List<V> fetchValues(final List<K> keys) {
        return keys.stream().map(this::fetchValue).collect(Collectors.toList());
    }

    @Override
    public boolean exists(final K key) {
        return this.fetchValue(key) != null;
    }

    private void initRouter() {
        log.info("Initializing partition router...");
        final Map<Integer, String> response = makeRequestForPartitionHostMapping();
        this.router = new PartitionRouter<>(this.keySerde, this.topicName, this.partitionFinder, response);
        this.knownHosts = this.router.getAllHosts();
    }

    private Map<Integer, String> makeRequestForPartitionHostMapping() {
        final String url = this.streamsStateHost.getPartitionToHostUrl();
        Map<Integer, String> partitionHostMappingResponse = new HashMap<>();
        try (final ResponseBody responseBody = requestManager.makeRequest(url)) {
            if (responseBody != null) {
                final TypeReference<Map<Integer, String>> typeRef = new TypeReference<>() {};
                partitionHostMappingResponse = this.client.objectMapper()
                    .readValue(Objects.requireNonNull(responseBody).byteStream(), typeRef);
                log.info("Collected information about the partitions and hosts."
                        + " There are {} partitions and {} distinct hosts", partitionHostMappingResponse.size(),
                    (int) partitionHostMappingResponse.values().stream().distinct().count());
            } else {
                log.info("Response body was null. Returning an empty mapping.");
            }
            return partitionHostMappingResponse;
        } catch (final IOException e) {
            throw new InternalErrorException("There was a problem handling the response: " + e.getMessage());
        }
    }
}
