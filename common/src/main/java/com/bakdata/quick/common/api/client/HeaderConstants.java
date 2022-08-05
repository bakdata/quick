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

/**
 * A collection of constants for managing headers.
 */
public class HeaderConstants {

    private static final String UPDATE_PARTITION_HOST_MAPPING_HEADER = "X-Cache-Update";
    // The constant below indicates the existence of a header.
    // See: https://stackoverflow.com/a/65241869 for more details.
    private static final String HEADER_EXISTS = "?1";

    private HeaderConstants() {
    }

    public static String getCacheMissHeaderName() {
        return UPDATE_PARTITION_HOST_MAPPING_HEADER;
    }

    public static String getCacheMissHeaderValue() {
        return HEADER_EXISTS;
    }

}
