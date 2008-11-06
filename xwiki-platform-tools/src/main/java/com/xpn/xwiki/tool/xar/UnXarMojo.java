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
package com.xpn.xwiki.tool.xar;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.archiver.ArchiverException;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Expand a XAR file.
 * 
 * @version $Id: $
 * @goal unxar
 * @requiresDependencyResolution runtime
 */
public class UnXarMojo extends AbstractXarMojo
{
    /**
     * ":".
     */
    private static final String TWO_POINTS = ":";

    /**
     * "...".
     */
    private static final String DOTDOTDOT = "...";

    /**
     * The groupId of the XAR dependency to expand.
     * 
     * @parameter
     * @required
     */
    private String groupId;

    /**
     * The artifactId of the XAR dependency to expand.
     * 
     * @parameter
     * @required
     */
    private String artifactId;

    /**
     * The location where to put the expanded XAR.
     * 
     * @parameter
     * @required
     */
    private File outputDirectory;

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        this.outputDirectory.mkdirs();

        try {
            performUnArchive();
        } catch (Exception e) {
            throw new MojoExecutionException("Error while expanding the XAR file " + HOOK_OPEN + this.groupId
                + TWO_POINTS + this.artifactId + HOOK_CLOSE, e);
        }
    }

    /**
     * @return the maven artifact.
     * @throws MojoExecutionException error when seraching for the mavebn artifact.
     */
    private Artifact findArtifact() throws MojoExecutionException
    {
        Artifact resolvedArtifact = null;

        getLog().debug(
            "Searching for an artifact that matches " + HOOK_OPEN + this.groupId + TWO_POINTS + this.artifactId
                + HOOK_CLOSE + DOTDOTDOT);

        for (Artifact artifact : (Set<Artifact>) this.project.getArtifacts()) {
            getLog().debug(
                "Checking artifact " + HOOK_OPEN + artifact.getGroupId() + TWO_POINTS + artifact.getArtifactId()
                    + TWO_POINTS + artifact.getType() + HOOK_CLOSE + DOTDOTDOT);

            if (artifact.getGroupId().equals(this.groupId) && artifact.getArtifactId().equals(this.artifactId)) {
                resolvedArtifact = artifact;
                break;
            }
        }

        if (resolvedArtifact == null) {
            throw new MojoExecutionException("Artifact " + HOOK_OPEN + this.groupId + TWO_POINTS + this.artifactId
                + HOOK_CLOSE + " is not a dependency of the project.");
        }

        return resolvedArtifact;
    }

    /**
     * Unzip maven artifact.
     * 
     * @throws ArchiverException error when unzip package.
     * @throws IOException error when unzip package.
     * @throws MojoExecutionException error when unzip package.
     */
    private void performUnArchive() throws ArchiverException, IOException, MojoExecutionException
    {
        Artifact artifact = findArtifact();

        getLog().debug("Source XAR = " + HOOK_OPEN + artifact.getFile() + HOOK_CLOSE);

        unpack(artifact.getFile(), this.outputDirectory, "XarMojo", true);
    }
}
