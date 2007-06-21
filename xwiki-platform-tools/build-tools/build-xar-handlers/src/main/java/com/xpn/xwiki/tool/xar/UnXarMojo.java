/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Expand a XAR file.
 *
 * @version $Id: $
 * @goal unxar
 * @requiresDependencyResolution runtime
 */
public class UnXarMojo extends AbstractMojo
{
    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The groupId of the XAR dependency to expand
     * @parameter
     * @required
     */
    private String groupId;

    /**
     * The artifactId of the XAR dependency to expand
     * @parameter
     * @required
     */
    private String artifactId;

    /**
     * The location where to put the expanded XAR
     * @parameter
     * @required
     */
    private File outputDirectory;

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        this.outputDirectory.mkdirs();

        try
        {
            performUnArchive();
        }
        catch (Exception e)
        {
            throw new MojoExecutionException("Error while expanding the XAR file ["
                + this.groupId + ":" + this.artifactId + "]", e );
        }
    }

    private Artifact findArtifact() throws MojoExecutionException
    {
        Artifact resolvedArtifact = null;

        getLog().debug("Searching for an artifact that matches [" + this.groupId + ":"
            + this.artifactId + "]...");

        Iterator it = this.project.getArtifacts().iterator();
        while (it.hasNext())
        {
            Artifact artifact = (Artifact) it.next();

            getLog().debug("Checking artifact [" + artifact.getGroupId() + ":"
                + artifact.getArtifactId() + ":" + artifact.getType() + "]...");

            if (artifact.getGroupId().equals(this.groupId)
                && artifact.getArtifactId().equals(this.artifactId))
            {
                resolvedArtifact = artifact;
                break;
            }
        }

        if (resolvedArtifact == null)
        {
            throw new MojoExecutionException( "Artifact [" + this.groupId + ":" + this.artifactId
                + "] is not a dependency of the project.");
        }

        return resolvedArtifact;
    }

    private void performUnArchive() throws ArchiverException, IOException, MojoExecutionException
    {
        Artifact artifact = findArtifact();

        getLog().debug("Source XAR = [" + artifact.getFile() + "]");

        ZipUnArchiver unArchiver = new ZipUnArchiver();
        unArchiver.setSourceFile(artifact.getFile());
        unArchiver.setDestFile(this.outputDirectory);
        unArchiver.enableLogging(new ConsoleLogger(Logger.LEVEL_ERROR, "UnXarMojo"));
        unArchiver.extract();
    }
}
