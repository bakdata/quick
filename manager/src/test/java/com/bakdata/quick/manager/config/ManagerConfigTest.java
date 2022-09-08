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

package com.bakdata.quick.manager.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.quick.common.ConfigUtils;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ManagerConfigTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCreateManagerConfigWithExpectedValues(final boolean configFlag) {
        final Map<String, Object> properties = Map.of(
            "quick.manager.update-managed-images", configFlag,
            "quick.manager.create-topic-registry", configFlag
        );
        final ManagerConfig managerConfig = ConfigUtils.createWithProperties(properties, ManagerConfig.class);
        assertThat(managerConfig.isUpdateManagedImages()).isEqualTo(configFlag);
        assertThat(managerConfig.isCreateTopicRegistry()).isEqualTo(configFlag);
    }
}
