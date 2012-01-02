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
 * Default implementation of {@link ExtensionDependency}.
 * 
 * @version $Id$
 */
public class DefaultExtensionDependency extends AbstractExtensionDependency
{
    /**
     * @param id the id of the extension dependency
     * @param versionConstraint the version constraint of the extension dependency
     */
    public DefaultExtensionDependency(String id, VersionConstraint versionConstraint)
    {
        super(id, versionConstraint);
    }

    /**
     * @param id the id of the extension dependency
     * @param versionConstraint the version constraint of the extension dependency
     * @param properties the custom properties of the extension dependency
     */
    public DefaultExtensionDependency(String id, VersionConstraint versionConstraint, Map<String, Object> properties)
    {
        super(id, versionConstraint, properties);
    }

    /**
     * Create new instance by cloning the provided one with different version constraint.
     * 
     * @param dependency the extension dependency to copy
     * @param versionConstraint the version constraint to set
     */
    public DefaultExtensionDependency(ExtensionDependency dependency, VersionConstraint versionConstraint)
    {
        super(dependency, versionConstraint);
    }
}
