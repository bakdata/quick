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

package com.bakdata.quick.common.util;

import com.bakdata.quick.common.config.KafkaConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Utility class for converting CLI arguments.
 */
public final class CliArgHandler {

    private CliArgHandler() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Converts a kafka configuration into cli args as expected by streams-bootstrap.
     *
     * @param kafkaConfig bootstrap server and schema registry url
     * @return list containing "--brokers=XX" and "--schema-registry-url=XX"
     */
    public static List<String> convertArgs(final KafkaConfig kafkaConfig) {
        return convertArgs(Collections.emptyMap(), kafkaConfig);
    }

    /**
     * Converts key value pairs into cli arguments.
     *
     * @param args arguments to convert
     * @return list containing strings formatted "--key=value"
     */
    public static List<String> convertArgs(final Map<String, String> args) {
        return args.entrySet().stream().map(CliArgHandler::toCliParameter).collect(Collectors.toList());
    }

    /**
     * Converts arbitrary key value pairs and a kafka config into cli args.
     *
     * @param args        arguments to convert
     * @param kafkaConfig bootstrap server and schema registry url
     * @return list containing strings formatted "--key=value" and "--brokers=XX" and "--schema-registry-url=XX"
     */
    public static List<String> convertArgs(final Map<String, String> args, final KafkaConfig kafkaConfig) {
        final List<String> listArgs = new ArrayList<>();
        listArgs.add(String.format("%s=%s", "--brokers", kafkaConfig.getBootstrapServer()));
        listArgs.add(String.format("%s=%s", "--schema-registry-url", kafkaConfig.getSchemaRegistryUrl()));
        args.entrySet().stream().map(CliArgHandler::toCliParameter).forEach(listArgs::add);
        return listArgs;
    }

    private static String toCliParameter(final Entry<String, String> argument) {
        String key = argument.getKey();
        if (!key.startsWith("--")) {
            key = "--" + key;
        }
        return String.format("%s=%s", key, argument.getValue());
    }
}
