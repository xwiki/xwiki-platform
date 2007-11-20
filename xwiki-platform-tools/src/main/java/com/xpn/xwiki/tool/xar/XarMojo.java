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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiveEntry;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * Gather all resources in a XAR file (which is actually a ZIP file). Also generates a XAR
 * descriptor if none is provided.
 * <p>
 * Note that the generated descriptor currently doesn't handle translations.
 * </p>
 * 
 * @version $Id: $
 * @goal xar
 * @phase package
 * @requiresDependencyResolution runtime
 */
public class XarMojo extends AbstractXarMojo
{
    /**
     * To look up Archiver/UnArchiver implementations.
     * 
     * @parameter expression="${component.org.codehaus.plexus.archiver.manager.ArchiverManager}"
     * @required
     */
    protected ArchiverManager archiverManager;

    /**
     * The maven project.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        if (this.project.getResources().size() < 1) {
            this.getLog().warn("No XAR created as no resources were found");
            return;
        }

        try {
            performArchive();
        } catch (Exception e) {
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
        File xarFile =
            new File(this.project.getBuild().getDirectory(), this.project.getArtifactId()
                + ".xar");

        // The source dir points to the target/classes directory where the Maven resources plugin
        // has copied the XAR files during the process-resources phase.
        File sourceDir = new File(this.project.getBuild().getOutputDirectory());

        ZipArchiver archiver = new ZipArchiver();
        archiver.setDestFile(xarFile);
        archiver.setIncludeEmptyDirs(false);
        archiver.setCompress(true);

        // Unzip dependent XARs on top of this project's XML documents but without overwriting
        // existing files since we want this projet's files to be used if they override a file
        // present in a XAR dependency.
        unpackDependentXars();
        archiver.addDirectory(sourceDir);

        // If no package.xml can be found at the top level of the current project, generate one
        if (archiver.getFiles().get(PACKAGE_XML) == null) {
            File generatedPackageFile = new File(sourceDir, PACKAGE_XML);
            generatePackageXml(generatedPackageFile, archiver.getFiles().values());
            archiver.addFile(generatedPackageFile, PACKAGE_XML);
        }

        archiver.createArchive();

        this.project.getArtifact().setFile(xarFile);
    }

    /**
     * Create and add package configuration file to the package.
     * 
     * @param packageFile the package when to add configuration file.
     * @param files the files in the package.
     * @throws IOException error when writing the configuration file.
     */
    private void generatePackageXml(File packageFile, Collection files) throws IOException
    {
        this.getLog()
            .info("Generating package.xml descriptor at [" + packageFile.getPath() + "]");

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
        for (Iterator it = files.iterator(); it.hasNext();) {
            ArchiveEntry entry = (ArchiveEntry) it.next();

            String fullName = getFullNameFromXML(entry.getFile());

            if (fullName != null) {
                fw.write(" <file defaultAction=\"0\" language=\"\">" + fullName + "</file>\n");
            }
        }

        fw.write("  </files>\n");
        fw.write("</package>\n");
        fw.close();
    }

    /**
     * Get wiki document full name found in xml.
     * 
     * @param file the file to parse.
     * @return the full name of the document.
     */
    private String getFullNameFromXML(File file)
    {
        String fullname = null;

        try {
            XWikiDocument doc = new XWikiDocument();
            doc.fromXML(file);
            fullname = doc.getFullName();
        } catch (Exception e) {
            this.getLog().warn("Failed to parse " + file.getAbsolutePath(), e);
        }

        return fullname;
    }

    /**
     * Unpack xar dependencies before pack then into it.
     * 
     * @throws MojoExecutionException error when unpack dependencies.
     */
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
     * Unpacks A XAR artifacts into the build output directory, along with the project's XAR files.
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
        unpack(file, outputLocation, "XarMojo", false);
    }
}
