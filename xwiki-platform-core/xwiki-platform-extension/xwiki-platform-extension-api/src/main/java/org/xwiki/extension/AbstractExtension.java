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

/**
 * Base class for {@link Extension} implementations.
 * 
 * @version $Id$
 */
public abstract class AbstractExtension implements Extension
{
    /**
     * @see #getId()
     */
    protected ExtensionId id;

    /**
     * @see #getType()
     */
    protected String type;

    /**
     * @see #getName()
     */
    protected String name;

    /**
     * @see #getDescription()
     */
    protected String description;

    /**
     * @see #getAuthors()
     */
    protected List<String> authors = new ArrayList<String>();

    /**
     * @see #getWebSite()
     */
    protected String website;

    /**
     * @see #getRepository()
     */
    protected ExtensionRepository repository;

    /**
     * @see #getProperties()
     */
    protected Map<String, Object> properties = new HashMap<String, Object>();

    /**
     * @see #getDependencies()
     */
    protected List<ExtensionDependency> dependencies;
    
    /**
     * @param repository the repository where this extension comes from
     * @param id the extension identifier
     * @param type the extension type
     */
    public AbstractExtension(ExtensionRepository repository, ExtensionId id, String type)
    {
        this.repository = repository;

        this.id = id;
        this.type = type;
    }

    /**
     * Create new extension descriptor by copying provided one.
     * 
     * @param repository the repository where this extension comes from
     * @param extension the extension to copy
     */
    public AbstractExtension(ExtensionRepository repository, Extension extension)
    {
        this(repository, extension.getId(), extension.getType());

        setDescription(extension.getDescription());
        setAuthors(extension.getAuthors());
        setWebsite(extension.getWebSite());

        List< ? extends ExtensionDependency> newDependencies = extension.getDependencies();
        if (!newDependencies.isEmpty()) {
            this.dependencies = new ArrayList<ExtensionDependency>(extension.getDependencies());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.Extension#getId()
     */
    public ExtensionId getId()
    {
        return this.id;
    }

    /**
     * @param id the extension id
     * @see #getId()
     */
    protected void setId(ExtensionId id)
    {
        this.id = id;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.Extension#getType()
     */
    public String getType()
    {
        return this.type;
    }

    /**
     * @param type the type of the extension
     * @see #getType()
     */
    protected void setType(String type)
    {
        this.type = type;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.Extension#getName()
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @param name the display name of the extension
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.Extension#getDescription()
     */
    public String getDescription()
    {
        return this.description;
    }

    /**
     * @param description a description of the extension
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.Extension#getAuthors()
     */
    public List<String> getAuthors()
    {
        return this.authors;
    }

    /**
     * @param authors the extension authors
     */
    public void setAuthors(List<String> authors)
    {
        this.authors = new ArrayList<String>(authors);
    }

    /**
     * Add a new author to the extension.
     * 
     * @param author an author name
     */
    public void addAuthor(String author)
    {
        this.authors.add(author);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.Extension#getWebSite()
     */
    public String getWebSite()
    {
        return this.website;
    }

    /**
     * @param website an URL for the extension website
     */
    public void setWebsite(String website)
    {
        this.website = website;
    }

    /**
     * Add a new dependency to the extension.
     * 
     * @param dependency a dependency
     */
    public void addDependency(ExtensionDependency dependency)
    {
        if (this.dependencies == null) {
            this.dependencies = new ArrayList<ExtensionDependency>();
        }

        this.dependencies.add(dependency);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.Extension#getDependencies()
     */
    public List< ? extends ExtensionDependency> getDependencies()
    {
        return this.dependencies != null ? Collections.unmodifiableList(this.dependencies) : Collections
            .<ExtensionDependency> emptyList();
    }

    /**
     * @param dependencies the dependencies of the extension
     * @see #getDependencies()
     */
    public void setDependencies(List< ? extends ExtensionDependency> dependencies)
    {
        this.dependencies = new ArrayList<ExtensionDependency>(dependencies);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.Extension#getRepository()
     */
    public ExtensionRepository getRepository()
    {
        return this.repository;
    }

    /**
     * @param repository the repository of the extension
     * @see #getRepository()
     */
    protected void setRepository(ExtensionRepository repository)
    {
        this.repository = repository;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.Extension#getProperties()
     */
    public Map<String, Object> getProperties()
    {
        return Collections.unmodifiableMap(this.properties);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.Extension#getProperty(java.lang.String)
     */
    public Object getProperty(String key)
    {
        return this.properties.get(key);
    }

    /**
     * Set a property.
     * 
     * @param key the property key
     * @param value the property value
     * @see #getProperty(String)
     */
    public void putProperty(String key, Object value)
    {
        this.properties.put(key, value);
    }

    /**
     * Get a property.
     * 
     * @param <T> type of the property value
     * @param key the property key
     * @param def the value to return if no property is associated to the provided key
     * @return the property value or <code>default</code> of the property is not found
     * @see #getProperty(String)
     */
    public <T> T getProperty(String key, T def)
    {
        return this.properties.containsKey(key) ? (T) this.properties.get(key) : def;
    }

    // Object

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return getId().toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        return this == obj || (obj instanceof Extension && getId().equals(((Extension) obj).getId()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        return getId().hashCode();
    }
}
