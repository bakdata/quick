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

package com.bakdata.quick.mirror;


import com.bakdata.quick.mirror.service.context.QueryContextProvider;
import com.bakdata.quick.mirror.service.context.QueryServiceContext;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Value;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsMetadata;

/**
 * REST API exposing current Kafka Streams state.
 */
@Controller("/streams")
public class StreamsStateController {
    private final KafkaStreams streams;
    private final String storeName;

    /**
     * Injectable constructor.
     */
    @Inject
    public StreamsStateController(final QueryContextProvider contextProvider) {
        final QueryServiceContext queryServiceContext = contextProvider.get();
        this.streams = queryServiceContext.getStreams();
        this.storeName = queryServiceContext.getPointStoreName();
    }

    /**
     * Returns a mapping from partition to host.
     */
    @Get("/partitions")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Integer, String> getApplicationHosts() {
        return this.streams.streamsMetadataForStore(this.storeName).stream()
            .flatMap(StreamsStateController::getAddressesForPartitions)
            .filter(distinctByKey(PartitionAddress::getPartition))
            .collect(Collectors.toMap(PartitionAddress::getPartition, PartitionAddress::getAddress));
    }

    private static Stream<PartitionAddress> getAddressesForPartitions(final StreamsMetadata metadata) {
        return metadata.topicPartitions().stream()
            .map(partition -> new PartitionAddress(partition.partition(), metadata.host(), metadata.port()));
    }

    private static <T> Predicate<T> distinctByKey(final Function<? super T, ?> keyExtractor) {
        final Set<Object> seen = ConcurrentHashMap.newKeySet();
        return element -> seen.add(keyExtractor.apply(element));
    }

    /**
     * POJO holding information about the partition and its host address.
     */
    @Value
    private static class PartitionAddress {
        int partition;
        String host;
        int port;

        private String getAddress() {
            return String.format("%s:%d", this.host, this.port);
        }
    }
}
