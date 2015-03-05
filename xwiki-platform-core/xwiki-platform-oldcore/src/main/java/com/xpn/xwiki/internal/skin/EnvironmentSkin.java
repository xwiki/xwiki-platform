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

import java.net.URI;
import java.net.URL;

import javax.inject.Provider;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.environment.Environment;
import org.xwiki.skin.Resource;
import org.xwiki.skin.Skin;

import com.xpn.xwiki.XWikiContext;

/**
 * @version $Id$
 * @since 6.4M1
 */
public class EnvironmentSkin extends AbstractSkin
{
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentSkin.class);

    private Environment environment;

    private Configuration properties;

    private Provider<XWikiContext> xcontextProvider;

    public EnvironmentSkin(String id, InternalSkinManager skinManager, InternalSkinConfiguration configuration,
        Environment environment, Provider<XWikiContext> xcontextProvider)
    {
        super(id, skinManager, configuration);

        this.environment = environment;
        this.xcontextProvider = xcontextProvider;
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
                    this.properties = new PropertiesConfiguration(url);
                } catch (ConfigurationException e) {
                    LOGGER.debug("Failed to load skin [{}] properties file", this.id,
                        ExceptionUtils.getRootCauseMessage(e));
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
        String resourcePath = getResourcePath(resourceName, false);

        if (this.environment.getResource(resourcePath) != null) {
            return createResource(resourcePath, resourceName);
        }

        return null;
    }

    protected AbstractEnvironmentResource createResource(String resourcePath, String resourceName)
    {
        return new SkinEnvironmentResource(resourcePath, resourceName, this, this.environment, this.xcontextProvider);
    }

    private String getResourcePath(String resource, boolean testExist)
    {
        String skinFolder = getSkinFolder();
        String resourcePath = getSkinFolder() + resource;

        // Prevent inclusion of templates from other directories
        String normalizedResource = URI.create(resourcePath).normalize().toString();
        if (!normalizedResource.startsWith(skinFolder)) {
            LOGGER.warn("Direct access to template file [{}] refused. Possible break-in attempt!", normalizedResource);

            return null;
        }

        if (testExist) {
            // Check if the resource exist
            if (this.environment.getResource(resourcePath) == null) {
                return null;
            }
        }

        return resourcePath;
    }
}
