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
package org.xwiki.extension.wrap;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionLicense;
import org.xwiki.extension.repository.ExtensionRepository;

/**
 * Wrap an extension.
 * 
 * @param <T> the extension type
 * @version $Id$
 */
public class WrappingExtension<T extends Extension> implements Extension
{
    /**
     * @see #getExtension()
     */
    private T extension;

    /**
     * @param extension the wrapped extension
     */
    public WrappingExtension(T extension)
    {
        this.extension = extension;
    }

    /**
     * @return the wrapped extension
     */
    public T getExtension()
    {
        return this.extension;
    }

    // Extension

    @Override
    public ExtensionId getId()
    {
        return getExtension().getId();
    }

    @Override
    public Collection<String> getFeatures()
    {
        return getExtension().getFeatures();
    }

    @Override
    public String getType()
    {
        return getExtension().getType();
    }

    @Override
    public String getName()
    {
        return getExtension().getName();
    }

    @Override
    public Collection<ExtensionLicense> getLicenses()
    {
        return getExtension().getLicenses();
    }

    @Override
    public String getDescription()
    {
        return getExtension().getDescription();
    }

    @Override
    public String getWebSite()
    {
        return getExtension().getWebSite();
    }

    @Override
    public List<String> getAuthors()
    {
        return getExtension().getAuthors();
    }

    @Override
    public List< ? extends ExtensionDependency> getDependencies()
    {
        return getExtension().getDependencies();
    }

    @Override
    public void download(File file) throws ExtensionException
    {
        getExtension().download(file);
    }

    @Override
    public ExtensionRepository getRepository()
    {
        return getExtension().getRepository();
    }

    @Override
    public Map<String, Object> getProperties()
    {
        return getExtension().getProperties();
    }

    @Override
    public Object getProperty(String key)
    {
        return getExtension().getProperty(key);
    }
}
