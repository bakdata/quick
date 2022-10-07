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

import com.bakdata.quick.common.api.client.mirror.MirrorHost;
import okhttp3.HttpUrl;
import okhttp3.Request;

/**
 * Utils for Quick clients.
 */
public final class ClientUtils {

    private static final int DEFAULT_HTTP_PORT = 80;

    private ClientUtils() {
    }

    /**
     * Creates a new URL from a {@link Request} object.
     *
     * @param request The request object containing the URL
     * @param mirrorHost The Mirror host which contains the new URL
     */
    public static HttpUrl createMirrorUrlFromRequest(final Request request, final MirrorHost mirrorHost) {
        final String host = mirrorHost.getUrl().host();
        return request.url().newBuilder().host(host).port(DEFAULT_HTTP_PORT).build();
    }
}
