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
import java.time.Instant;

import javax.inject.Provider;

import org.xwiki.skin.ResourceRepository;
import org.xwiki.url.URLConfiguration;

import com.xpn.xwiki.XWikiContext;

/**
 * Skin resource for the {@link ClassLoaderSkin}, where the skin's resources are resolved using the class loader.
 *
 * @version $Id$
 * @since 13.8RC1
 */
public class ClassLoaderSkinResource extends AbstractSkinResource
{
    private final ClassLoader classLoader;

    /**
     * Default constructor.
     *
     * @param path the path of the resource in the skin (for instance, {@code "/templates/display.vm"})
     * @param resourceName the name of the resource (for instance, {@code "display.vm"})
     * @param repository the resource repository, used to access the respository's id
     * @param classloader the classloader used to resolve the resources
     * @param xcontextProvider the context provider, used to access the context when resolving the resources URLs
     * @param urlConfiguration the url configuration, used when resolving the resources URLs
     */
    public ClassLoaderSkinResource(String path, String resourceName, ResourceRepository repository,
        ClassLoader classloader, Provider<XWikiContext> xcontextProvider, URLConfiguration urlConfiguration)
    {
        super(createId(classloader, path), path, resourceName, repository, null, xcontextProvider, urlConfiguration);

        this.classLoader = classloader;
    }

    private static String createId(ClassLoader classloader, String path)
    {
        return "classloader:" + classloader.getName() + ':' + path;
    }

    @Override
    public Instant getInstant() throws Exception
    {
        // TODO: return the date of the package file holding the resource
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String path)
    {
        return this.classLoader.getResourceAsStream(path);
    }
}
