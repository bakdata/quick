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

package com.bakdata.quick.common.api.client.mirror;

import static com.bakdata.quick.common.api.client.TestUtils.mockResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bakdata.quick.common.api.client.HttpClient;
import com.bakdata.quick.common.api.client.routing.PartitionRouter;
import com.bakdata.quick.common.config.MirrorConfig;
import com.bakdata.quick.common.resolver.StringResolver;
import java.util.List;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.Test;

class PartitionedMirrorClientTest {
    private final HttpClient mockClient = mock(HttpClient.class);
    private final MirrorRequestManager mockRequestManager = mock(MirrorRequestManager.class);
    private final PartitionRouter<String> mockRouter = mock(PartitionRouter.class);
    private final MirrorClient<String, String> stringMirrorClient =
        new PartitionedMirrorClient<>(this.mockClient, new StringResolver(), this.mockRequestManager, this.mockRouter);

    @Test
    void shouldCallUpdateRoutingInfoWhenUpdateHeaderIsSetWhenFetchingValue() {
        final List<MirrorHost> singleReplicaMirrorWithTwoPartitions = List.of(
            new MirrorHost("123.456.789.000", MirrorConfig.directAccess()),
            new MirrorHost("123.456.789.000", MirrorConfig.directAccess())
        );
        final MirrorHost firstPartition = singleReplicaMirrorWithTwoPartitions.get(0);
        final MirrorHost secondPartition = singleReplicaMirrorWithTwoPartitions.get(1);

        when(this.mockRouter.findHost(eq("key-1"))).thenReturn(firstPartition);
        final HttpUrl httpUrl = firstPartition.forKey("key-1");
        final ResponseWrapper response = ResponseWrapper.fromResponse(mockResponse());
        when(this.mockRequestManager.makeRequest(eq(httpUrl))).thenReturn(response);
        when(this.mockRequestManager.processResponse(eq(response), any())).thenReturn("value-1");
        final String value1 = this.stringMirrorClient.fetchValue("key-1");

        assertThat(value1).isEqualTo("value-1");
        verify(this.mockRequestManager).makeRequest(eq(httpUrl));
        verify(this.mockRequestManager).processResponse(eq(response), any());

        when(this.mockRouter.findHost(eq("key-2"))).thenReturn(secondPartition);
        final HttpUrl secondKeyUrl = secondPartition.forKey("key-2");
        final ResponseWrapper fallbackResponse = ResponseWrapper.fromFallbackResponse(mockResponse());
        when(this.mockRequestManager.makeRequest(eq(secondKeyUrl))).thenReturn(fallbackResponse);
        when(this.mockRequestManager.processResponse(eq(fallbackResponse), any())).thenReturn("value-2");

        final String value2 = this.stringMirrorClient.fetchValue("key-2");

        verify(this.mockRouter).updateRoutingInfo();
        assertThat(value2).isEqualTo("value-2");
    }

    @Test
    void shouldReturnAllValuesFromMirrorWithTwoReplicaWhenFetchingAll() {
        final List<MirrorHost> multiReplicaMirror = List.of(
            new MirrorHost("123.456.789.000", MirrorConfig.directAccess()),
            new MirrorHost("000.987.654.321", MirrorConfig.directAccess())
        );

        final List<String> firstMirrorValues = List.of("value1", "value2");
        final List<String> secondMirrorValues = List.of("value3", "value4");

        when(this.mockRouter.getAllHosts()).thenReturn(multiReplicaMirror);
        final ResponseWrapper response = ResponseWrapper.fromResponse(mockResponse());
        when(this.mockRequestManager.makeRequest(any())).thenReturn(response);
        when(this.mockRequestManager.processResponse(eq(response), any())).thenReturn(firstMirrorValues,
            secondMirrorValues);

        final List<String> allValues = this.stringMirrorClient.fetchAll();

        verify(this.mockRequestManager, times(2)).makeRequest(any());
        verify(this.mockRequestManager, times(2)).processResponse(any(), any());
        assertThat(allValues).hasSize(4).containsAll(List.of("value1", "value2", "value3", "value4"));
    }

    @Test
    void shouldReturnAllValuesFromMirrorWithTwoReplicaWhenFetchValues() {
        final List<MirrorHost> multiReplicaMirror = List.of(
            new MirrorHost("123.456.789.000", MirrorConfig.directAccess()),
            new MirrorHost("000.987.654.321", MirrorConfig.directAccess())
        );

        final List<String> queriedKeys = List.of("key-1", "key-2", "key-3", "key-4");
        final List<String> firstMirrorValues = List.of("value-1", "value-2");
        final List<String> secondMirrorValues = List.of("value-3", "value-4");

        final MirrorHost firstReplica = multiReplicaMirror.get(0);
        when(this.mockRouter.findHost("key-1")).thenReturn(firstReplica);
        when(this.mockRouter.findHost("key-2")).thenReturn(firstReplica);
        final MirrorHost secondReplica = multiReplicaMirror.get(1);
        when(this.mockRouter.findHost("key-3")).thenReturn(secondReplica);
        when(this.mockRouter.findHost("key-4")).thenReturn(secondReplica);

        final HttpUrl urlForFirstMirror = firstReplica.forKeys(List.of("key-1", "key-2"));
        final ResponseWrapper response = ResponseWrapper.fromResponse(mockResponse());
        when(this.mockRequestManager.makeRequest(eq(urlForFirstMirror))).thenReturn(response);
        when(this.mockRequestManager.processResponse(eq(response), any())).thenReturn(firstMirrorValues);

        final HttpUrl urlForSecondMirror = secondReplica.forKeys(List.of("key-3", "key-4"));
        final ResponseWrapper fallbackResponse = ResponseWrapper.fromFallbackResponse(mockResponse());
        when(this.mockRequestManager.makeRequest(eq(urlForSecondMirror))).thenReturn(
            fallbackResponse);
        when(this.mockRequestManager.processResponse(eq(fallbackResponse), any())).thenReturn(secondMirrorValues);

        final List<String> allValues = this.stringMirrorClient.fetchValues(queriedKeys);

        verify(this.mockRequestManager, times(2)).makeRequest(any());
        verify(this.mockRequestManager, times(2)).processResponse(any(), any());
        verify(this.mockRouter).updateRoutingInfo();
        assertThat(allValues).hasSize(4).containsAll(List.of("value-1", "value-2", "value-3", "value-4"));
    }

    @Test
    void shouldReturnAllValuesFromMirrorWithOneReplicaAndTwoPartitionsWhenFetchValues() {
        final List<MirrorHost> singleReplicaMirrorWithTwoPartitions = List.of(
            new MirrorHost("123.456.789.000", MirrorConfig.directAccess()),
            new MirrorHost("123.456.789.000", MirrorConfig.directAccess())
        );

        final MirrorHost firstPartitions = singleReplicaMirrorWithTwoPartitions.get(0);
        when(this.mockRouter.findHost("key-2")).thenReturn(firstPartitions);
        final MirrorHost secondPartition = singleReplicaMirrorWithTwoPartitions.get(1);
        when(this.mockRouter.findHost("key-3")).thenReturn(secondPartition);

        final List<String> queriedKeys = List.of("key-2", "key-3");
        final HttpUrl url = firstPartitions.forKeys(queriedKeys);
        final ResponseWrapper fallbackResponse = ResponseWrapper.fromFallbackResponse(mockResponse());
        when(this.mockRequestManager.makeRequest(eq(url))).thenReturn(fallbackResponse);
        when(this.mockRequestManager.processResponse(eq(fallbackResponse), any())).thenReturn(
            List.of("value-2", "value-3"));

        final List<String> allValues = this.stringMirrorClient.fetchValues(queriedKeys);

        verify(this.mockRequestManager).makeRequest(eq(url));
        verify(this.mockRequestManager).processResponse(eq(fallbackResponse), any());
        verify(this.mockRouter).updateRoutingInfo();
        assertThat(allValues).hasSize(2).containsAll(List.of("value-2", "value-3"));
    }

    @Test
    void shouldReturnAllValuesFromMirrorWithOneReplicaWhenFetchRange() {
        final MirrorHost singleReplica = new MirrorHost("123.456.789.000", MirrorConfig.directAccess());

        when(this.mockRouter.findHost("key-1")).thenReturn(singleReplica);
        final HttpUrl rangeUrl = singleReplica.forRange("key-1", "1", "4");
        final ResponseWrapper fallbackResponse = ResponseWrapper.fromFallbackResponse(mockResponse());
        when(this.mockRequestManager.makeRequest(eq(rangeUrl))).thenReturn(fallbackResponse);
        final List<String> values = List.of("value-1", "value-2", "value-3", "value-4");
        when(this.mockRequestManager.processResponse(eq(fallbackResponse), any())).thenReturn(values);

        final List<String> allValues = this.stringMirrorClient.fetchRange("key-1", "1", "4");

        verify(this.mockRequestManager).makeRequest(eq(rangeUrl));
        verify(this.mockRequestManager).processResponse(eq(fallbackResponse), any());
        verify(this.mockRouter).updateRoutingInfo();
        assertThat(allValues).hasSize(4).containsAll(values);
    }
}

