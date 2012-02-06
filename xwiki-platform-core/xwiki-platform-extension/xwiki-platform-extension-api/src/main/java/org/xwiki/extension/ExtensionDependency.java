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

import java.util.Map;

import org.xwiki.extension.version.VersionConstraint;

/**
 * An extension dependency.
 * 
 * @version $Id$
 */
public interface ExtensionDependency
{
    /**
     * @return the id (or feature) of the target extension
     */
    String getId();

    /**
     * @return the version constraint of the target extension
     */
    VersionConstraint getVersionConstraint();

    /**
     * Extends {@link ExtensionDependency} standard properties.
     * <p>
     * Theses are generally provided by specific repositories. For example a AETHER repository will provide AETHER
     * Dependency representation to avoid conversion when searching for the dependency on a AETHER based repository.
     * 
     * @return the properties
     */
    Map<String, Object> getProperties();

    /**
     * @param key the property key
     * @return the property value
     */
    Object getProperty(String key);
}
