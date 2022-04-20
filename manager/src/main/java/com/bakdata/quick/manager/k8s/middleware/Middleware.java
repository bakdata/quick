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

package com.bakdata.quick.manager.k8s.middleware;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Singular;
import io.fabric8.kubernetes.model.annotation.Version;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

/**
 * POJO integrating  traefik's 'middleware' k8s CRD with fabric8's k8s client.
 *
 * <p>
 * This class supports only the <a href="https://doc.traefik.io/traefik/middlewares/stripprefix/">StripPrefix'
 * middleware</a>'.
 *
 * @see CustomResource
 * @see <a href="https://doc.traefik.io/traefik/middlewares/overview/">Traefik documentation</a>
 * @see
 * <a href="https://github.com/fabric8io/kubernetes-client/tree/master/kubernetes-examples/src/main/java/io/fabric8/kubernetes/examples/crds">
 * fabric8io's crd pojo example</a>
 */

@Version(Middleware.API_VERSION)
@Group(Middleware.GROUP)
@Kind(Middleware.KIND)
@Singular(Middleware.SINGULAR)
@Plural(Middleware.PLURAL)
// This creates a builder used by the kube-client
@Buildable(
    editableEnabled = false,
    builderPackage = Middleware.BUILDER_PATH,
    refs = @BuildableReference(CustomResource.class)
)
public class Middleware extends CustomResource<MiddlewareSpec, Void> implements Namespaced {
    public static final String API_VERSION = "v1alpha1";
    public static final String GROUP = "traefik.containo.us";
    public static final String KIND = "Middleware";
    public static final String SINGULAR = "middleware";
    public static final String PLURAL = "middlewares";

    static final String BUILDER_PATH = "io.fabric8.kubernetes.api.builder";
}
