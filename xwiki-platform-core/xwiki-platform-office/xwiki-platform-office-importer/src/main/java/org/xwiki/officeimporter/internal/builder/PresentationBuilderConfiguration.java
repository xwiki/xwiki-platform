/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.officeimporter.internal.builder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Configuration for the presentation builder.
 *
 * @version $Id$
 * @since 16.8.0
 * @since 16.4.4
 * @since 15.10.13
 */
@Component(roles = PresentationBuilderConfiguration.class)
@Singleton
public class PresentationBuilderConfiguration implements Initializable
{
    private static final String DEFAULT_IMAGE_FORMAT = "jpg";

    private int slideWidth = 1920;

    private float quality = 95;

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configurationSource;

    @Inject
    private Logger logger;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            // For backward compatibility reason we check both custom-document-formats and document-formats.
            if (!extractConfigurationFromJsonRegistry("/custom-document-formats.json")) {
                extractConfigurationFromJsonRegistry("/document-formats.js");
            }
        } catch (Exception e) {
            this.logger.error("Error when initializing values from document format registry, "
                + "default values will be used.", e);
        }
    }

    private boolean extractConfigurationFromJsonRegistry(String filename) throws IOException
    {
        AtomicBoolean result = new AtomicBoolean(false);
        try (InputStream configurationInput = getClass().getResourceAsStream(filename)) {
            if (configurationInput != null) {
                // Load the configuration from the JSON file
                ObjectMapper objectMapper = new ObjectMapper();
                // Read the JSON which should be an array of JSON objects
                // Each JSON object should have the following properties: name (string), storeProperties (object with
                // key "PRESENTATION" that again has a key FilterData with keys Quality and Width that are integers).
                JsonNode jsonNode = objectMapper.readTree(configurationInput);
                for (JsonNode formatNode : jsonNode) {
                    getJsonNode(formatNode, "name")
                        .filter(nameNode -> "HTML".equals(nameNode.asText()))
                        .flatMap(nameNode -> getJsonNode(formatNode, "storeProperties"))
                        .flatMap(storeProperties -> getJsonNode(storeProperties, "PRESENTATION"))
                        .flatMap(presentationProperties -> getJsonNode(presentationProperties, "FilterData"))
                        .ifPresent(filterData -> {
                            result.set(true);
                            getJsonNode(filterData, "Quality")
                                .ifPresent(qualityNode -> this.quality = qualityNode.asInt());
                            getJsonNode(filterData, "Width")
                                .ifPresent(widthNode -> this.slideWidth = widthNode.asInt());
                        });
                }
            }
        }
        return result.get();
    }

    private Optional<JsonNode> getJsonNode(JsonNode jsonNode, String fieldName)
    {
        return Optional.ofNullable(jsonNode.get(fieldName));
    }

    /**
     * @return the width of the images to generate for slides in pixels
     */
    public int getSlideWidth()
    {
        return this.configurationSource.getProperty("officeimporter.presentation.slideWidth", this.slideWidth);
    }

    /**
     * @return the image quality to use when converting slides to images
     */
    public float getQuality()
    {
        if ("png".equals(this.getImageFormat())) {
            return 0f;
        }

        return this.configurationSource.getProperty("officeimporter.presentation.quality", this.quality) / 100f;
    }

    /**
     * @return the image format to use when converting slides to images.
     */
    public String getImageFormat()
    {
        return this.configurationSource.getProperty("officeimporter.presentation.imageFormat", DEFAULT_IMAGE_FORMAT);
    }
}
