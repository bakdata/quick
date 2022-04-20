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

package com.bakdata.quick.manager.application;

import static io.micronaut.http.HttpRequest.DELETE;
import static io.micronaut.http.HttpRequest.GET;
import static io.micronaut.http.HttpRequest.POST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bakdata.quick.common.api.client.ApplicationClient;
import com.bakdata.quick.common.api.model.manager.creation.ApplicationCreationData;
import com.bakdata.quick.common.api.model.manager.ApplicationDescription;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@MicronautTest
class ApplicationControllerTest {
    private static final String NAME = "test-application-name";
    private static final String REGISTRY = "hub.docker.com";
    private static final String IMAGE_NAME = "test-image-name";
    private static final String TAG = "test-version";
    private static final int REPLICAS = 3;
    private static final int PORT = 8080;
    private static final Map<String, String> ARGS = Map.of("--arg1", "value1");


    @Client("/")
    @Inject
    RxHttpClient httpClient;
    @Inject
    ApplicationClient applicationClient;
    @Inject
    ApplicationService service;

    @Test
    void shouldGetApplicationInformation() {
        final ApplicationDescription expected = new ApplicationDescription(NAME);

        when(this.service.getApplicationInformation(anyString())).thenReturn(Single.just(
            expected));

        final ApplicationDescription result = this.applicationClient.getApplicationInformation(NAME).blockingGet();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldDeployApplication() {
        final ApplicationCreationData applicationCreationData =
            new ApplicationCreationData(NAME, REGISTRY, IMAGE_NAME, TAG, REPLICAS, PORT, ARGS);

        when(this.service.deployApplication(applicationCreationData))
            .thenReturn(Completable.complete());

        final Completable completable = this.applicationClient.deployApplication(applicationCreationData);
        Optional.ofNullable(completable.blockingGet()).ifPresent(Assertions::fail);
    }

    @Test
    void shouldDeleteApplication() {
        when(this.service.deleteApplication(NAME)).thenReturn(Completable.complete());

        when(this.service.deleteApplication(NAME))
            .thenReturn(Completable.complete());

        final Completable completable = this.applicationClient.deleteApplication(NAME);
        Optional.ofNullable(completable.blockingGet()).ifPresent(Assertions::fail);
    }

    @Test
    void shouldCallGetApplicationInformation() {
        final String getAppInfoUri = UriBuilder.of("/application/{name}/")
            .expand(Collections.singletonMap("name", NAME))
            .toString();

        final ApplicationDescription expected = new ApplicationDescription(NAME);

        when(this.service.getApplicationInformation(anyString())).thenReturn(Single.just(
            expected));

        this.httpClient.toBlocking().exchange(GET(getAppInfoUri));

        verify(this.service).getApplicationInformation(NAME);
    }

    @Test
    void shouldCallDeployApplication() {
        final ApplicationCreationData applicationCreationData =
            new ApplicationCreationData(NAME, REGISTRY, IMAGE_NAME, TAG, REPLICAS, PORT, ARGS);

        when(this.service.deployApplication(applicationCreationData))
            .thenReturn(Completable.complete());

        this.httpClient.toBlocking().exchange(POST("/application/", applicationCreationData));

        verify(this.service).deployApplication(applicationCreationData);
    }

    @Test
    void shouldCallDeleteApplication() {
        final String deletionUri = UriBuilder.of("/application/{name}/")
            .expand(Collections.singletonMap("name", NAME))
            .toString();

        when(this.service.deleteApplication(NAME)).thenReturn(Completable.complete());

        this.httpClient.toBlocking().exchange(DELETE(deletionUri));

        verify(this.service).deleteApplication(NAME);
    }

    @MockBean(ApplicationService.class)
    ApplicationService applicationService() {
        return mock(ApplicationService.class);
    }
}
