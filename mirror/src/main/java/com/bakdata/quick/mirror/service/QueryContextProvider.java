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

/**
 * Imitates the functionality of the javax's Provider
 * and extends it with an additional method to set a context.
 */
public interface QueryContextProvider {

    /**
     * Provides a fully-constructed and injected instance of QueryServiceContext.
     * @return An instance of the QueryServiceContext
     */
    QueryServiceContext get();

    /**
     * Sets a context. The idea behind this method is to
     * circumvent the need to a bean through the ApplicationContext
     * @param context an instance of QueryServiceContext
     */
    void setQueryContext(QueryServiceContext context);

}
