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

import javax.inject.Provider;

import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.environment.Environment;
import org.xwiki.skin.Resource;
import org.xwiki.skin.Skin;
import org.xwiki.url.URLConfiguration;

import com.xpn.xwiki.XWikiContext;

/**
 * Represents a skin stored in the file system.
 *
 * @version $Id$
 * @since 6.4M1
 */
public class EnvironmentSkin extends AbstractSkin
{
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentSkin.class);

    private Environment environment;

    private Configuration properties;

    private Provider<XWikiContext> xcontextProvider;

    private URLConfiguration urlConfiguration;

    public EnvironmentSkin(String id, InternalSkinManager skinManager, InternalSkinConfiguration configuration,
        Logger logger, Environment environment, Provider<XWikiContext> xcontextProvider,
        URLConfiguration urlConfiguration)
    {
        super(id, skinManager, configuration, logger);

        this.environment = environment;
        this.xcontextProvider = xcontextProvider;
        this.urlConfiguration = urlConfiguration;
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

    public Configuration getProperties()
    {
        if (this.properties == null) {
            URL url = this.environment.getResource(getSkinFolder() + "skin.properties");
            if (url != null) {
                try {
                    this.properties = new Configurations().properties(url);
                } catch (ConfigurationException e) {
                    LOGGER.error("Failed to load skin [{}] properties file ([])", this.id, url,
                        ExceptionUtils.getRootCauseMessage(e));

                    this.properties = new BaseConfiguration();
                }
            } else {
                LOGGER.debug("No properties found for skin [{}]", this.id);

                this.properties = new BaseConfiguration();
            }
        }

        return this.properties;
    }

    public String getSkinFolder()
    {
        return "/skins/" + this.id + '/';
    }

    @Override
    public Resource<?> getLocalResource(String resourceName)
    {
        String resourcePath = getSkinResourcePath(resourceName);

        if (this.environment.getResource(resourcePath) != null) {
            return createResource(resourcePath, resourceName);
        }

        return null;
    }

    protected AbstractEnvironmentResource createResource(String resourcePath, String resourceName)
    {
        return new SkinEnvironmentResource(resourcePath, resourceName, this, this.environment, this.xcontextProvider,
            this.urlConfiguration);
    }

    private String getSkinResourcePath(String resource)
    {
        String skinFolder = getSkinFolder();
        String resourcePath = skinFolder + resource;

        // Prevent inclusion of templates from other directories
        Path normalizedResource = Paths.get(resourcePath).normalize();
        if (!normalizedResource.startsWith(skinFolder)) {
            LOGGER.warn("Direct access to skin file [{}] refused. Possible break-in attempt!", normalizedResource);

            return null;
        }

        return resourcePath;
    }

    @Override
    public String getOutputSyntaxString()
    {
        return getProperties().getString("outputSyntax");
    }
}
