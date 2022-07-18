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

/**
 * A utility class that contains various constants used in Quick.
 */
public class QuickConstants {

    private QuickConstants() {}

    private static final String UPDATE_PARTITION_HOST_MAPPING_HEADER = "X-Cache-Update";
    private static final String CACHE_UPDATE_MESSAGE = "There was a cache miss. Please update routing information.";

    public static String getUpdateMappingHeader() {
        return UPDATE_PARTITION_HOST_MAPPING_HEADER;
    }

    public static String getCacheUpdateMessage() {
        return CACHE_UPDATE_MESSAGE;
    }


}
