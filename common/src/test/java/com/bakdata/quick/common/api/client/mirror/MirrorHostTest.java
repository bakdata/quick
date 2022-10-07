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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.Test;

class MirrorHostTest {
    private static final String MIRROR_HOST_PREFIX = "quick-mirror-";
    private static final String MIRROR_HOST_PATH = "mirror";

    @Test
    void shouldConstructCorrectUrlForKeyRequest() {
        final MirrorHost mirrorHost = MirrorHost.createWithPrefix("test-for-key");
        final HttpUrl actual = mirrorHost.forKey("give-me-key");
        final String url = "http://%stest-for-key/%s/give-me-key";
        final String expected = String.format(url, MIRROR_HOST_PREFIX, MIRROR_HOST_PATH);
        assertThat(actual.toString()).isEqualTo(expected);
    }

    @Test
    void shouldConstructCorrectUrlForKeysRequest() {
        final MirrorHost mirrorHost = MirrorHost.createWithPrefix("test-for-keys");
        final HttpUrl actual = mirrorHost.forKeys(List.of("test-1", "test-2", "test-3"));
        final String url = "http://%stest-for-keys/%s/keys?ids=test-1,test-2,test-3";
        final String expected = String.format(url, MIRROR_HOST_PREFIX, MIRROR_HOST_PATH);
        assertThat(actual.toString()).isEqualTo(expected);
    }

    @Test
    void shouldConstructCorrectUrlForAllRequest() {
        final MirrorHost mirrorHost = MirrorHost.createWithPrefix("test-for-all");
        final HttpUrl actual = mirrorHost.forAll();
        final String url = "http://%stest-for-all/%s";
        final String expected = String.format(url, MIRROR_HOST_PREFIX, MIRROR_HOST_PATH);
        assertThat(actual.toString()).isEqualTo(expected);
    }

    @Test
    void shouldConstructCorrectUrlForRangeRequest() {
        final MirrorHost mirrorHost = MirrorHost.createWithPrefix("test-for-rage");
        final HttpUrl actual = mirrorHost.forRange("test-key", "range-field-from", "range-field-to");
        final String url = "http://%stest-for-rage/%s/range/%s?from=%s&to=%s";
        final String expected =
            String.format(url, MIRROR_HOST_PREFIX, MIRROR_HOST_PATH, "test-key", "range-field-from", "range-field-to");
        assertThat(actual.toString()).isEqualTo(expected);
    }

    @Test
    void shouldBeEqualIfTheTopicNameIsTheSame() {
        final MirrorHost firstMirrorHost = MirrorHost.createWithPrefix("topic-1");
        final MirrorHost secondMirrorHost = MirrorHost.createWithPrefix("topic-1");
        assertThat(firstMirrorHost).isEqualTo(secondMirrorHost);
    }

    @Test
    void shouldReturnHostWhenConvertedToString() {
        final MirrorHost mirrorHost = MirrorHost.createWithPrefix("test-for-to-string");
        assertThat(mirrorHost.toString()).isEqualTo(
            String.format("http://%s%s/%s", MIRROR_HOST_PREFIX, "test-for-to-string", MIRROR_HOST_PATH));
    }

    @Test
    void shouldConstructCorrectUrlWithIpAndPort() {
        final MirrorHost mirrorHost = MirrorHost.createWithNoPrefix("10.30.40.0:8080");
        final HttpUrl actual = mirrorHost.forKey("give-me-key");
        final String url = "http://10.30.40.0:8080/%s/give-me-key";
        final String expected = String.format(url, MIRROR_HOST_PATH);
        assertThat(actual.toString()).isEqualTo(expected);
    }
}
