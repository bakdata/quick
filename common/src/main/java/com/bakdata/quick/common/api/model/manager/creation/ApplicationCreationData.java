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

package com.bakdata.quick.common.api.model.manager.creation;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Map;
import lombok.Value;

/**
 * User supplied data for creating a new application.
 */
@Value
public class ApplicationCreationData implements CreationData {
    String name;
    String registry;
    String imageName;
    String tag;
    @Nullable
    Integer replicas;
    @Nullable
    Integer port;
    @Nullable
    Map<String, String> arguments;
}
