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

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Wrapper holding values for lazy initialization.
 *
 * @param <T> value supplied lazily
 */
public class Lazy<T> {
    private final Supplier<? extends T> supplier;
    @Nullable
    private T value;

    public Lazy(final Supplier<? extends T> supplier) {
        this.supplier = supplier;
        this.value = null;
    }

    /**
     * Returns value that is lazily initialized.
     */
    @NonNull
    public T get() {
        if (this.value == null) {
            this.value = this.supplier.get();
            Objects.requireNonNull(this.value, "Lazy initialization must not return null.");
        }
        return this.value;
    }
}
