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

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import okhttp3.ResponseBody;

/**
 * A wrapper for a response from a call to made with the HttpClient.
 * It consists of a response body extracted from the response and an optional field
 * for keeping the information about the custom X-Cache-Update header. This header is used to
 * signal the need for an update of the mapping between partitions and mirror hosts.
 */
@Getter
@Setter
public class ResponseWrapper {

    @Nullable
    private ResponseBody responseBody;
    @NonNull
    private Optional<String> updateCacheHeader;

    public ResponseWrapper() {
        this.updateCacheHeader = Optional.empty();
    }
}
