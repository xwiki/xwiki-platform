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
package com.xpn.xwiki.internal.skin;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.filter.input.InputSource;
import org.xwiki.skin.Resource;
import org.xwiki.skin.Skin;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Common abstract class for the skins that manipulate resources.
 *
 * @version $Id$
 * @since 13.8RC1
 */
public abstract class AbstractResourceSkin extends AbstractSkin
{
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractResourceSkin.class);

    private Configuration properties;

    /**
     * Default constructor.
     *
     * @param id the skin id (for instance, {@code "flamingo"})
     * @param skinManager the skin manager that instantiates this skin
     * @param configuration the skin internal configuration, used to access the default parent skin id
     * @param logger a logger used to log warning in case of error when parsin a skin's syntax
     */
    public AbstractResourceSkin(String id, InternalSkinManager skinManager,
        InternalSkinConfiguration configuration, Logger logger)
    {
        super(id, skinManager, configuration, logger);
    }

    abstract AbstractResource<InputSource> createResource(String resourcePath, String resourceName);

    abstract URL getResourceURL(String resourcePath);

    @Override
    public String getOutputSyntaxString()
    {
        return getProperties().getString("outputSyntax");
    }

    @Override
    protected Skin createParent()
    {
        Skin skin;

        String parentId = getProperties().getString("parent");

        if (parentId != null) {
            if (parentId.isEmpty()) {
                // There is explicitly no parent (make sure to not fallback on default parent skin)
                skin = VOID;
            } else {
                skin = this.skinManager.getSkin(parentId);
            }
        } else {
            skin = null;
        }

        return skin;
    }

    @Override
    public Resource<?> getLocalResource(String resourceName)
    {
        String resourcePath = getSkinResourcePath(resourceName);

        if (resourcePath != null && getResourceURL(resourcePath) != null) {
            return createResource(resourcePath, resourceName);
        }

        return null;
    }

    protected String getPropertiesPath()
    {
        return getSkinFolder() + "skin.properties";
    }

    protected String getSkinFolder()
    {
        return "skins/" + this.id + '/';
    }

    protected Configuration getProperties()
    {
        if (this.properties == null) {
            URL url = getResourceURL(getPropertiesPath());
            if (url != null) {
                try {
                    this.properties = new Configurations().properties(url);
                } catch (ConfigurationException e) {
                    LOGGER.error("Failed to load skin [{}] properties file ([])", this.id, url,
                        getRootCauseMessage(e));

                    this.properties = new BaseConfiguration();
                }
            } else {
                LOGGER.debug("No properties found for skin [{}]", this.id);

                this.properties = new BaseConfiguration();
            }
        }

        return this.properties;
    }

    private String getSkinResourcePath(String resource)
    {
        String skinFolder = getSkinFolder();
        String resourcePath = skinFolder + resource;

        // Prevent access to resources from other directories
        Path normalizedResource = Paths.get(resourcePath).normalize();
        // Protect against directory attacks.
        if (!normalizedResource.startsWith(skinFolder)) {
            LOGGER.warn("Direct access to skin file [{}] refused. Possible break-in attempt!", normalizedResource);
            return null;
        }

        return resourcePath;
    }
}
