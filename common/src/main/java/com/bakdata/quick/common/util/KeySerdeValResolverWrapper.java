package com.bakdata.quick.common.util;

import com.bakdata.quick.common.resolver.TypeResolver;
import lombok.Getter;
import org.apache.kafka.common.serialization.Serde;

@Getter
public class KeySerdeValResolverWrapper<K, V> {

    private final Serde<K> keySerde;
    private final TypeResolver<V> valueTypeResolver;

    public KeySerdeValResolverWrapper(Serde<K> keySerde, TypeResolver<V> valueTypeResolver) {
        this.keySerde = keySerde;
        this.valueTypeResolver = valueTypeResolver;
    }
}
