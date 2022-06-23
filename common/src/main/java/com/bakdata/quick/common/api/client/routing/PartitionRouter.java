package com.bakdata.quick.common.api.client.routing;

import com.bakdata.quick.common.api.client.HttpClient;
import com.bakdata.quick.common.api.client.StreamsStateHost;
import com.bakdata.quick.common.api.model.TopicData;
import com.bakdata.quick.common.api.model.mirror.MirrorHost;
import com.bakdata.quick.common.config.MirrorConfig;
import com.bakdata.quick.common.exception.MirrorException;
import com.bakdata.quick.common.type.QuickTopicType;
import io.micronaut.http.HttpStatus;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class PartitionRouter<K> implements Router<K> {

    private final HttpClient client;
    private final StreamsStateHost streamsStateHost;
    private final QuickTopicType keyType;
    private final String topic;
    private final PartitionFinder partitionFinder;
    private final HashMap<Integer, MirrorHost> partitionToHost;

    public PartitionRouter(final HttpClient client, final StreamsStateHost streamsStateHost,
                           final QuickTopicType keyType, final String topic) {
        this.client = client;
        this.streamsStateHost = streamsStateHost;
        this.keyType = keyType;
        this.topic = topic;
        this.partitionFinder = MirrorConfig.getDefaultPartitionFinder();
        this.partitionToHost = new HashMap<>();
        init();
    }

    private void init() {
        final String url = this.streamsStateHost.getPartitionToHostUrl();
        final Request request = new Request.Builder().url(url).get().build();

        try (final Response response = this.client.newCall(request).execute()) {
            if (response.code() == HttpStatus.NOT_FOUND.getCode()) {
                throw new MirrorException("Resource not found", HttpStatus.NOT_FOUND);
            }

            final ResponseBody body = response.body();
            if (response.code() != HttpStatus.OK.getCode()) {
                //log.error("Got error response from mirror: {}", body);
                final String errorMessage = String.format(
                        "Error while fetching data. Requested resource responded with status code %d", response.code()
                );
                throw new MirrorException(errorMessage, HttpStatus.valueOf(response.code()));
            }

            // Code 200 and empty body indicates an error
            if (body == null) {
                throw new MirrorException("Resource responded with empty body", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            final Map<Integer, String> mirrorValue = this.client.objectMapper().readValue(body.byteStream(), Map.class);
            updatePartitionToHostInfo(mirrorValue);
        } catch (final IOException exception) {
            throw new MirrorException("Not able to parse content", HttpStatus.INTERNAL_SERVER_ERROR, exception);
        }
    }

    private void updatePartitionToHostInfo(Map<Integer, String> mirrorValue) {

    }

    @Override
    public MirrorHost getHost(K key) {
        final byte[] serializedKey = keyType.getSerde().serializer().serialize(topic, key);
        final int partition = partitionFinder.getForSerializedKey(serializedKey, this.partitionToHost.size());
        return this.partitionToHost.get(partition);
    }
}
