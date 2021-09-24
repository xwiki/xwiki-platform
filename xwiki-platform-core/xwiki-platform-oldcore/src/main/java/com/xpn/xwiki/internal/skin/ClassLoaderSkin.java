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
import org.xwiki.url.URLConfiguration;

import com.xpn.xwiki.XWikiContext;

/**
 * Represents a skin than can be access using the classloader (for instance, stored in a jar).
 *
 * @version $Id$
 * @since 13.8RC1
 */
public class ClassLoaderSkin extends AbstractResourceSkin
{
    private final ClassLoader classloader;

    private final Provider<XWikiContext> xcontextProvider;

    private final URLConfiguration urlConfiguration;

    /**
     * Default constructor.
     *
     * @param id the skin id (for instance, {@code "flamingo"})
     * @param skinManager the skin manager that instantiates this skin
     * @param configuration the skin internal configuration, used to access the default parent skin id
     * @param logger a logger used to log warning in case of error when parsing a skin's syntax
     * @param xcontextProvider a wiki context provide, used to give access to the context when resolving the skin's
     *     rsources.
     * @param urlConfiguration the url configuration used to resolve the url of the skin's resources
     * @param classLoader the class loader used to lookup the skin
     */
    public ClassLoaderSkin(String id, InternalSkinManager skinManager, InternalSkinConfiguration configuration,
        Logger logger, Provider<XWikiContext> xcontextProvider,
        URLConfiguration urlConfiguration, ClassLoader classLoader)
    {
        super(id, skinManager, configuration, logger);

        this.classloader = classLoader;
        this.xcontextProvider = xcontextProvider;
        this.urlConfiguration = urlConfiguration;
    }

    @Override
    protected URL getResourceURL(String resourcePath)
    {
        return this.classloader.getResource(resourcePath);
    }

    @Override
    protected ClassLoaderSkinResource createResource(String resourcePath, String resourceName)
    {
        return new ClassLoaderSkinResource(resourcePath, resourceName, this, this.classloader, this.xcontextProvider,
            this.urlConfiguration);
    }
}
