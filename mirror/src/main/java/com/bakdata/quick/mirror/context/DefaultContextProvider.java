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

package com.bakdata.quick.mirror.context;

import com.bakdata.quick.common.exception.MirrorTopologyException;
import edu.umd.cs.findbugs.annotations.Nullable;
import jakarta.inject.Singleton;

/**
 * Basic implementation of MirrorContextProvider.
 */
@Singleton
public class DefaultContextProvider<K, V> implements MirrorContextProvider<K, V> {

    private MirrorContext<?, V> mirrorContext = MirrorContext.<K, V>builder().build();

    @Override
    public MirrorContext<?, V> get() {
        if (this.mirrorContext == null) {
            throw new MirrorTopologyException("A context for the topology has not been set.");
        }
        return this.mirrorContext;
    }

    @Override
    public void setMirrorContext(final MirrorContext<?, V> context) {
        this.mirrorContext = context;
    }
}
