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

package com.bakdata.quick.ingest.controller;

import static com.bakdata.quick.common.TestTypeUtils.newAvroData;
import static com.bakdata.quick.common.TestTypeUtils.newDoubleData;
import static com.bakdata.quick.common.TestTypeUtils.newIntegerData;
import static com.bakdata.quick.common.TestTypeUtils.newLongData;
import static com.bakdata.quick.common.TestTypeUtils.newStringData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bakdata.quick.avro.ChartRecord;
import com.bakdata.quick.common.api.model.ErrorMessage;
import com.bakdata.quick.common.api.model.KeyValuePair;
import com.bakdata.quick.common.api.model.TopicWriteType;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.type.QuickTopicData.QuickData;
import com.bakdata.quick.common.type.QuickTopicType;
import com.bakdata.quick.common.type.TopicTypeService;
import com.bakdata.quick.common.type.registry.QuickTopicTypeService;
import com.bakdata.quick.ingest.service.IngestFilter;
import com.bakdata.quick.ingest.service.IngestFilter.IngestLists;
import com.bakdata.quick.ingest.service.IngestService;
import com.bakdata.quick.ingest.service.KafkaIngestService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.Value;
import org.apache.avro.generic.GenericData.Record;
import org.apache.avro.generic.GenericRecord;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@MicronautTest
@Property(name = "micronaut.security.enabled", value = "false")
class IngestControllerTest {

    private static final String TOPIC = "topic";
    @Inject
    private IngestService ingestService;

    @Inject
    private TopicTypeService typeService;

    @Inject
    private IngestFilter ingestFilter;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    @Client("/")
    private RxHttpClient client;

    private static Stream<Arguments> valueParsingProvider() {
        final QuickData<String> stringInfo = newStringData();
        final ChartRecord record = inputRecord();
        return Stream.of(
            new TestValueArgument<>(new QuickTopicData<>(TOPIC, TopicWriteType.MUTABLE, stringInfo, stringInfo),
                "value", "value"),
            new TestValueArgument<>(
                new QuickTopicData<>(TOPIC, TopicWriteType.MUTABLE, stringInfo, newIntegerData()), 5, 5),
            new TestValueArgument<>(
                new QuickTopicData<>(TOPIC, TopicWriteType.MUTABLE, stringInfo, newDoubleData()), 5.0, 5.0),
            new TestValueArgument<>(new QuickTopicData<>(TOPIC, TopicWriteType.MUTABLE, stringInfo, getAvroInfo()),
                record, outputRecord()),
            new TestValueArgument<>(new QuickTopicData<>(TOPIC, TopicWriteType.MUTABLE, stringInfo, newLongData()),
                5L, 5L))
            .map(arg -> Arguments.of(arg.getData().getValueData().getType(), arg));
    }

    private static Stream<Arguments> keyParsingProvider() {
        return keyArguments().map(arg -> Arguments.of(arg.getData().getKeyData().getType(), arg));
    }

    private static Stream<Arguments> deleteFromBodyProvider() {
        return keyArguments()
            .filter(argument -> argument.getData().getKeyData().getType() == QuickTopicType.SCHEMA)
            .map(arg -> Arguments.of(arg.getData().getKeyData().getType(), arg));
    }

    private static Stream<Arguments> deleteFromPathProvider() {
        return keyArguments()
            .filter(argument -> argument.getData().getKeyData().getType() != QuickTopicType.SCHEMA)
            .map(arg -> Arguments.of(arg.getData().getKeyData().getType(), arg));
    }

    private static Stream<Arguments> keyParsingInvalidProvider() {
        return invalidKeyArguments().map(arg -> Arguments.of(arg.getData().getKeyData().getType(), arg));
    }

    private static Stream<TestKeyArgument<?, ?, ?>> keyArguments() {
        final QuickData<String> stringInfo = newStringData();
        final ChartRecord record = inputRecord();
        return Stream.of(
            new TestKeyArgument<>(new QuickTopicData<>(TOPIC, TopicWriteType.MUTABLE, stringInfo, stringInfo),
                "value", "value"),
            new TestKeyArgument<>(new QuickTopicData<>(TOPIC, TopicWriteType.MUTABLE, newIntegerData(), stringInfo),
                5, 5),
            new TestKeyArgument<>(new QuickTopicData<>(TOPIC, TopicWriteType.MUTABLE, newDoubleData(), stringInfo),
                5.0, 5.0),
            new TestKeyArgument<>(new QuickTopicData<>(TOPIC, TopicWriteType.MUTABLE, getAvroInfo(), stringInfo),
                record, outputRecord()),
            new TestKeyArgument<>(new QuickTopicData<>(TOPIC, TopicWriteType.MUTABLE, newLongData(), stringInfo),
                5L, 5L)
        );
    }

    private static Stream<TestInvalidKeyArgument<?, ?, ?>> invalidKeyArguments() {
        final QuickData<String> stringInfo = newStringData();
        return Stream.of(
            new TestInvalidKeyArgument<>(
                new QuickTopicData<>(TOPIC, TopicWriteType.MUTABLE, stringInfo, stringInfo),
                5L),
            new TestInvalidKeyArgument<>(
                new QuickTopicData<>(TOPIC, TopicWriteType.MUTABLE, newIntegerData(), stringInfo),
                "value"),
            new TestInvalidKeyArgument<>(
                new QuickTopicData<>(TOPIC, TopicWriteType.MUTABLE, stringInfo, stringInfo),
                5.0),
            new TestInvalidKeyArgument<>(
                new QuickTopicData<>(TOPIC, TopicWriteType.MUTABLE, newLongData(), stringInfo),
                "value")
        );
    }

    private static ChartRecord inputRecord() {
        return ChartRecord.newBuilder().setFieldId(5L).setCountPlays(5L).build();
    }

    private static Record outputRecord() {
        final Record record = new Record(ChartRecord.getClassSchema());
        record.put(0, 5L);
        record.put(1, 5L);
        return record;
    }

    private static QuickData<GenericRecord> getAvroInfo() {
        final QuickData<GenericRecord> avroInfo = newAvroData();
        avroInfo.getResolver().configure(new AvroSchema(ChartRecord.getClassSchema()));
        return avroInfo;
    }

    private static <T> HttpRequest<?> creatIngestRequest(final T body) {
        return
            HttpRequest.create(HttpMethod.POST, "/topic/")
                .body(body)
                .contentType(MediaType.APPLICATION_JSON_TYPE);
    }

    @ParameterizedTest(name = "[{index}] testValueParsing for type {0}")
    @MethodSource("valueParsingProvider")
    <K, V, E> void testValueParsing(final QuickTopicType type, final TestValueArgument<K, V, E> argument) {
        final KeyValuePair<String, V> pair = new KeyValuePair<>("key", argument.getValue());

        when(this.ingestService.sendData(eq(TOPIC), any())).thenReturn(Completable.complete());

        doReturn(Single.just(argument.getData())).when(this.typeService).getTopicData(TOPIC);

        doReturn(Single.just(new IngestLists<>(List.of(pair), List.of())))
            .when(this.ingestFilter).prepareIngest(any(), any());

        final HttpResponse<?> response = this.client.toBlocking().exchange(HttpRequest.POST("/topic/", pair));

        assertThat((CharSequence) response.getStatus()).isEqualTo(HttpStatus.OK);

        verify(this.ingestService, times(1)).sendData(TOPIC, List.of(pair));
    }

    @ParameterizedTest(name = "[{index}] testKeyParsing for type {0}")
    @MethodSource("keyParsingProvider")
    <K, V, E> void testKeyParsing(final QuickTopicType type, final TestKeyArgument<K, V, E> argument) {
        final KeyValuePair<K, String> pair = new KeyValuePair<>(argument.getKey(), "value");

        when(this.ingestService.sendData(eq(TOPIC), any())).thenReturn(Completable.complete());

        doReturn(Single.just(argument.getData())).when(this.typeService).getTopicData(TOPIC);

        doReturn(Single.just(new IngestLists<>(List.of(pair), List.of())))
            .when(this.ingestFilter).prepareIngest(any(), any());

        final HttpResponse<?> response = this.client.toBlocking().exchange(HttpRequest.POST("/topic/", pair));
        assertThat((CharSequence) response.getStatus()).isEqualTo(HttpStatus.OK);

        verify(this.ingestService).sendData(TOPIC, List.of(pair));
    }

    @ParameterizedTest(name = "[{index}] shouldDeleteKeyInPath for type {0}")
    @MethodSource("deleteFromPathProvider")
    <K, V, E> void shouldDeleteKeyInPath(final QuickTopicType type, final TestKeyArgument<K, V, E> argument) {
        when(this.ingestService.deleteData(eq(TOPIC), any())).thenReturn(Completable.complete());
        doReturn(Single.just(argument.getData())).when(this.typeService).getTopicData(TOPIC);
        final String keyString = argument.getData().getKeyData().getResolver().toString(argument.getKey());
        final HttpResponse<?> response = this.client.toBlocking().exchange(HttpRequest.DELETE("/topic/" + keyString));

        assertThat((CharSequence) response.getStatus()).isEqualTo(HttpStatus.OK);
        verify(this.ingestService, times(1)).deleteData(TOPIC, List.of(argument.getExpected()));
    }

    @ParameterizedTest(name = "[{index}] shouldDeleteKeyInBody for type {0}")
    @MethodSource("deleteFromBodyProvider")
    <K, V, E> void shouldDeleteKeyInBody(final QuickTopicType type, final TestKeyArgument<K, V, E> argument) {
        final K key = argument.getKey();
        when(this.ingestService.deleteData(eq(TOPIC), any())).thenReturn(Completable.complete());
        doReturn(Single.just(argument.getData())).when(this.typeService).getTopicData(TOPIC);
        final HttpResponse<?> response =
            this.client.toBlocking().exchange(HttpRequest.DELETE("/topic", key));

        assertThat((CharSequence) response.getStatus()).isEqualTo(HttpStatus.OK);
        verify(this.ingestService).deleteData(TOPIC, List.of(argument.getExpected()));
    }

    @Test
    void shouldThrowBadRequestErrorWhenJsonHasNoKeyOrValueField() {
        doReturn(Single.just(new QuickTopicData<>(TOPIC, TopicWriteType.MUTABLE, newStringData(), newStringData())))
            .when(this.typeService).getTopicData(TOPIC);

        final String jsonWithNoKeyValueField = "{\"a\":\"test\"}";

        final String expectedErrorMessage =
            String.format("Could not find 'key' or 'value' fields in: %s", jsonWithNoKeyValueField);

        assertThatExceptionOfType(HttpClientResponseException.class)
            .isThrownBy(
                () -> this.client.retrieve(creatIngestRequest(jsonWithNoKeyValueField)).blockingFirst())
            .isInstanceOfSatisfying(HttpClientResponseException.class,
                ex -> assertThat(this.extractErrorMessage(ex))
                    .isPresent()
                    .get()
                    .hasFieldOrPropertyWithValue("type", "errors/clientError")
                    .hasFieldOrPropertyWithValue("title", "Bad Request")
                    .extracting(ErrorMessage::getDetail, InstanceOfAssertFactories.STRING)
                    .isEqualTo(expectedErrorMessage));
    }

    @Test
    void shouldThrowBadRequestErrorWhenJsonIsNotValidToSchema() {
        doReturn(Single.just(new QuickTopicData<>(TOPIC, TopicWriteType.MUTABLE, newStringData(), getAvroInfo())))
            .when(this.typeService).getTopicData(TOPIC);

        final String invalidJsonRecord = "{\"key\": \"123\", \"value\": {\"invalidId\":1,\"invalidCountPlays\":2}}";
        final String expectedErrorMessage =
            "Data does not conform to schema: Field fieldId type:LONG pos:0 not set and has no default value";

        assertThatExceptionOfType(HttpClientResponseException.class)
            .isThrownBy(() -> this.client.retrieve(creatIngestRequest(invalidJsonRecord)).blockingFirst())
            .isInstanceOfSatisfying(HttpClientResponseException.class,
                ex -> assertThat(this.extractErrorMessage(ex))
                    .isPresent()
                    .get()
                    .hasFieldOrPropertyWithValue("type", "errors/clientError")
                    .hasFieldOrPropertyWithValue("title", "Bad Request")
                    .extracting(ErrorMessage::getDetail, InstanceOfAssertFactories.STRING)
                    .isEqualTo(expectedErrorMessage));
    }

    @ParameterizedTest(name = "[{index}] testWrongKeyType for type {0}")
    @MethodSource("keyParsingInvalidProvider")
    <K, V, I> void shouldThrowBadRequestErrorWhenKeyTypeIsWrong(final QuickTopicType type,
        final TestInvalidKeyArgument<K, V, I> argument) {

        final KeyValuePair<I, String> pair = new KeyValuePair<>(argument.getKey(), "value");

        when(this.ingestService.sendData(eq(TOPIC), any())).thenReturn(Completable.complete());

        doReturn(Single.just(argument.getData())).when(this.typeService).getTopicData(TOPIC);

        final String expectedErrorMessage = String.format("Data must be of type %s. Got:",
            type.getTypeResolver().getType().toString().toLowerCase());

        assertThatExceptionOfType(HttpClientResponseException.class)
            .isThrownBy(() -> this.client.retrieve(creatIngestRequest(pair)).blockingFirst())
            .isInstanceOfSatisfying(HttpClientResponseException.class,
                ex -> assertThat(this.extractErrorMessage(ex))
                    .isPresent()
                    .get()
                    .hasFieldOrPropertyWithValue("type", "errors/clientError")
                    .hasFieldOrPropertyWithValue("title", "Bad Request")
                    .extracting(ErrorMessage::getDetail, InstanceOfAssertFactories.STRING)
                    .startsWith(expectedErrorMessage));
    }

    @Test
    void testMethodNotAllowed() {
        assertThatExceptionOfType(HttpClientResponseException.class)
            .isThrownBy(() -> this.client.toBlocking().exchange(HttpRequest.GET("/topic/")))
            .withMessage("Method Not Allowed")
            .satisfies(ex -> assertThat((CharSequence) ex.getStatus()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED));
    }

    private Optional<ErrorMessage> extractErrorMessage(final HttpClientResponseException ex) {
        try {
            return Optional
                .ofNullable(this.objectMapper.readValue((String) ex.getResponse().body(), ErrorMessage.class));
        } catch (final JsonProcessingException e) {
            return Optional.empty();
        }
    }

    @MockBean(KafkaIngestService.class)
    IngestService ingestService() {
        return mock(IngestService.class);
    }

    @MockBean(QuickTopicTypeService.class)
    TopicTypeService topicTypeService() {
        return mock(TopicTypeService.class);
    }

    @MockBean(IngestFilter.class)
    IngestFilter ingestFilter() {
        return mock(IngestFilter.class);
    }

    @Value
    static class TestValueArgument<K, V, E> {
        QuickTopicData<K, V> data;
        V value;
        E expected;
    }

    @Value
    static class TestKeyArgument<K, V, E> {
        QuickTopicData<K, V> data;
        K key;
        E expected;
    }

    @Value
    static class TestInvalidKeyArgument<K, V, I> {
        QuickTopicData<K, V> data;
        I key;
    }
}
