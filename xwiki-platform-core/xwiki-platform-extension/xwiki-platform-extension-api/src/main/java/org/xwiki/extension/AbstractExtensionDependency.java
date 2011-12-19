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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.xwiki.extension.version.VersionConstraint;

/**
 * Base class for {@link ExtensionDependency} implementations.
 * 
 * @version $Id$
 */
public abstract class AbstractExtensionDependency implements ExtensionDependency
{
    /**
     * @see #getId()
     */
    protected String id;

    /**
     * @see #getVersionConstraint()
     */
    protected VersionConstraint versionConstraint;

    /**
     * @see #getProperties()
     */
    protected Map<String, Object> properties = new HashMap<String, Object>();

    /**
     * Create new instance by cloning the provided one with different version constraint.
     * 
     * @param dependency the extension dependency to copy
     * @param versionConstraint the version constraint to set
     */
    public AbstractExtensionDependency(ExtensionDependency dependency, VersionConstraint versionConstraint)
    {
        this(dependency.getId(), versionConstraint, dependency.getProperties());
    }

    /**
     * @param id the id (or feature) of the extension dependency
     * @param versionConstraint the version constraint of the extension dependency
     */
    public AbstractExtensionDependency(String id, VersionConstraint versionConstraint)
    {
        this(id, versionConstraint, null);
    }

    /**
     * @param id the id (or feature) of the extension dependency
     * @param versionConstraint the version constraint of the extension dependency
     * @param properties the custom properties of the extension dependency
     */
    public AbstractExtensionDependency(String id, VersionConstraint versionConstraint, Map<String, Object> properties)
    {
        this.id = id;
        this.versionConstraint = versionConstraint;
        if (properties != null) {
            this.properties.putAll(properties);
        }
    }

    @Override
    public String getId()
    {
        return this.id;
    }

    /**
     * @param id the extension id
     * @see #getId()
     */
    public void setId(String id)
    {
        this.id = id;
    }

    @Override
    public VersionConstraint getVersionConstraint()
    {
        return this.versionConstraint;
    }

    /**
     * @param versionConstraint the version constraint of the target extension
     */
    public void setVersionConstraint(VersionConstraint versionConstraint)
    {
        this.versionConstraint = versionConstraint;
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
    protected void putProperty(String key, Object value)
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
        return getId() + '-' + getVersionConstraint();
    }
}
