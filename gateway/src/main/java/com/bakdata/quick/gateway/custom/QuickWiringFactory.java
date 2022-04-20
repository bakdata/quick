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

package com.bakdata.quick.gateway.custom;

import graphql.schema.DataFetcher;
import graphql.schema.idl.FieldWiringEnvironment;
import graphql.schema.idl.WiringFactory;

/**
 * Sets default data fetcher to custom QuickPropertyFetcher
 *
 * <p>
 * A {@link WiringFactory} setting the default {@link DataFetcher} to {@link QuickPropertyDataFetcher} instead of {@link
 * graphql.schema.PropertyDataFetcher}. This allows for handling {@link org.apache.avro.generic.GenericRecord}
 * properly.
 *
 * @see QuickPropertyDataFetcher
 * @see WiringFactory
 */
public class QuickWiringFactory implements WiringFactory {
    public static QuickWiringFactory create() {
        return new QuickWiringFactory();
    }

    @Override
    public DataFetcher getDefaultDataFetcher(final FieldWiringEnvironment environment) {
        return new QuickPropertyDataFetcher<>(environment.getFieldDefinition().getName());
    }
}
