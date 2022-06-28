package com.bakdata.quick.common.api.client.routing;

import com.bakdata.quick.common.api.client.HttpClient;
import com.bakdata.quick.common.api.client.StreamsStateHost;
import com.bakdata.quick.common.api.model.mirror.MirrorHost;
import com.bakdata.quick.common.config.MirrorConfig;
import com.bakdata.quick.common.exception.MirrorException;
import io.micronaut.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.kafka.common.serialization.Serde;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A router that leverages the fact that a mirror knows which partitions it stores
 * (it has a mapping between a partitions and a host),
 * and thus can use this information to introduce routing based on the specific partition mapping
 * @param <K> the type of key
 */
@Slf4j
public class PartitionRouter<K> implements Router<K> {

    private final HttpClient client;
    private final StreamsStateHost streamsStateHost;
    private final Serde<K> keySerde;
    private final String topic;
    private final PartitionFinder partitionFinder;
    private final Map<Integer, MirrorHost> partitionToHost = new HashMap<>();

    /**
     *
     * @param client http client
     * @param streamsStateHost info about the streams state host
     * @param keySerde serializer for the key
     * @param topic the name of the corresponding topic
     */
    public PartitionRouter(final HttpClient client, final StreamsStateHost streamsStateHost,
                           final Serde<K> keySerde, final String topic) {
        this.client = client;
        this.streamsStateHost = streamsStateHost;
        this.keySerde = keySerde;
        this.topic = topic;
        this.partitionFinder = MirrorConfig.getDefaultPartitionFinder();
        init();
    }

    /**
     * Fetches host-partition mapping from the StreamsStateController and updates the content
     * of the partitionToHost map with the retrieved information
     */
    private void init() {
        log.info("Initializing partition router...");
        final String url = this.streamsStateHost.getPartitionToHostUrl();
        final Request request = new Request.Builder().url(url).get().build();
        makeAndProcessRequestForPartitionHostMapping(request);
    }

    /**
     * Makes a request to the controller's endpoint responsible for providing partition-host info
     * and processes the corresponding response.
     * @param request request to the controller
     */
    private void makeAndProcessRequestForPartitionHostMapping(Request request) {
        try (final Response response = this.client.newCall(request).execute()) {
            processResponse(response);
        } catch (final IOException exception) {
            throw new MirrorException("Not able to parse content", HttpStatus.INTERNAL_SERVER_ERROR, exception);
        }
    }

    /**
     * Fetches information about partition-host mapping if response is valid
     * @param response response from endpoint
     * @throws IOException exception if response or body are invalid
     */
    private void processResponse(Response response) throws IOException {
        ResponseBody body = getBodyIfResponseValid(response);
        if (body == null) {
            throw new MirrorException("Resource responded with empty body", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        updatePartitionToHostMapping(body);
    }

    /**
     * Reads value from the response body and converts it to the Partition -> MirrorHost mapping
     * @param body response body
     * @throws IOException an exception if a value can be read for the byte stream
     */
    private void updatePartitionToHostMapping(final ResponseBody body) throws IOException {
        final Map<Integer, String> partitionHostMappingResponse = this.client.objectMapper().readValue(
                body.byteStream(), Map.class);
        log.info("Collected information about the partitions and hosts. There are {} partitions and {} distinct hosts",
                partitionHostMappingResponse.size(), partitionHostMappingResponse.values().stream().distinct());
        for (Map.Entry<Integer, String> entry : partitionHostMappingResponse.entrySet()) {
            MirrorHost host = new MirrorHost(entry.getValue(), MirrorConfig.directAccess());
            // partition -> host
            partitionToHost.put(entry.getKey(), host);
        }
    }

    private ResponseBody getBodyIfResponseValid(final Response response) {
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
        return body;

    }

    @Override
    public MirrorHost getHost(final K key) {
        final byte[] serializedKey = this.keySerde.serializer().serialize(topic, key);
        final int partition = partitionFinder.getForSerializedKey(serializedKey, this.partitionToHost.size());
        if (partitionToHost.isEmpty() || !partitionToHost.containsKey(partition)) {
            throw new IllegalStateException("Router has not been initialized properly.");
        }
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