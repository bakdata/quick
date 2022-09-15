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

package com.bakdata.quick.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.quick.common.ConfigUtils;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GatewayConfigTest {
    @Test
    void createConfig() {
        final Map<String, Object> properties = Map.of("quick.schema.path", "/test/path/schema.graphql");
        final GatewayConfig config = ConfigUtils.createWithProperties(properties, GatewayConfig.class);
        assertThat(config.getPath()).isEqualTo("/test/path/schema.graphql");
    }
}
