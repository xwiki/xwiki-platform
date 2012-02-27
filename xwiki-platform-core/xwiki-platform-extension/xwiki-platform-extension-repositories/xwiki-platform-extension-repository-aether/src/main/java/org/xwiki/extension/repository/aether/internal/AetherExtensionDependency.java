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
package org.xwiki.extension.repository.aether.internal;

import org.sonatype.aether.graph.Dependency;
import org.xwiki.extension.AbstractExtensionDependency;
import org.xwiki.extension.version.internal.DefaultVersionConstraint;

public class AetherExtensionDependency extends AbstractExtensionDependency
{
    public static final String PKEY_AETHER_DEPENDENCY = "aether.Dependency";

    public static final String PKEY_MAVEN_DEPENDENCY = "maven.Dependency";

    public AetherExtensionDependency(Dependency aetherDependency, org.apache.maven.model.Dependency mavenDependency)
    {
        super(AetherUtils.createExtensionId(aetherDependency.getArtifact()).getId(), new DefaultVersionConstraint(
            aetherDependency.getArtifact().getVersion()));

        // custom properties
        putProperty(PKEY_AETHER_DEPENDENCY, aetherDependency);
        putProperty(PKEY_MAVEN_DEPENDENCY, mavenDependency);
    }

    public Dependency getAetherDependency()
    {
        return (Dependency) this.getProperty(PKEY_AETHER_DEPENDENCY);
    }

    /**
     * @return the Maven dependency object
     */
    public Dependency getMavenDependency()
    {
        return (Dependency) getProperty(PKEY_MAVEN_DEPENDENCY);
    }
}
