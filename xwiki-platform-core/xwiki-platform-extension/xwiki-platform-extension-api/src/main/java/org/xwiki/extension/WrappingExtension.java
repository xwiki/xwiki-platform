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
package org.xwiki.extension;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.xwiki.extension.repository.ExtensionRepository;

public class WrappingExtension implements Extension
{
    private Extension extension;

    public WrappingExtension(Extension extension)
    {
        this.extension = extension;
    }

    public Extension getExtension()
    {
        return this.extension;
    }

    // Extension

    public ExtensionId getId()
    {
        return getExtension().getId();
    }

    public String getType()
    {
        return getExtension().getType();
    }

    public String getName()
    {
        return getExtension().getName();
    }

    public String getDescription()
    {
        return getExtension().getDescription();
    }

    public String getWebSite()
    {
        return getExtension().getWebSite();
    }

    public List<String> getAuthors()
    {
        return getExtension().getAuthors();
    }

    public List< ? extends ExtensionDependency> getDependencies()
    {
        return getExtension().getDependencies();
    }

    public void download(File file) throws ExtensionException
    {
        getExtension().download(file);
    }

    public ExtensionRepository getRepository()
    {
        return getExtension().getRepository();
    }

    public Map<String, Object> getProperties()
    {
        return getExtension().getProperties();
    }

    public Object getProperty(String key)
    {
        return getExtension().getProperty(key);
    }
}
