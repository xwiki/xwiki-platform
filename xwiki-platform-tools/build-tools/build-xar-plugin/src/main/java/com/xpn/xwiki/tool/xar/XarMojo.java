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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiveFileFilter;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Gather all resources in a XAR file (which is actually a ZIP file). Also generates a XAR
 * descriptor if none is provided.
 *
 * <p>Note that the generated descriptor currently doesn't handle
 * translations.</p>
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

    /**
     * Create the XAR by zipping the resource files.
     *
     * @throws Exception if the zipping failed for some reason
     */
    private void performArchive() throws Exception
    {
        File xarFile = new File(this.project.getBuild().getDirectory(),
            this.project.getArtifactId() + ".xar");
        File sourceDir = new File(this.project.getBuild().getOutputDirectory());
        ZipArchiver archiver = new ZipArchiver();
        archiver.setDestFile(xarFile);
        archiver.setIncludeEmptyDirs(false);
        archiver.setCompress(true);

        unpackDependentXars();
        archiver.addDirectory(sourceDir);

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

    private void unpackDependentXars() throws MojoExecutionException
    {
        Set artifacts = this.project.getArtifacts();
        for (Iterator iter = artifacts.iterator(); iter.hasNext();) {
            Artifact artifact = (Artifact) iter.next();
            ScopeArtifactFilter filter = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME);
            if (!artifact.isOptional() && filter.include(artifact)) {
                String type = artifact.getType();
                if ("xar".equals(type)) {
                    unpackXarToOutputDirectory(artifact);
                }
            }
        }
    }

    /**
     * Unpacks A XAR artifacts into the build output directory, along with the project's
     * XAR files.
     *
     * @param artifact the XAR artifact to unpack.
     * @throws MojoExecutionException in case of unpack error
     */
    private void unpackXarToOutputDirectory(Artifact artifact) throws MojoExecutionException
    {
        File outputLocation = new File(this.project.getBuild().getOutputDirectory());

        if (!outputLocation.exists()) {
            outputLocation.mkdirs();
        }

        File file = artifact.getFile();
        try {
            unpack(file, outputLocation);
        } catch (NoSuchArchiverException e) {
            this.getLog().info(
                "Skip unpacking dependency file with unknown extension: " + file.getPath());
        }
    }

    /**
     * Unpacks the archive file (exclude the package.xml file if it exists)
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

            // Do not unpack any package.xml file in dependant XARs. We'll generate a complete one
            // automatically.
            List filters = new ArrayList();
            filters.add(new ArchiveFileFilter() {
                public boolean include(InputStream dataStream, String entryName ) {
                    return (!entryName.equals("package.xml"));
                }});

            unArchiver.setArchiveFilters(filters);
            unArchiver.extract();
        } catch (Exception e) {
            throw new MojoExecutionException("Error unpacking file [" + file + "] to [" + location
                + "]", e);
        }
    }
}
