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

package com.bakdata.quick.mirror.service;

import com.bakdata.quick.common.exception.InternalErrorException;
import edu.umd.cs.findbugs.annotations.Nullable;
import jakarta.inject.Singleton;

/**
 * Basic implementation of QueryContextProvider.
 */
@Singleton
public class DefaultQueryContextProvider implements QueryContextProvider {

    @Nullable
    private QueryServiceContext queryServiceContext;

    @Override
    public void setQueryContext(final QueryServiceContext context) {
        this.queryServiceContext = context;
    }

    @Override
    public QueryServiceContext get() {
        if (this.queryServiceContext == null) {
            throw new InternalErrorException("A context for the query service has not been set.");
        }
        return this.queryServiceContext;
    }
}
