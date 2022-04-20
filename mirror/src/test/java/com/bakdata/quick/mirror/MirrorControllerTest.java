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

import static io.restassured.RestAssured.when;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.bakdata.quick.avro.ChartRecord;
import com.bakdata.quick.common.api.model.mirror.MirrorValue;
import com.bakdata.quick.mirror.base.HostConfig;
import com.bakdata.quick.mirror.service.KafkaQueryService;
import com.bakdata.quick.mirror.service.QueryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Single;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.Value;
import org.apache.avro.generic.GenericData.Record;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@MicronautTest
@Property(name = "pod.ip", value = "127.0.0.1")
class MirrorControllerTest {

    @Inject
    private ObjectMapper objectMapper;
    @Inject
    private HostConfig hostConfig;
    @Inject
    private QueryService<?> queryService;

    @ParameterizedTest
    @MethodSource("keys")
    <V> void shouldReturnValuesForKey(final Argument<V> value) throws JsonProcessingException {
        doReturn(Single.just(new MirrorValue<>("test"))).when(this.queryService).get(anyString());

        final String expected = this.objectMapper.writeValueAsString(new MirrorValue<>("test"));
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() ->
                when()
                    .get("http://" + this.hostConfig.toConnectionString() + "/mirror/{id}", value.getData())
                    .then()
                    .statusCode(HttpStatus.OK.getCode())
                    .body(equalTo(expected))
            );
    }

    @Test
    void shouldReturnValuesForKeys() throws JsonProcessingException {
        doReturn(Single.just(new MirrorValue<>(List.of("test1", "test2", "test3")))).when(this.queryService)
            .getValues(List.of("1", "2", "3"));

        final String expected =
            this.objectMapper.writeValueAsString(new MirrorValue<>(List.of("test1", "test2", "test3")));
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() ->
                when()
                    .get("http://" + this.hostConfig.toConnectionString() + "/mirror/keys?ids=1,2,3")
                    .then()
                    .statusCode(HttpStatus.OK.getCode())
                    .body(equalTo(expected))
            );
    }

    @ParameterizedTest
    @MethodSource("values")
    <V> void shouldReturnValues(final Argument<V> value) throws JsonProcessingException {
        doReturn(Single.just(new MirrorValue<>(value.getData()))).when(this.queryService).get(anyString());

        final String expected = this.objectMapper.writeValueAsString(new MirrorValue<>(value.getData()));
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() ->
                when()
                    .get("http://" + this.hostConfig.toConnectionString() + "/mirror/{id}", "key")
                    .then()
                    .statusCode(HttpStatus.OK.getCode())
                    .body(equalTo(expected))
            );
    }


    @ParameterizedTest
    @MethodSource("values")
    <V> void shouldReturnListValues(final Argument<V> value) throws JsonProcessingException {
        final MirrorValue<List<V>> item = new MirrorValue<>(List.of(value.getData()));
        doReturn(Single.just(item)).when(this.queryService).getAll();

        final String expected = this.objectMapper.writeValueAsString(item);
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() ->
                when()
                    .get("http://" + this.hostConfig.toConnectionString() + "/mirror")
                    .then()
                    .statusCode(HttpStatus.OK.getCode())
                    .body(equalTo(expected))
            );
    }

    @MockBean(KafkaQueryService.class)
    QueryService queryService() {
        return mock(QueryService.class);
    }

    private static Stream<Argument<?>> keys() {
        return Stream.of("value", 5, 5.0, 5L).map(Argument::new);
    }

    private static Stream<Argument<?>> values() {
        return Stream.of("value", 5, 5.0, outputRecord(), 5L).map(Argument::new);
    }

    private static Record outputRecord() {
        final Record record = new Record(ChartRecord.getClassSchema());
        record.put(0, 5L);
        record.put(1, 5L);
        return record;
    }

    @Value
    static class Argument<T> {
        T data;
    }
}
