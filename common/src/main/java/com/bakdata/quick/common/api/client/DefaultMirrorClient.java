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
import com.bakdata.quick.common.api.model.TopicData;
import com.bakdata.quick.common.api.model.mirror.MirrorHost;
import com.bakdata.quick.common.api.model.mirror.MirrorValue;
import com.bakdata.quick.common.config.MirrorConfig;
import com.bakdata.quick.common.exception.MirrorException;
import com.bakdata.quick.common.resolver.TypeResolver;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.micronaut.http.HttpStatus;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Default HTTP client for working with Quick mirrors.
 *
 * @param <K> key type
 * @param <V> value type
 */
@Slf4j
public class DefaultMirrorClient<K, V> implements MirrorClient<K, V> {

    private final ObjectMapper mapper = new ObjectMapper();

    private final MirrorHost mirrorHost;
    private final StreamsStateHost streamsStateHost;
    private final HttpClient client;
    private final MirrorValueParser<V> parser;

    /**
     * Constructor for the client.
     *
     * @param topicName    name of the topic for which the mirror is deployed
     * @param client       http client
     * @param mirrorConfig configuration of the mirror host
     * @param typeResolver the value's {@link TypeResolver}
     */
    public DefaultMirrorClient(final String topicName, final HttpClient client, final MirrorConfig mirrorConfig,
                               final TypeResolver<V> typeResolver) {
        this(new MirrorHost(topicName, mirrorConfig), client, typeResolver);
    }

    /**
     * Constructor that can be used when the mirror client is based on an IP or other non-standard host.
     *
     * @param mirrorHost   host to use
     * @param client       http client
     * @param typeResolver the value's {@link TypeResolver}
     */
    public DefaultMirrorClient(final MirrorHost mirrorHost, final HttpClient client,
                               final TypeResolver<V> typeResolver) {
        this.host = mirrorHost;
        this.client = client;
        this.parser = new MirrorValueParser<>(typeResolver, client.objectMapper());
    }

    @Override
    @Nullable
    public V fetchValue(final K key) {
        return this.sendRequest(this.host.forKey(key.toString()), this.parser::deserialize);
    }

    @Override
    public List<V> fetchAll() {
        return Objects.requireNonNullElse(this.sendRequest(this.host.forAll(), this.parser::deserializeList),
            Collections.emptyList());
    }

    @Override
    public boolean exists(final K key) {
        return this.fetchValue(key) != null;
    }

    @Override
    @Nullable
    public List<V> fetchValues(final List<K> keys) {
        final List<String> collect = keys.stream().map(Object::toString).collect(Collectors.toList());
        return this.sendRequest(this.host.forKeys(collect), this.parser::deserializeList);
    }

    @Nullable
    private <T> T sendRequest(final String url, final ParserFunction<T> parser) {
        final Request request = new Request.Builder().url(url).get().build();

        try (final Response response = this.client.newCall(request).execute()) {
            if (response.code() == HttpStatus.NOT_FOUND.getCode()) {
                return null;
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

            final MirrorValue<T> mirrorValue = parser.parse(body.byteStream());
            return mirrorValue.getValue();
        } catch (final IOException exception) {
            throw new MirrorException("Not able to parse content", HttpStatus.INTERNAL_SERVER_ERROR, exception);
        }
    }
}
