package com.bakdata.quick.common.api.client.routing;

import com.bakdata.quick.common.api.client.HttpClient;
import com.bakdata.quick.common.api.client.StreamsStateHost;
import com.bakdata.quick.common.api.model.TopicData;
import com.bakdata.quick.common.api.model.mirror.MirrorHost;
import com.bakdata.quick.common.config.MirrorConfig;
import com.bakdata.quick.common.exception.MirrorException;
import com.bakdata.quick.common.type.QuickTopicType;
import io.micronaut.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A router that makes use of the fact that a mirror knows which partitions it stores
 * (it has a mapping between a partitions and a host),
 * and thus can use this information to introduce routing based on the specific partition mapping
 * @param <K> the type of key
 */
@Slf4j
public class PartitionRouter<K> implements Router<K> {

    private final HttpClient client;
    private final StreamsStateHost streamsStateHost;
    private final QuickTopicType keyType;
    private final String topic;
    private final PartitionFinder partitionFinder;
    private final Map<Integer, MirrorHost> partitionToHost = new HashMap<>();

    /**
     *
     * @param client http client
     * @param streamsStateHost info about the streams state host
     * @param keyType the type of key
     * @param topic the name of the corresponding topic
     */
    public PartitionRouter(final HttpClient client, final StreamsStateHost streamsStateHost,
                           final QuickTopicType keyType, final String topic) {
        this.client = client;
        this.streamsStateHost = streamsStateHost;
        this.keyType = keyType;
        this.topic = topic;
        this.partitionFinder = MirrorConfig.getDefaultPartitionFinder();
        init();
    }

    /**
     * Fetches host-partition mapping from the StreasStateController and updates the content
     * of the partitionToHost map with the retrieved information
     */
    private void init() {
        final String url = this.streamsStateHost.getPartitionToHostUrl();
        final Request request = new Request.Builder().url(url).get().build();

        try (final Response response = this.client.newCall(request).execute()) {
            if (response.code() == HttpStatus.NOT_FOUND.getCode()) {
                throw new MirrorException("Resource not found", HttpStatus.NOT_FOUND);
            }

            final ResponseBody body = response.body();
            if (response.code() != HttpStatus.OK.getCode()) {
                log.error("Got error response from mirror: {}", body);
                final String errorMessage = String.format(
                        "Error while fetching data. Requested resource responded with status code %d", response.code()
                );
                throw new MirrorException(errorMessage, HttpStatus.valueOf(response.code()));
            }

            // Code 200 and empty body indicates an error
            if (body == null) {
                throw new MirrorException("Resource responded with empty body", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            final Map<Integer, String> partitionHostMappingResponse = this.client.objectMapper().readValue(body.byteStream(), Map.class);
            updatePartitionToHostInfo(partitionHostMappingResponse);

        } catch (final IOException exception) {
            throw new MirrorException("Not able to parse content", HttpStatus.INTERNAL_SERVER_ERROR, exception);
        }
    }

    private void updatePartitionToHostInfo(Map<Integer, String> partitionHostMappingFromMirror) {
        for (Map.Entry<Integer, String> entry : partitionHostMappingFromMirror.entrySet()) {
            MirrorHost host = new MirrorHost(entry.getValue(), MirrorConfig.directAccess());
            // partition -> host
            partitionToHost.put(entry.getKey(), host);
        }

    }

    @Override
    public MirrorHost getHost(K key) {
        if (partitionToHost.isEmpty()) {
            throw new IllegalStateException("Router has not been initialized properly.");
        }
        // is info about the topic schema retrieved automatically?
        final byte[] serializedKey = keyType.getSerde().serializer().serialize(topic, key);
        final int partition = partitionFinder.getForSerializedKey(serializedKey, this.partitionToHost.size());
        return this.partitionToHost.get(partition);
    }

    @Override
    public List<MirrorHost> getAllHosts() {
        if (partitionToHost.isEmpty()) {
            throw new IllegalStateException("Router has not been initialized properly.");
        }
        return (List<MirrorHost>) partitionToHost.values();
    }
}
