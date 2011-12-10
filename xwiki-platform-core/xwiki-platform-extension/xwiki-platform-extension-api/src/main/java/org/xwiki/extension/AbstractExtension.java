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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * @see #getFeatures()
     */
    protected Set<String> features = new HashSet<String>();

    /**
     * @see #getType()
     */
    protected String type;

    /**
     * @see #getName()
     */
    protected String name;

    /**
     * @see #getLicenses()
     */
    protected List<ExtensionLicense> licenses = new ArrayList<ExtensionLicense>();

    /**
     * @see #getSummary()
     */
    protected String summary;

    /**
     * @see #getDescription()
     */
    protected String description;

    /**
     * @see #getAuthors()
     */
    protected List<ExtensionAuthor> authors = new ArrayList<ExtensionAuthor>();

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
     * The file of the extension.
     */
    protected ExtensionFile file;

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

        setFeatures(extension.getFeatures());

        setName(extension.getName());
        setDescription(extension.getDescription());
        setAuthors(extension.getAuthors());
        setWebsite(extension.getWebSite());
        if (extension.getLicenses() != null && !extension.getLicenses().isEmpty()) {
            setLicenses(extension.getLicenses());
        }
        setSummary(extension.getSummary());

        List< ? extends ExtensionDependency> newDependencies = extension.getDependencies();
        if (!newDependencies.isEmpty()) {
            this.dependencies = new ArrayList<ExtensionDependency>(extension.getDependencies());
        }
    }

    @Override
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

    @Override
    public Collection<String> getFeatures()
    {
        return this.features;
    }

    /**
     * @param features the extension ids also provided by this extension
     */
    public void setFeatures(Collection<String> features)
    {
        this.features = new LinkedHashSet<String>(features);
    }

    /**
     * Add a new feature to the extension.
     * 
     * @param feature a feature name
     */
    public void addFeature(String feature)
    {
        this.features.add(feature);
    }

    @Override
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

    @Override
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

    @Override
    public Collection<ExtensionLicense> getLicenses()
    {
        return this.licenses;
    }

    /**
     * @param licenses the licenses of the extension
     */
    public void setLicenses(Collection<ExtensionLicense> licenses)
    {
        this.licenses = new ArrayList<ExtensionLicense>(licenses);
    }

    /**
     * Add a new license to the extension.
     * 
     * @param license a license
     */
    public void addLicense(ExtensionLicense license)
    {
        this.licenses.add(license);
    }

    @Override
    public String getSummary()
    {
        return this.summary;
    }

    /**
     * @param summary a short description of the extension
     */
    public void setSummary(String summary)
    {
        this.summary = summary;
    }

    @Override
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

    @Override
    public List<ExtensionAuthor> getAuthors()
    {
        return this.authors;
    }

    /**
     * @param authors the authors of the extension
     */
    public void setAuthors(Collection<ExtensionAuthor> authors)
    {
        this.authors = new ArrayList<ExtensionAuthor>(authors);
    }

    /**
     * Add a new author to the extension.
     * 
     * @param author an author
     */
    public void addAuthor(ExtensionAuthor author)
    {
        this.authors.add(author);
    }

    @Override
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

    @Override
    public List< ? extends ExtensionDependency> getDependencies()
    {
        return this.dependencies != null ? Collections.unmodifiableList(this.dependencies) : Collections
            .<ExtensionDependency> emptyList();
    }

    /**
     * @param dependencies the dependencies of the extension
     * @see #getDependencies()
     */
    public void setDependencies(Collection< ? extends ExtensionDependency> dependencies)
    {
        this.dependencies = new ArrayList<ExtensionDependency>(dependencies);
    }

    @Override
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

    @Override
    public ExtensionFile getFile()
    {
        return this.file;
    }

    /**
     * @param file the file of the extension
     */
    protected void setFile(ExtensionFile file)
    {
        this.file = file;
    }

    @Override
    public Map<String, Object> getProperties()
    {
        return Collections.unmodifiableMap(this.properties);
    }

    @Override
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
