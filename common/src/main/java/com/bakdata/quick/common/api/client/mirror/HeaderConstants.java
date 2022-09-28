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

/**
 * A collection of constants for managing headers.
 */
public class HeaderConstants {

    // The X-Cache-Update header is set when there is a need to update the
    // partition-host mapping in the {@link com.bakdata.quick.common.api.client.routing.PartitionRouter}.
    // The need arises when a replica of the mirror is added or removed.
    public static final String UPDATE_PARTITION_HOST_MAPPING_HEADER = "X-Cache-Update";
    // The constant below indicates the existence of a header.
    // See: https://stackoverflow.com/a/65241869 for more details.
    public static final String HEADER_EXISTS = "?1";

    private HeaderConstants() {
    }
}
