/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
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

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

/**
 * Gather all resources in a XAR file (which is actually a ZIP file)
 *
 * @version $Id: $
 * @goal xar
 * @phase package
 */
public class XarMojo extends AbstractMojo
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
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        if (this.project.getResources().size() < 1)
        {
            this.getLog().warn("No XAR created as no resources were found");
            return;
        }

        try
        {
            performArchive();
        }
        catch (Exception e)
        {
            throw new MojoExecutionException("Error while creating XAR file", e );
        }
    }

    private void performArchive() throws ArchiverException, IOException
    {
        File xarFile = new File(this.project.getBuild().getDirectory(),
            this.project.getArtifactId() + ".xar" );
        ZipArchiver archiver = new ZipArchiver();
        archiver.setDestFile(xarFile);
        archiver.setIncludeEmptyDirs(false);
        archiver.setCompress(true);
        archiver.addDirectory(new File(this.project.getBuild().getOutputDirectory()));
        archiver.createArchive();
        this.project.getArtifact().setFile(xarFile);
    }
}
