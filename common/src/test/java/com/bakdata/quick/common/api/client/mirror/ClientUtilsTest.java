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

import static com.bakdata.quick.common.api.client.ClientUtils.createMirrorUrlFromRequest;
import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.quick.common.config.MirrorConfig;
import okhttp3.HttpUrl;
import okhttp3.Request;
import org.junit.jupiter.api.Test;

class ClientUtilsTest {
    @Test
    void shouldCreateCorrectMirrorUrlFromRequestWithNoQueryParameter() {
        final MirrorHost unreachableMirror = new MirrorHost("unreachable-mirror", new MirrorConfig());
        final Request failedRequest = new Request.Builder().url(unreachableMirror.forKey("123")).build();
        final MirrorHost reachableMirror = new MirrorHost("healthy-mirror", new MirrorConfig());
        final HttpUrl mirrorUrlFromRequest = createMirrorUrlFromRequest(failedRequest, reachableMirror);
        assertThat(mirrorUrlFromRequest.toString()).isEqualTo("http://quick-mirror-healthy-mirror/mirror/123");
    }

    @Test
    void shouldCreateCorrectMirrorUrlFromRequestWithQueryParameter() {
        final MirrorHost unreachableMirror = new MirrorHost("unreachable-mirror", new MirrorConfig());
        final Request failedRequest = new Request.Builder().url(unreachableMirror.forRange("123", "1", "3")).build();
        final MirrorHost reachableMirror = new MirrorHost("healthy-mirror", new MirrorConfig());
        final HttpUrl mirrorUrlFromRequest = createMirrorUrlFromRequest(failedRequest, reachableMirror);
        assertThat(mirrorUrlFromRequest.toString()).isEqualTo(
            "http://quick-mirror-healthy-mirror/mirror/range/123?from=1&to=3");
    }

    @Test
    void shouldCreateCorrectMirrorUrlWithDirectAccessFromRequestWithNoQueryParameter() {
        final MirrorHost unreachableMirror = new MirrorHost("10.20.30.40:8080", MirrorConfig.directAccess());
        final Request failedRequest = new Request.Builder().url(unreachableMirror.forKey("123")).build();
        final MirrorHost reachableMirror = new MirrorHost("healthy-mirror", new MirrorConfig());
        final HttpUrl mirrorUrlFromRequest = createMirrorUrlFromRequest(failedRequest, reachableMirror);
        assertThat(mirrorUrlFromRequest.toString()).isEqualTo("http://quick-mirror-healthy-mirror/mirror/123");
    }
}
