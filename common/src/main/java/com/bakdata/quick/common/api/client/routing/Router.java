package com.bakdata.quick.common.api.client.routing;

import com.bakdata.quick.common.api.model.mirror.MirrorHost;
import com.bakdata.quick.common.type.QuickTopicType;

public interface Router<K> {

    MirrorHost getHost(K key);
}
