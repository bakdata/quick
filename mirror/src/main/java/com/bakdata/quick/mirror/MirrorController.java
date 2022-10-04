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

package com.bakdata.quick.mirror;

import com.bakdata.quick.common.api.model.mirror.MirrorValue;
import com.bakdata.quick.mirror.service.QueryService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;
import io.reactivex.Single;
import jakarta.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * REST API of mirror applications.
 *
 * @param <K> key type
 * @param <V> value type
 */
@Slf4j
@Controller("/mirror")
public class MirrorController<K, V> {
    private final QueryService<V> queryService;

    @Inject
    public MirrorController(final QueryService<V> queryService) {
        this.queryService = queryService;
    }

    /**
     * Fetches values for the given key.
     */
    @Get("/{key}")
    public Single<HttpResponse<MirrorValue<V>>> get(@PathVariable("key") final String keyString) {
        log.debug("Request for key {}", keyString);
        return this.queryService.get(keyString);
    }

    /**
     * Fetches values for given ids in body.
     *
     * @param ids the ids to fetch
     * @return list of values for given keys
     */
    @Get("/keys")
    public Single<HttpResponse<MirrorValue<List<V>>>> getList(@QueryValue() final List<String> ids) {
        log.debug("Request for keys {}", ids);
        return this.queryService.getValues(ids);
    }

    /**
     * Fetches all values stores by this mirror.
     */
    @Get
    public Single<HttpResponse<MirrorValue<List<V>>>> getAll() {
        log.debug("Request for all existing keys.");
        return this.queryService.getAll();
    }

    /**
     * Fetches a range of values over a given key and a range field.
     */
    @Get("/range/{key}")
    public Single<HttpResponse<MirrorValue<List<V>>>> getRange(@PathVariable("key") final String keyString,
        @QueryValue final String from, @QueryValue final String to) {
        log.debug("Request for range with key {} from {} to {}", keyString, from, to);
        return this.queryService.getRange(keyString, from, to);
    }
}
