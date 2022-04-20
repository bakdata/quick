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

import lombok.Value;

/**
 * Holds information about a specific image.
 */
@Value(staticConstructor = "of")
public class ImageConfig {
    String image;
    int replicas;
    String version;

    public static ImageConfig of(final String dockerRegistry, final String image, final int replicas,
                                 final String version) {
        return new ImageConfig(getImage(dockerRegistry, image), replicas, version);
    }

    /**
     * This function extracts the tag from them imageString.
     *
     * <p>
     * For example consider the imageString of <i>registry.hub.docker.com/quick-gateway:example-tag</i>
     * The returned value would be <i>example-tag</i>.
     *
     * @param imageString Contains the docker registry, image name, and tag.
     * @return The tag of an image
     */
    public static String getTagFromString(final String imageString) {
        return imageString.substring(imageString.lastIndexOf(':') + 1);
    }

    public String asImageString() {
        return String.format("%s:%s", this.image, this.version);
    }

    private static String getImage(final String dockerRegistry, final String type) {
        return String.format("%s/%s", dockerRegistry, type);
    }
}
