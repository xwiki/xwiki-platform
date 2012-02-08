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
package org.xwiki.extension.repository.internal.core;

import org.apache.maven.model.Dependency;
import org.xwiki.extension.DefaultExtensionDependency;
import org.xwiki.extension.version.VersionConstraint;

/**
 * Extends {@link DefaultExtensionDependency} with Maven related informations.
 * 
 * @version $Id$
 */
public class MavenCoreExtensionDependency extends DefaultExtensionDependency
{
    /**
     * The key associated to the Maven dependency object.
     */
    public static final String PKEY_MAVEN_DEPENDENCY = "maven.Dependency";

    /**
     * @param extensionId the id of the extension dependency
     * @param constraint the version constraint of the extension dependency
     * @param mavenDependency the Maven dependency object
     */
    public MavenCoreExtensionDependency(String extensionId, VersionConstraint constraint, Dependency mavenDependency)
    {
        super(extensionId, constraint);

        // custom properties
        putProperty(PKEY_MAVEN_DEPENDENCY, mavenDependency);
    }

    /**
     * @return the Maven dependency object
     */
    public Dependency getMavenDependency()
    {
        return (Dependency) getProperty(PKEY_MAVEN_DEPENDENCY);
    }
}
