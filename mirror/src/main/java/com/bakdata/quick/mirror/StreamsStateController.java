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


import com.bakdata.quick.mirror.service.QueryContextProvider;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import lombok.Value;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.state.StreamsMetadata;
import org.jooq.lambda.Seq;
import javax.inject.Inject;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API exposing current Kafka Streams state.
 */
@Controller("/mirror")
public class StreamsStateController {
    private final KafkaStreams streams;
    private final String storeName;

    @Inject
    public StreamsStateController(final QueryContextProvider contextProvider) {
        this.streams = contextProvider.get().getStreams();
        this.storeName = contextProvider.get().getStoreName();
    }

    /**
     * Returns a mapping from partition to host.
     */
    @Get("/partitions")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Integer, String> getApplicationHosts() {
        return Seq.seq(this.streams.allMetadataForStore(this.storeName))
            .flatMap(StreamsStateController::getAddressesForPartitions)
            .distinct(PartitionAddress::getPartition)
            .collect(Collectors.toMap(PartitionAddress::getPartition, PartitionAddress::getAddress));
    }

    private static Seq<PartitionAddress> getAddressesForPartitions(final StreamsMetadata metadata) {
        return Seq.seq(metadata.topicPartitions())
            .map(partition ->
                new PartitionAddress(partition.partition(), metadata.host(), metadata.port()));
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
