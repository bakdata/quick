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

package com.bakdata.quick.manager.gateway;

import com.bakdata.quick.common.api.client.gateway.GatewayClient;
import com.bakdata.quick.common.api.model.gateway.SchemaData;
import com.bakdata.quick.common.api.model.manager.GatewayDescription;
import com.bakdata.quick.common.api.model.manager.GatewaySchema;
import com.bakdata.quick.common.api.model.manager.creation.GatewayCreationData;
import com.bakdata.quick.manager.gateway.resource.GatewayResourceLoader;
import com.bakdata.quick.manager.gateway.resource.GatewayResources;
import com.bakdata.quick.manager.graphql.GraphQLConverter;
import com.bakdata.quick.manager.k8s.KubernetesManagerClient;
import com.bakdata.quick.manager.k8s.resource.QuickResources.ResourcePrefix;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.functions.Action;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for handling gateway deployments on k8s cluster.
 */
@Singleton
@Requires(env = Environment.KUBERNETES)
@Slf4j
public class KubernetesGatewayService implements GatewayService {

    private final KubernetesManagerClient kubernetesManagerClient;
    private final GatewayClient gatewayClient;
    private final GraphQLConverter graphQLConverter;
    private final GatewayResourceLoader loader;

    /**
     * Injectable constructor.
     *
     * @param kubernetesManagerClient client for interacting with the k8s API
     * @param gatewayClient           client for communicating with gateways
     * @param loader                  the resource loader
     */
    public KubernetesGatewayService(final KubernetesManagerClient kubernetesManagerClient,
                                    final GatewayClient gatewayClient, final GraphQLConverter graphQLConverter,
                                    final GatewayResourceLoader loader) {
        this.kubernetesManagerClient = kubernetesManagerClient;
        this.gatewayClient = gatewayClient;
        this.graphQLConverter = graphQLConverter;
        this.loader = loader;
    }

    @Override
    public Single<List<GatewayDescription>> getGatewayList(final String requestId) {
        return this.kubernetesManagerClient.listDeployments(ResourcePrefix.GATEWAY.getPrefix())
            .map(deployments ->
                deployments.stream()
                    .map(GatewayResources::getGatewayDescription)
                    .collect(Collectors.toList())
            );
    }

    @Override
    public Single<GatewayDescription> getGateway(final String name, final String requestId) {
        final String deploymentName = GatewayResources.getResourceName(name);
        return this.kubernetesManagerClient.readDeployment(deploymentName)
            .map(GatewayResources::getGatewayDescription);
    }

    @Override
    public Completable createGateway(final GatewayCreationData gatewayCreationData, final String requestId) {
        log.info("Creating the gateway {}", gatewayCreationData.getName());
        return Single.fromCallable(() -> this.loader.forCreation(gatewayCreationData, ResourcePrefix.GATEWAY))
            .flatMapCompletable(this.kubernetesManagerClient::deploy);
    }

    @Override
    public Completable deleteGateway(final String name, final String requestId) {
        log.info("Deleting the gateway {}", name);
        return Single.fromCallable(() -> this.loader.forDeletion(name))
            .flatMapCompletable(this.kubernetesManagerClient::delete);
    }

    @Override
    public Completable updateSchema(final String name, final String graphQLSchema, final String requestId) {
        log.info("Updating schema of the gateway {} with {}", name, graphQLSchema);
        // Check if the gateway exists or not
        final Completable resourceExists =
            this.kubernetesManagerClient.checkDeploymentExistence(ResourcePrefix.GATEWAY, name);

        // applies the definition to gateway but not persisting it yet
        final Completable updateSchema = this.gatewayClient.updateSchema(name, new SchemaData(graphQLSchema));

        // persisting the applied definition to the ConfigMap and update the data of the ConfigMap
        final Completable updateConfigMap =
            this.kubernetesManagerClient.updateConfigMap(GatewayResources.getConfigMapName(name),
                Map.of("schema.graphql", graphQLSchema));

        return resourceExists.andThen(updateSchema)
            .andThen(updateConfigMap)
            .onErrorResumeNext(ex -> GatewayResources.handleDefinitionCreationError(ex, name));
    }

    @Override
    public Single<SchemaData> getGatewayWriteSchema(final String name, final String type, final SchemaFormat format,
        final String requestId) {
        final GatewaySchema gatewaySchema = new GatewaySchema(name, type);

        // make sure the gateway exists
        final Completable exists = this.kubernetesManagerClient.checkDeploymentExistence(ResourcePrefix.GATEWAY, name);

        final Action logAccess =
            () -> log.debug("Retrieve schema in '{}' gateway for type '{}' in '{}'", name, type, format.getName());

        final Single<SchemaData> fetchSchema =
            this.gatewayClient.getWriteSchema(gatewaySchema.getGateway(), gatewaySchema.getType())
                .map(response -> {
                    String schema = response.getSchema();
                    if (format == SchemaFormat.AVRO) {
                        schema = this.graphQLConverter.convert(schema).toString();
                    }
                    // TODO: Add support for protobuf
                    return new SchemaData(schema);
                });

        return exists.doOnComplete(logAccess).andThen(fetchSchema);
    }
}
