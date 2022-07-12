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

package com.bakdata.quick.manager.k8s;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.utils.Serialization;
import java.util.Collection;
import javax.inject.Singleton;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Contains custom k8s resources.
 */
@Singleton
public final class KubernetesResources {
    /*
     * This class works with the templating engine thymeleaf. In the resources directory, there are templates for all
     *  k8s resource Quick creates.
     */
    public static final int CONTAINER_PORT = 8080;
    public static final int SERVICE_PORT = 80;
    public static final String IMAGE_PULL_SECRET = "quick-image-secret";
    public static final String QUICK_CONFIG_NAME = "quick-config";
    public static final String QUICK_API_KEY_SECRET = "api-key-secret";
    private final TemplateEngine engine;

    /**
     * Constructor loading the template engine.
     */
    public KubernetesResources() {
        this.engine = new TemplateEngine();
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setTemplateMode(TemplateMode.TEXT);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setPrefix("/k8s/templates/");
        resolver.setSuffix(".th.yaml");
        this.engine.setTemplateResolver(resolver);
    }

    /**
     * Creates a dummy resource with a name.
     *
     * @param k8sResourceClass class of the resource
     * @param name             name of the resource
     * @param <T>              type of the resource
     * @return a resource object with the given name
     */
    @SuppressWarnings("OverlyBroadCatchBlock") // No need for handling reflective exceptions independently
    public static <T extends HasMetadata> T forDeletion(final Class<T> k8sResourceClass, final String name) {
        final ObjectMeta objectMeta = new ObjectMeta();
        objectMeta.setName(name);
        final T namedResource;
        try {
            namedResource = k8sResourceClass.getDeclaredConstructor().newInstance();
        } catch (final ReflectiveOperationException exception) {
            final String errorMessage =
                String.format("Could not create object of class %s. It must be a serializable k8s resource.",
                    k8sResourceClass.getName());
            throw new IllegalArgumentException(errorMessage, exception);
        }
        namedResource.setMetadata(objectMeta);
        return namedResource;
    }

    /**
     * K8s job for deleting Kafka related state of an application.
     *
     * <p>
     * The job starts a pods that runs single time invoking the stream reset functionality of common kafka streams.
     *
     * @see
     * <a href="https://github.com/bakdata/common-kafka-streams/blob/master/charts/streams-app-cleanup-job/templates/job.yaml">Helm
     * Chart Definition</a>
     * @see
     * <a href="https://github.com/bakdata/common-kafka-streams/blob/master/src/main/java/com/bakdata/common_kafka_streams/CleanUpRunner.java">Clean
     * Up Runner</a>
     */
    public Job createDeletionJob(final String name, final String image, final Collection<String> arguments) {
        final Context root = new Context();
        final String jobName = name + "-deletion";
        root.setVariable("name", jobName);
        root.setVariable("image", image);
        root.setVariable("args", arguments);
        root.setVariable("releaseName", "quick-dev");
        root.setVariable("pullPolicy", "Always");
        return this.loadResource(root, "streamsApp/deletion-job", Job.class);
    }

    public <R> R loadResource(final Context root, final String template, final Class<R> resourceClass) {
        final String yaml = this.engine.process(template, root);
        return Serialization.unmarshal(yaml, resourceClass);
    }
}
