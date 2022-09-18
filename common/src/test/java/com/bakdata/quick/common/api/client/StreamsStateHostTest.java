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

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.quick.common.api.model.mirror.MirrorHost;
import com.bakdata.quick.common.config.MirrorConfig;
import org.junit.jupiter.api.Test;

class StreamsStateHostTest {
    private static final String MIRROR_HOST_PREFIX = "quick-mirror";

    @Test
    void shouldConstructCorrectUrlForStreamStateHost() {
        final MirrorHost mirrorHost = new MirrorHost("test-for-streams", new MirrorConfig());
        final StreamsStateHost streamsStateHost = StreamsStateHost.fromMirrorHost(mirrorHost);
        final String actual = streamsStateHost.getPartitionToHostUrl();
        final String url = "http://%s-test-for-streams/streams/partitions";
        final String expected = String.format(url, MIRROR_HOST_PREFIX);
        assertThat(actual).isEqualTo(expected);
    }
}
