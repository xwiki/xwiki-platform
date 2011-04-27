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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.extension.repository.ExtensionRepository;

public abstract class AbstractExtension implements Extension
{
    private ExtensionId id;

    private String type;

    private String name;

    private String description;

    private String author;

    private String website;

    private ExtensionRepository repository;

    private Map<String, Object> properties = new HashMap<String, Object>();

    protected List<ExtensionDependency> dependencies;

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

        List<ExtensionDependency> dependencies = extension.getDependencies();
        if (!dependencies.isEmpty()) {
            this.dependencies = new ArrayList<ExtensionDependency>(extension.getDependencies());
        }
    }

    protected void setId(ExtensionId id)
    {
        this.id = id;
    }

    protected void setRepository(ExtensionRepository repository)
    {
        this.repository = repository;
    }

    protected void setType(String type)
    {
        this.type = type;
    }

    // Extension

    public ExtensionId getId()
    {
        return this.id;
    }

    public String getType()
    {
        return this.type;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return this.description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getAuthor()
    {
        return this.author;
    }

    public void setAuthor(String author)
    {
        this.author = author;
    }

    public String getWebSite()
    {
        return this.website;
    }

    public void setWebsite(String website)
    {
        this.website = website;
    }

    public void addDependency(ExtensionDependency dependency)
    {
        if (this.dependencies == null) {
            this.dependencies = new ArrayList<ExtensionDependency>();
        }

        this.dependencies.add(dependency);
    }

    public List<ExtensionDependency> getDependencies()
    {
        return this.dependencies != null ? Collections.unmodifiableList(this.dependencies) : Collections
            .<ExtensionDependency> emptyList();
    }

    public ExtensionRepository getRepository()
    {
        return this.repository;
    }

    protected void putProperty(String key, Object value)
    {
        this.properties.put(key, value);
    }

    public Map<String, Object> getProperties()
    {
        return Collections.unmodifiableMap(this.properties);
    }

    public Object getProperty(String key)
    {
        return this.properties.get(key);
    }

    public <T> T getProperty(String key, T def)
    {
        return this.properties.containsKey(key) ? (T) this.properties.get(key) : def;
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
