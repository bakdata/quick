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

package com.bakdata.quick.manager.config;

import io.micronaut.context.annotation.ConfigurationInject;
import io.micronaut.context.annotation.ConfigurationProperties;
import java.util.Optional;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Configuration regarding deployments executed by the manager.
 */
@ConfigurationProperties("quick")
@Getter
public class DeploymentConfig {
    private final String dockerRegistry;
    /**
     * If not manually set in a config, this uses the environment variable we inject during the image build and is
     * therefore the same as the manager's tag.
     */
    private final String defaultImageTag;
    private final int defaultReplicas;
    private final Optional<String> ingressHost;
    private final boolean ingressSsl;
    private final String ingressEntrypoint;

    /**
     * Default constructor.
     *
     * @param dockerRegistry    name of the docker registry where our image are
     * @param defaultImageTag   the default image tag
     * @param defaultReplicas   number of replicas to deploy by default
     * @param ingressHost       the host for this deployment. E.g. `dev` for dev.d9p.io
     * @param ingressSsl        whether the ingress should use ingress
     * @param ingressEntrypoint entry point for Traefik
     */
    @ConfigurationInject
    public DeploymentConfig(final String dockerRegistry, final String defaultImageTag, final int defaultReplicas,
                            final Optional<String> ingressHost, final boolean ingressSsl,
                            final String ingressEntrypoint) {
        this.dockerRegistry = dockerRegistry;
        this.defaultImageTag = defaultImageTag;
        this.defaultReplicas = defaultReplicas;
        this.ingressHost = ingressHost;
        this.ingressSsl = ingressSsl;
        this.ingressEntrypoint = ingressEntrypoint;
    }
}
