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

import org.apache.maven.model.Model;
import org.sonatype.aether.artifact.Artifact;
import org.xwiki.extension.AbstractExtension;
import org.xwiki.extension.repository.aether.internal.plexus.PlexusComponentManager;

public class AetherExtension extends AbstractExtension
{
    public static final String PKEY_AETHER_ATIFACT = "aether.Artifact";

    public static final String PKEY_MAVEN_MODEL = "maven.Model";

    public AetherExtension(Artifact artifact, Model mavenModel, AetherExtensionRepository repository,
        PlexusComponentManager plexusComponentManager)
    {
        super(repository, AetherUtils.createExtensionId(artifact), artifact.getExtension());

        setFile(new AetherExtensionFile(artifact, repository, plexusComponentManager, getType()));

        // custom properties
        putProperty(PKEY_AETHER_ATIFACT, artifact);
        putProperty(PKEY_MAVEN_MODEL, mavenModel);
    }
}
