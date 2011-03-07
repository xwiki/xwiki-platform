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
 *
 */
package com.xpn.xwiki.tool.xar;

import java.io.File;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Build;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.SilentLog;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.zip.ZipFile;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;

/**
 * Unit tests for {@link XarMojo}.
 * 
 * @version $Id$
 * @since 1.12
 */
public class XarMojoTest extends AbstractMojoTestCase
{
    protected static final String TESTRESSOURCES_PATH = "target/test-classes";

    /**
     * The name of the XarMojo field containing the MavenProject.
     */
    protected static final String MAVENPROJECT_FIELD = "project";

    /**
     * The path to the xar archive the plugin tries to create. This is used for the testValidXml() test case
     */
    protected static final String XAR_PATH_VALIDXML =
        getPath(TESTRESSOURCES_PATH + "/validXml/target/xwiki-enterprise-wiki.xar");

    /**
     * The path to the xar archive the plugin tries to create. This is used for the testNoXml() test case
     */
    protected static final String XAR_PATH_NOXML =
        getPath(TESTRESSOURCES_PATH + "/noXml/target/xwiki-enterprise-wiki-test.xar");

    protected static final String TMPDIR_PATH = "target/test-tmp/";

    /**
     * A temporary directory used for extracting xar archives.
     */
    private File tempDir;

    private XarMojo mojo;

    /**
     * Preparing the environment for a test.
     * 
     * @throws Exception if an error occurs
     */
    @Override
    protected void setUp() throws Exception
    {
        this.tempDir = new File(getPath(TMPDIR_PATH));
        if (this.tempDir.exists()) {
            deleteDirectory(this.tempDir);
        }
        assertTrue("Cannot create a temporary directory at " + this.tempDir.getAbsolutePath(), this.tempDir.mkdirs());
        super.setUp();

        this.mojo = new XarMojo();
        this.mojo.setLog(new SilentLog());
    }

    /**
     * Test method for {@link com.xpn.xwiki.tool.xar.XarMojo#execute()}. It provides an invalid package.xml and the
     * class must throw an exception in order to pass the test.
     *
     * @throws Exception if the XarMojo class fails to instantiate
     */
    public void testInvalidPackageXmlThrowsException() throws Exception
    {
        MavenProject project = getMavenProjectInvalidXml();
        setVariableValueToObject(this.mojo, MAVENPROJECT_FIELD, project);

        try {
            this.mojo.execute();
            fail("Should have raised an exception since the provided package.xml is invalid.");
        } catch (MojoExecutionException expected) {
            // Expected
        }
    }

    /**
     * Test method for {@link com.xpn.xwiki.tool.xar.XarMojo#execute()}. It provides a valid package.xml and the class
     * must complete successful in order to pass the test.
     * 
     * @throws Exception if the XarMojo class fails to instantiate
     */
    public void testValidPackageXml() throws Exception
    {
        MavenProject project = getMavenProjectValidXml();
        setVariableValueToObject(this.mojo, MAVENPROJECT_FIELD, project);

        // Testing the plugin's execution
        try {
            this.mojo.execute();
        } catch (MojoExecutionException e) {
            fail("The execution failed with the following error : " + e.getMessage());
        }

        // Checking whether the generated xar archive contains the right data.
        ZipUnArchiver unarchiver = new ZipUnArchiver(new File(XAR_PATH_VALIDXML));

        ZipFile zip = new ZipFile(XAR_PATH_VALIDXML);
        Enumeration entries = zip.getEntries();
        assertTrue(entries.hasMoreElements());
        assertEquals(entries.nextElement().toString(), XarMojo.PACKAGE_XML);
        Collection<String> documentNames =
            XarMojo.getDocumentNamesFromXML(new File(getPath(TESTRESSOURCES_PATH
                + "/validXml/src/main/resources/package.xml")));
        int countEntries = 0;

        while (entries.hasMoreElements()) {
            String entryName = entries.nextElement().toString();
            ++countEntries;
            unarchiver.extract(entryName, this.tempDir);

            File currentFile = new File(this.tempDir, entryName);
            String documentName = XWikiDocument.getFullName(currentFile);
            if (!documentNames.contains(documentName)) {
                fail("Document [" + documentName + "] cannot be found in the newly created xar archive.");
            }
        }
        assertEquals("The newly created xar archive doesn't contain the required documents", documentNames.size(),
            countEntries);

    }

    /**
     * Test method for {@link com.xpn.xwiki.tool.xar.XarMojo#execute()}. It doesn't provide any package.xml and the
     * class must complete successful in order to pass the test.
     * 
     * @throws Exception if the XarMojo class fails to instantiate
     */
    public void testNoPackageXml() throws Exception
    {
        MavenProject project = getMavenProjectNoXml();
        setVariableValueToObject(this.mojo, MAVENPROJECT_FIELD, project);

        // Testing the plugin's execution
        this.mojo.execute();

        // Checking whether the generated xar archive contains the right data.

        // Verify that the zip file contains the generated package.xml file
        ZipFile zip = new ZipFile(XAR_PATH_NOXML);
        assertNotNull("Package.xml file not found in zip!", zip.getEntry(XarMojo.PACKAGE_XML));

        // Extract package.xml and extract all the entries one by one and read them as a XWiki Document to verify
        // they're valid.
        ZipUnArchiver unarchiver = new ZipUnArchiver(new File(XAR_PATH_NOXML));
        unarchiver.extract(XarMojo.PACKAGE_XML, this.tempDir);
        Collection<String> documentNames = XarMojo.getDocumentNamesFromXML(new File(this.tempDir, XarMojo.PACKAGE_XML));
        int countEntries = 0;
        Enumeration entries = zip.getEntries();
        while (entries.hasMoreElements()) {
            String entryName = entries.nextElement().toString();
            if (!entryName.equals(XarMojo.PACKAGE_XML)) {
                ++countEntries;
                unarchiver.extract(entryName, this.tempDir);

                File currentFile = new File(this.tempDir, entryName);
                String documentName = XWikiDocument.getFullName(currentFile);
                if (!documentNames.contains(documentName)) {
                    fail("Document [" + documentName + "] cannot be found in the newly created XAR archive.");
                }
            }
        }

        assertEquals("The newly created xar archive doesn't contain the required documents", documentNames.size(),
            countEntries);
    }

    /**
     * Gets a MavenProject containing an invalid package.xml for testing purposes.
     * 
     * @return the created MavenProject
     */
    private MavenProject getMavenProjectInvalidXml()
    {
        MavenProject project = new MavenProject();

        Build build = new Build();
        build.setOutputDirectory(getPath(TESTRESSOURCES_PATH + "/malformedXml/target/classes"));
        build.setDirectory(getPath(TESTRESSOURCES_PATH + "/malformedXml/target"));
        project.setBuild(build);

        project.setName("Test Project Invalid");
        project.setDescription("Test Description Invalid");
        project.setVersion("Test Version");
        project.setFile(new File(getPath(TESTRESSOURCES_PATH + "/malformedXml/pom.xml")));

        Resource res = new Resource();
        res.setDirectory(getPath(TESTRESSOURCES_PATH + "/malformedXml/src/main/resources"));
        project.addResource(res);
        res = new Resource();
        res.setDirectory(getPath(TESTRESSOURCES_PATH + "/malformedXml/target/maven-shared-archive-resources"));
        project.addResource(res);

        project.setArtifactId("xwiki-enterprise-wiki-test-artifact");
        project.setArtifacts(new HashSet());

        return project;
    }

    /**
     * Releasing the resources used by the test.
     * 
     * @throws Exception if an error occurs
     */
    @Override
    protected void tearDown() throws Exception
    {
        deleteDirectory(this.tempDir);
        super.tearDown();
    }

    /**
     * Gets a MavenProject containing a valid package.xml for testing purposes.
     * 
     * @return the created MavenProject
     */
    private MavenProject getMavenProjectValidXml()
    {
        MavenProject project = new MavenProject();

        Build build = new Build();
        build.setOutputDirectory(getPath(TESTRESSOURCES_PATH + "/validXml/target/classes"));
        build.setDirectory(getPath(TESTRESSOURCES_PATH + "/validXml/target"));
        project.setBuild(build);

        project.setName("Test Project");
        project.setDescription("Test Description");
        project.setVersion("TestVersion");
        project.setFile(new File(getPath(TESTRESSOURCES_PATH + "/validXml/pom.xml")));

        Resource res = new Resource();
        res.setDirectory(getPath(TESTRESSOURCES_PATH + "/validXml/src/main/resources"));
        project.addResource(res);
        res = new Resource();
        res.setDirectory(getPath(TESTRESSOURCES_PATH + "/validXml/target/maven-shared-archive-resources"));
        project.addResource(res);

        project.setArtifactId("xwiki-enterprise-wiki");
        project.setArtifacts(new HashSet());

        Artifact artifact =
            new DefaultArtifact("artifact2", "1", VersionRange.createFromVersion("1"), "", "", "", null);
        project.setArtifact(artifact);

        return project;
    }

    /**
     * Gets a MavenProject which doesn't contain any valid package.xml in resources/ for testing purposes.
     * 
     * @return the created MavenProject
     * @throws Exception if a dummy package.xml file cannot be created in target/classes.
     */
    private MavenProject getMavenProjectNoXml() throws Exception
    {
        MavenProject project = new MavenProject();

        Build build = new Build();
        build.setOutputDirectory(getPath(TESTRESSOURCES_PATH + "/noXml/target/classes"));
        build.setDirectory(getPath(TESTRESSOURCES_PATH + "/noXml/target"));
        // Creating a dummy package.xml file under target/classes. The plugin should ignore that and look
        // under resources/ for it and create a new one if it doesn't exist.
        File dummyPackageXml = new File(build.getOutputDirectory(), XarMojo.PACKAGE_XML);
        if (!dummyPackageXml.exists()) {
            dummyPackageXml.createNewFile();
        }
        project.setBuild(build);

        project.setName("Test Project2");
        project.setDescription("Test Description2");
        project.setVersion("TestVersion2");
        project.setFile(new File(getPath(TESTRESSOURCES_PATH + "/noXml/pom.xml")));

        Resource res = new Resource();
        res.setDirectory(getPath(TESTRESSOURCES_PATH + "/noXml/src/main/resources"));
        project.addResource(res);
        res = new Resource();
        res.setDirectory(getPath(TESTRESSOURCES_PATH + "/noXml/target/maven-shared-archive-resources"));
        project.addResource(res);

        project.setArtifactId("xwiki-enterprise-wiki-test");
        project.setArtifacts(new HashSet());

        Artifact artifact = new DefaultArtifact("artifact", "1", VersionRange.createFromVersion("1"), "", "", "", null);
        project.setArtifact(artifact);

        return project;
    }

    /**
     * Returns the absolute path corresponding to a string and makes sure the file separator is the one used by the host
     * OS.
     * 
     * @param s the relative path. It must use '/' as file separator
     * @return the absolute path
     */
    private static String getPath(String s)
    {
        return getBasedir() + File.separator + s.replace("/", File.separator);
    }

    /**
     * Recursively deletes a directory. It can also be used for deleting ordinary files.
     * 
     * @param path the path to the directory to be deleted
     * @return whether the action has been successfully completed or not.
     */
    private static boolean deleteDirectory(File path)
    {
        if (path.exists() && path.isDirectory()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }

        return path.delete();
    }
}
