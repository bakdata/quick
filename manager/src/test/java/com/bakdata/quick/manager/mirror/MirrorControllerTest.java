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

package com.bakdata.quick.manager.mirror;

import static com.bakdata.quick.manager.TestUtil.createDefaultMirrorCreationData;
import static io.micronaut.http.HttpRequest.DELETE;
import static io.micronaut.http.HttpRequest.POST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bakdata.quick.common.api.model.manager.creation.MirrorArguments;
import com.bakdata.quick.common.api.model.manager.creation.MirrorCreationData;
import io.micronaut.rxjava2.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Completable;
import jakarta.inject.Inject;
import java.util.Collections;
import org.junit.jupiter.api.Test;

@MicronautTest
class MirrorControllerTest {
    private static final String NAME = "test-topic";

    @Client("/")
    @Inject
    RxHttpClient httpClient;

    @Inject
    MirrorService service;

    @Test
    void shouldCreateMirrorWithDefaultValues() {
        final MirrorCreationData mirrorCreationData = createDefaultMirrorCreationData(NAME);
        when(this.service.createMirror(eq(mirrorCreationData))).thenReturn(Completable.complete());

        this.httpClient.toBlocking().exchange(POST("topic/mirror", mirrorCreationData));

        verify(this.service).createMirror(mirrorCreationData);
    }

    @Test
    void shouldCreateMirrorWithMirrorArguments() {
        final MirrorCreationData mirrorCreationData = new MirrorCreationData(
            NAME,
            NAME,
            1,
            null,
            new MirrorArguments(null, "test-range-field", "test-range-key"));

        when(this.service.createMirror(eq(mirrorCreationData))).thenReturn(Completable.complete());
        this.httpClient.toBlocking().exchange(POST("topic/mirror", mirrorCreationData));

        verify(this.service).createMirror(mirrorCreationData);
    }

    @Test
    void shouldDeleteMirror() {
        final String deletionUri = UriBuilder.of("/topic/{name}/mirror")
            .expand(Collections.singletonMap("name", NAME))
            .toString();

        when(this.service.deleteMirror(anyString())).thenReturn(Completable.complete());
        this.httpClient.toBlocking().exchange(DELETE(deletionUri));
        verify(this.service).deleteMirror(NAME);
    }

    @MockBean(value = MirrorService.class)
    MirrorService mirrorService() {
        return mock(MirrorService.class);
    }
}
