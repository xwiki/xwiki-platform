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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.xwiki.extension.AbstractExtension;
import org.xwiki.extension.ExtensionException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionLicenseManager;
import org.xwiki.extension.repository.aether.internal.plexus.PlexusComponentManager;
import org.xwiki.properties.ConverterManager;

public class AetherExtension extends AbstractExtension
{
    public static final String PKEY_GROUPID = "aether.groupid";

    public static final String PKEY_ARTIFACTID = "aether.artifactid";

    public static final String MPKEYPREFIX = "xwiki.extension.";

    public static final String MPKEY_FEATURES = MPKEYPREFIX + "features";

    private PlexusComponentManager plexusComponentManager;

    private List<ExtensionId> suggested;

    private Model mavenModel;

    public AetherExtension(ExtensionId id, Model mavenModel, AetherExtensionRepository repository,
        PlexusComponentManager mavenComponentManager, ConverterManager converter, ExtensionLicenseManager licenseManager)
    {
        // See bundle as jar packages since bundle are actually store as jar files
        super(repository, id, mavenModel.getPackaging().equals("bundle") ? "jar" : mavenModel.getPackaging());

        this.plexusComponentManager = mavenComponentManager;
        this.mavenModel = mavenModel;

        // custom properties
        putProperty(PKEY_GROUPID, this.mavenModel.getGroupId());
        putProperty(PKEY_ARTIFACTID, this.mavenModel.getArtifactId());
    }

    @Override
    public void download(File file) throws ExtensionException
    {
        RepositorySystem repositorySystem;
        try {
            repositorySystem = this.plexusComponentManager.getPlexus().lookup(RepositorySystem.class);
        } catch (ComponentLookupException e) {
            throw new ExtensionException("Failed to get org.sonatype.aether.RepositorySystem component", e);
        }

        AetherExtensionRepository aetherRepository = (AetherExtensionRepository) getRepository();

        ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.addRepository(aetherRepository.getRemoteRepository());
        artifactRequest.setArtifact(new DefaultArtifact(this.mavenModel.getGroupId(), this.mavenModel.getArtifactId(),
            getType(), this.mavenModel.getVersion()));

        ArtifactResult artifactResult;
        try {
            artifactResult = repositorySystem.resolveArtifact(aetherRepository.getSession(), artifactRequest);
        } catch (ArtifactResolutionException e) {
            throw new ExtensionException("Failed to resolve artifact", e);
        }

        File aetherFile = artifactResult.getArtifact().getFile();

        try {
            FileUtils.moveFile(aetherFile, file);
        } catch (IOException e) {
            throw new ExtensionException("Failed to copy file", e);
        }
    }

    /**
     * @return the source Maven {@link Model}.
     */
    public Model getMavenModel()
    {
        return this.mavenModel;
    }
}
