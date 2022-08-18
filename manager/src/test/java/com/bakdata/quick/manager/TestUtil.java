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

package com.bakdata.quick.manager;

import com.bakdata.quick.manager.config.ApplicationSpecificationConfig;
import com.bakdata.quick.manager.config.HardwareResource;
import com.bakdata.quick.manager.config.HardwareResource.Cpu;
import com.bakdata.quick.manager.config.HardwareResource.Memory;
import java.util.Optional;

/**
 * Utility class for tests.
 */
public final class TestUtil {

    private TestUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static ApplicationSpecificationConfig newAppSpec() {
        return new ApplicationSpecificationConfig(Optional.empty(), newResourceConfig());
    }

    private static HardwareResource newResourceConfig() {
        return new HardwareResource(newMemoryResource("250Mi", "500Mi"),
            newCpuResource("1", "5")
        );
    }

    /**
     * Returns memory k8s settings.
     */
    private static Memory newMemoryResource(final String request, final String limit) {
        return new Memory() {
            @Override
            public String getLimit() {
                return limit;
            }

            @Override
            public String getRequest() {
                return request;
            }
        };
    }

    /**
     * Returns cpu k8s settings.
     */
    private static Cpu newCpuResource(final String request, final String limit) {
        return new Cpu() {
            @Override
            public String getLimit() {
                return limit;
            }

            @Override
            public String getRequest() {
                return request;
            }
        };
    }
}
