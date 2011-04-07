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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.xwiki.extension.repository.ExtensionRepository;

public abstract class AbstractExtension implements Extension
{
    private ExtensionId id;

    private String type;

    private String description;

    private String author;

    private String website;

    private List<ExtensionDependency> dependencies = new ArrayList<ExtensionDependency>();

    private ExtensionRepository repository;

    public AbstractExtension(ExtensionRepository repository, ExtensionId id, String type)
    {
        this.repository = repository;

        this.id = id;
        this.type = type;
    }

    public AbstractExtension(ExtensionRepository repository, Extension extension)
    {
        this(repository, extension.getId(), extension.getType());

        setDescription(extension.getDescription());
        setAuthor(extension.getAuthor());
        setWebsite(extension.getWebSite());
        
        this.dependencies.addAll(extension.getDependencies());
    }

    // Extension

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setAuthor(String author)
    {
        this.author = author;
    }

    public void setWebsite(String website)
    {
        this.website = website;
    }

    public ExtensionId getId()
    {
        return this.id;
    }
    
    protected void setId(ExtensionId id)
    {
        this.id = id;
    }

    public String getType()
    {
        return this.type;
    }

    public String getDescription()
    {
        return this.description;
    }

    public String getAuthor()
    {
        return this.author;
    }

    public String getWebSite()
    {
        return this.website;
    }

    public void addDependency(ExtensionDependency dependency)
    {
        this.dependencies.add(dependency);
    }

    public List<ExtensionDependency> getDependencies()
    {
        return Collections.unmodifiableList(this.dependencies);
    }

    public ExtensionRepository getRepository()
    {
        return this.repository;
    }

    // Object

    @Override
    public String toString()
    {
        return getId().toString();
    }

    @Override
    public boolean equals(Object obj)
    {
        return this == obj || (obj instanceof Extension && getId().equals(((Extension) obj).getId()));
    }

    @Override
    public int hashCode()
    {
        return getId().hashCode();
    }
}
