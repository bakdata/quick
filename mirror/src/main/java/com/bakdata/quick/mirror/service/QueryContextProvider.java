package com.bakdata.quick.mirror.service;

import javax.inject.Provider;

public interface QueryContextProvider<K, V> extends Provider<QueryServiceContext<K, V>> {

    void setQueryContext(QueryServiceContext<K, V> context);
}
