/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Gather all resources in a XAR file (which is actually a ZIP file). Also generates a XAR
 * descriptor if none is provided.
 *
 * @version $Id: $
 * @goal xar
 * @phase package
 * @requiresDependencyResolution runtime
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
     * Directory to unpack dependent XARs into if needed
     *
     * @parameter expression="${project.build.directory}/xar/work"
     * @required
     */
    private File workDirectory;

    /**
     * To look up Archiver/UnArchiver implementations
     *
     * @parameter expression="${component.org.codehaus.plexus.archiver.manager.ArchiverManager}"
     * @required
     */
    protected ArchiverManager archiverManager;
    
    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        if (this.project.getResources().size() < 1) {
            this.getLog().warn("No XAR created as no resources were found");
            return;
        }

        try {
            performArchive();
        }
        catch (Exception e) {
            throw new MojoExecutionException("Error while creating XAR file", e);
        }
    }

    private void performArchive() throws ArchiverException, IOException, MojoExecutionException
    {
        File xarFile = new File(this.project.getBuild().getDirectory(),
            this.project.getArtifactId() + ".xar");
        File sourceDir = new File(this.project.getBuild().getOutputDirectory());
        ZipArchiver archiver = new ZipArchiver();
        archiver.setDestFile(xarFile);
        archiver.setIncludeEmptyDirs(false);
        archiver.setCompress(true);
        archiver.addDirectory(sourceDir);

        List dependentXars = unpackDependentXars();
        for (Iterator iter = dependentXars.iterator(); iter.hasNext();) {
            File xarDir = (File) iter.next();
            archiver.addDirectory(xarDir, null, new String[] {"package.xml"});
        }

        // If no package.xml can be found at the top level of the current project, generate one
        if (archiver.getFiles().get("package.xml") == null) {
            File generatedPackageFile = new File(sourceDir, "package.xml");
            generatePackageXml(generatedPackageFile, archiver.getFiles().keySet());
            archiver.addFile(generatedPackageFile, "package.xml");
        }

        archiver.createArchive();

        this.project.getArtifact().setFile(xarFile);
    }

    private void generatePackageXml(File packageFile, Set files) throws IOException
    {
        this.getLog().info("Generating package.xml descriptor at [" + packageFile.getPath() + "]"); 

        FileWriter fw = new FileWriter(packageFile);
        fw.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
        fw.write("<package>\n");
        fw.write("  <infos>\n");
        fw.write("    <name>" + this.project.getName() + "</name>\n");
        fw.write("    <description>" + this.project.getDescription() + "</description>\n");
        fw.write("    <licence></licence>\n");
        fw.write("    <author>XWiki.Admin</author>\n");
        fw.write("    <version>" + this.project.getVersion() + "</version>\n");
        fw.write("    <backupPack>true</backupPack>\n");
        fw.write("  </infos>\n");
        fw.write("  <files>\n");

        // First element before the "/" is the space name, rest is the document name. Warn if there
        // are more than 1 "/".
        for (Iterator it = files.iterator(); it.hasNext();)
        {
            String fileName = (String) it.next();
            StringTokenizer st = new StringTokenizer(fileName, "/");
            if (st.countTokens() != 2) {
                this.getLog().warn("Invalid file location [" + fileName + "], skipping it.");
            } else {
                fw.write("    <file defaultAction=\"0\" language=\"\">" + st.nextToken() + "."
                    + st.nextToken() + "</file>\n");
            }
        }

        fw.write("  </files>\n");
        fw.write("</package>\n");
        fw.close();
    }

    private List unpackDependentXars() throws MojoExecutionException
    {
        List dependentXars = new ArrayList(); 
        Set artifacts = this.project.getArtifacts();
        for (Iterator iter = artifacts.iterator(); iter.hasNext();) {
            Artifact artifact = (Artifact) iter.next();
            ScopeArtifactFilter filter = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME);
            if (!artifact.isOptional() && filter.include(artifact)) {
                String type = artifact.getType();
                if ("xar".equals(type)) {
                    File xarDir = unpackXarToTempDirectory(artifact);
                    dependentXars.add(xarDir);
                }
            }
        }
        return dependentXars;
    }

    /**
     * Unpacks A XAR artifacts into a temporary directory inside <tt>workDirectory</tt> named with
     * the name of the XAR.
     *
     * @param artifact the XAR artifact to unpack.
     * @return the directory containing the unpacked XAR.
     * @throws MojoExecutionException in case of unpack error
     */
    private File unpackXarToTempDirectory(Artifact artifact) throws MojoExecutionException
    {
        String name = artifact.getFile().getName();
        File tempLocation = new File(this.workDirectory, name.substring(0, name.length() - 4));

        boolean process = false;
        if (!tempLocation.exists()) {
            tempLocation.mkdirs();
            process = true;
        } else if (artifact.getFile().lastModified() > tempLocation.lastModified()) {
            process = true;
        }

        if (process) {
            File file = artifact.getFile();
            try {
                unpack(file, tempLocation);
            } catch (NoSuchArchiverException e) {
                this.getLog().info(
                    "Skip unpacking dependency file with unknown extension: " + file.getPath());
            }
        }

        return tempLocation;
    }

    /**
     * Unpacks the archive file.
     *
     * @param file File to be unpacked.
     * @param location Location where to put the unpacked files.
     */
    private void unpack(File file, File location)
        throws MojoExecutionException, NoSuchArchiverException
    {
        try {
            ZipUnArchiver unArchiver = new ZipUnArchiver();
            unArchiver.enableLogging(new ConsoleLogger(Logger.LEVEL_ERROR, "XarMojo"));
            unArchiver.setSourceFile(file);
            unArchiver.setDestDirectory(location);
            unArchiver.setOverwrite(true);
            unArchiver.extract();
        } catch (Exception e) {
            throw new MojoExecutionException("Error unpacking file [" + file + "] to [" + location
                + "]", e);
        }
    }
}
