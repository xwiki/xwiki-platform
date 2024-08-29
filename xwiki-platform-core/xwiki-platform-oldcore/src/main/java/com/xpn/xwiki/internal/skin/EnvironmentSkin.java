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

import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.environment.Environment;
import org.xwiki.url.URLConfiguration;

import com.xpn.xwiki.XWikiContext;

/**
 * Represents a skin stored in the file system.
 *
 * @version $Id$
 * @since 6.4M1
 */
public class EnvironmentSkin extends AbstractResourceSkin
{
    private final Environment environment;

    private final Provider<XWikiContext> xcontextProvider;

    private final URLConfiguration urlConfiguration;

    /**
     * Default constructor.
     *
     * @param id the skin id (for instance, {@code "flamingo"})
     * @param skinManager the skin manager that instantiates this skin
     * @param configuration the skin internal configuration, used to access the default parent skin id
     * @param logger a logger used to log warning in case of error when parsing a skin's syntax
     * @param environment the wiki environment, this is where this skin load its resources from
     * @param xcontextProvider a wiki context provide, used to give access to the context when resolving the skin's
     *     rsources.
     * @param urlConfiguration the url configuration used to resolve the url of the skin's resources
     */
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
    protected URL getResourceURL(String propertiesPath)
    {
        return this.environment.getResource(propertiesPath);
    }

    @Override
    protected EnvironmentSkinResource createResource(String resourcePath, String resourceName)
    {
        return new EnvironmentSkinResource(resourcePath, resourceName, this, this.environment, this.xcontextProvider,
            this.urlConfiguration);
    }

    @Override
    protected String getSkinFolder()
    {
        return '/' + super.getSkinFolder();
    }

    /**
     * Check if the skin exists by checking if a directory exists and contains a {@code skin.properties} file.
     *
     * @return {@code true} if the skin exists, {@code false} otherwise
     * @since 13.8RC1
     */
    public boolean exists()
    {
        return getResourceURL(getPropertiesPath()) != null;
    }
}
