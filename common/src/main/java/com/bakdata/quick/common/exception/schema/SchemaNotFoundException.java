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

package com.bakdata.quick.common.exception.schema;

import com.bakdata.quick.common.exception.QuickException;
import io.micronaut.http.HttpStatus;

/**
 * Exception when subject doesn't exist.
 */
public class SchemaNotFoundException extends QuickException {

    public SchemaNotFoundException(final String subject) {
        super(String.format("Subject \"%s\" not found in schema registry", subject));
    }

    @Override
    protected HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
