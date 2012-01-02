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
import org.xwiki.extension.version.InvalidVersionRangeException;
import org.xwiki.extension.version.internal.DefaultVersionConstraint;

public class AetherExtensionDependency extends AbstractExtensionDependency
{
    private Dependency aetherDependency;

    public AetherExtensionDependency(Dependency aetherDependency) throws InvalidVersionRangeException
    {
        super(AetherUtils.createExtensionId(aetherDependency.getArtifact()).getId(), new DefaultVersionConstraint(
            aetherDependency.getArtifact().getVersion()));

        this.aetherDependency = aetherDependency;
    }

    public Dependency getAetherDependency()
    {
        return this.aetherDependency;
    }
}
