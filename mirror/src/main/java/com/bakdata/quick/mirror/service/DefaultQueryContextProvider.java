package com.bakdata.quick.mirror.service;

import com.bakdata.quick.common.exception.InternalErrorException;
import edu.umd.cs.findbugs.annotations.Nullable;

import javax.inject.Singleton;

@Singleton
public class DefaultQueryContextProvider<K, V> implements QueryContextProvider<K, V> {

    @Nullable
    private QueryServiceContext<K, V> queryServiceContext;

    @Override
    public void setQueryContext(QueryServiceContext<K, V> context) {
        this.queryServiceContext = context;
    }

    @Override
    public QueryServiceContext<K, V> get() {
        if (this.queryServiceContext == null) {
            throw new InternalErrorException("A context for the query service has not been set.");
        }
        return this.queryServiceContext;
    }
}
