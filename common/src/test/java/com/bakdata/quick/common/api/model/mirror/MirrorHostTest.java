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

package com.bakdata.quick.common.api.model.mirror;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.quick.common.config.MirrorConfig;
import java.util.List;
import org.junit.jupiter.api.Test;

class MirrorHostTest {

    private static final String MIRROR_HOST_PREFIX = "quick-mirror";
    private static final String MIRROR_HOST_PATH = "mirror";

    @Test
    void shouldConstructCorrectUrlForKeyRequest() {
        final MirrorHost mirrorHost = new MirrorHost("test-for-key", new MirrorConfig());
        final String actual = mirrorHost.forKey("give-me-key");
        final String url = "http://%s-test-for-key/%s/give-me-key";
        assertThat(actual).isEqualTo(String.format(url, MIRROR_HOST_PREFIX, MIRROR_HOST_PATH));
    }

    @Test
    void shouldConstructCorrectUrlForKeysRequest() {
        final MirrorHost mirrorHost = new MirrorHost("test-for-keys", new MirrorConfig());
        final String actual = mirrorHost.forKeys(List.of("test-1", "test-2", "test-3"));
        final String url = "http://%s-test-for-keys/%s?ids=test-1,test-2,test-3";
        assertThat(actual).isEqualTo(String.format(url, MIRROR_HOST_PREFIX, MIRROR_HOST_PATH));
    }

    @Test
    void shouldConstructCorrectUrlForAllRequest() {
        final MirrorHost mirrorHost = new MirrorHost("test-for-all", new MirrorConfig());
        final String actual = mirrorHost.forAll();
        final String url = "http://%s-test-for-all/%s";
        assertThat(actual).isEqualTo(String.format(url, MIRROR_HOST_PREFIX, MIRROR_HOST_PATH));
    }

    @Test
    void shouldConstructCorrectUrlForRangeRequest() {
        final MirrorHost mirrorHost = new MirrorHost("test-for-rage", new MirrorConfig());
        final String actual = mirrorHost.forRange("test-key", "range-field-from", "range-field-to");
        final String url = "http://%s-test-for-rage/%s/%s?from=%s&to=%s";
        assertThat(actual).isEqualTo(
            String.format(url, MIRROR_HOST_PREFIX, MIRROR_HOST_PATH, "test-key", "range-field-from", "range-field-to"));
    }
}
