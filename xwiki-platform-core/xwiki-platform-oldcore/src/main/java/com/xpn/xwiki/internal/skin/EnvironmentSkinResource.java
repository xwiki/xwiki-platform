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

import java.io.InputStream;

import javax.inject.Provider;

import org.xwiki.environment.Environment;
import org.xwiki.skin.ResourceRepository;
import org.xwiki.url.URLConfiguration;

import com.xpn.xwiki.XWikiContext;

/**
 * Skin resource for the {@link ClassLoaderSkin}, where the skin's resources are resolved using the environment.
 *
 * @version $Id$
 * @since 6.4M1
 */
public class EnvironmentSkinResource extends AbstractSkinResource
{
    /**
     * Default constructor.
     *
     * @param path the path of the resource in the skin (for instance, {@code "/templates/display.vm"})
     * @param resourceName the name of the resource (for instance, {@code "display.vm"})
     * @param repository the resource repository, used to access the respository's id
     * @param environment the environment used to resolve the resources
     * @param xcontextProvider the context provider, used to access the context when resolving the resources URLs
     * @param urlConfiguration the url configuration, used when resolving the resources URLs
     */
    public EnvironmentSkinResource(String path, String resourceName, ResourceRepository repository,
        Environment environment, Provider<XWikiContext> xcontextProvider, URLConfiguration urlConfiguration)
    {
        super(createId(path), path, resourceName, repository, environment, xcontextProvider, urlConfiguration);
    }

    public static String createId(String path)
    {
        return "environment:" + path;
    }

    @Override
    public InputStream getResourceAsStream(String path)
    {
        return this.environment.getResourceAsStream(path);
    }
}
