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

package com.bakdata.quick.common.api.client;

import static com.bakdata.quick.common.api.client.ClientUtils.createMirrorUrlFromRequest;
import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.quick.common.api.model.mirror.MirrorHost;
import com.bakdata.quick.common.config.MirrorConfig;
import okhttp3.Request;
import org.junit.jupiter.api.Test;

class ClientUtilsTest {
    @Test
    void shouldCreateCorrectMirrorUrlFromRequestWithNoQueryParameter() {
        final MirrorHost mirrorHost = new MirrorHost("test-client-utils", new MirrorConfig());
        final Request failedRequest = new Request.Builder().url(mirrorHost.forKey("123")).build();
        final String mirrorUrlFromRequest = createMirrorUrlFromRequest(failedRequest, mirrorHost);
        assertThat(mirrorUrlFromRequest).isEqualTo("http://quick-mirror-test-client-utils/mirror/123");
    }

    @Test
    void shouldCreateCorrectMirrorUrlFromRequestWithQueryParameter() {
        final MirrorHost mirrorHost = new MirrorHost("test-client-utils", new MirrorConfig());
        final Request failedRequest = new Request.Builder().url(mirrorHost.forRange("123", "1", "3")).build();
        final String mirrorUrlFromRequest = createMirrorUrlFromRequest(failedRequest, mirrorHost);
        assertThat(mirrorUrlFromRequest).isEqualTo(
            "http://quick-mirror-test-client-utils/mirror/range/123?from=1&to=3");
    }
}
