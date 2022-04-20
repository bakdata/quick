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

package com.bakdata.quick.common.type.inferer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;


/**
 * Consumer for inferring types of a kafka topic.
 */
public class InferConsumer {
    public static final int POLL_TIMEOUT_IN_S = 5;
    public static final int MAX_POLL_RECORDS = 500;
    private final KafkaConsumer<byte[], byte[]> consumer;
    private final String topic;

    public InferConsumer(final String bootstrapServer, final String topic, final String appId) {
        this.consumer = createConsumer(bootstrapServer, appId);
        this.topic = topic;
    }

    /**
     * Fetches a list of record.
     */
    public ConsumerRecords<byte[], byte[]> fetchRecords() {
        this.consumer.subscribe(List.of(this.topic));
        this.consumer.seekToBeginning(this.consumer.assignment());
        final ConsumerRecords<byte[], byte[]> consumerRecords =
            this.consumer.poll(Duration.ofSeconds(POLL_TIMEOUT_IN_S));
        this.consumer.close();
        return consumerRecords;
    }

    private static KafkaConsumer<byte[], byte[]> createConsumer(final String bootstrapServer, final String appId) {
        final Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, appId + "-infer-consumer");
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, Integer.toString(MAX_POLL_RECORDS));
        return new KafkaConsumer<>(properties);
    }
}
