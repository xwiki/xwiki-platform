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
package org.xwiki.test.docker.junit5;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.tool.extension.util.ExtensionMojoHelper;

/**
 * Generates a minimal XWiki WAR that is expanded in the passed target directory.
 *
 * @version $Id$
 * @since 10.9
 */
public class WARBuilder
{
    private static final Logger LOGGER = LoggerFactory.getLogger(WARBuilder.class);

    private static final String JAR = "jar";

    private ExtensionMojoHelper extensionHelper;

    private ConfigurationFilesGenerator configurationFilesGenerator;

    private ArtifactResolver artifactResolver;

    private MavenResolver mavenResolver;

    /**
     * Initialize an XWiki environment (ECM, etc).
     *
     * @param artifactResolver the resolver to resolve artifacts from Maven repositories
     * @param mavenResolver the resolver to read Maven POMs
     * @param repositoryResolver the resolver to create Maven repositories and sessions
     */
    public WARBuilder(ArtifactResolver artifactResolver, MavenResolver mavenResolver,
        RepositoryResolver repositoryResolver)
    {
        this.artifactResolver = artifactResolver;
        this.mavenResolver = mavenResolver;
        this.configurationFilesGenerator = new ConfigurationFilesGenerator(repositoryResolver);

        // TODO: extract code from ExtensionMojo so that we don't have to depend on a maven plugin....
        try {
            this.extensionHelper = ExtensionMojoHelper.create(null, new File("./target/xwiki-data/"));
        } catch (MojoExecutionException e) {
            throw new RuntimeException("Failed to initialize resolver", e);
        }
    }

    /**
     * Generates a minimal XWiki WAR that is expanded in the passed target directory.
     * <p>
     * Note that dependencies from the module under test are not included in this WAR since they'll be installed as
     * Extensions in {@link ExtensionInstaller} (thus proving that they can be installed as Extensions!).
     *
     * @param testConfiguration the configuration to build (database, debug mode, etc). This is used for example to
     * bundle the right JDBC driver in {@code WEB-INF/lib} for the target database
     * @param targetWARDirectory the target driedctory where the expanded WAR will be generated
     * @throws Exception in case of error
     */
    public void build(TestConfiguration testConfiguration, File targetWARDirectory) throws Exception
    {
        // Create a minimal XWiki WAR that doesn't contain any dependencies from the module under test (those
        // dependencies will be installed as extensions in ExtensionInstaller).

        // Step: Find the version of the XWiki JARs that we'll resolve to populate the minimal WAR in WEB-INF/lib
        LOGGER.info("Finding version ...");
        String xwikiVersion = this.mavenResolver.getModelFromCurrentPOM().getVersion();
        LOGGER.info("Found version = [{}]", xwikiVersion);

        // Step: Gather all the required JARs for the minimal WAR
        LOGGER.info("Resolving distribution dependencies ...");
        Collection<ArtifactResult> artifactResults = this.artifactResolver.getDistributionDependencies(xwikiVersion);
        List<File> warDependencies = new ArrayList<>();
        List<Artifact> jarDependencies = new ArrayList<>();
        List<File> skinDependencies = new ArrayList<>();
        for (ArtifactResult artifactResult : artifactResults) {
            Artifact artifact = artifactResult.getArtifact();
            // Note: we ignore XAR dependencies since they'll be provisioned as Extensions in ExtensionInstaller
            if (artifact.getExtension().equalsIgnoreCase("war")) {
                warDependencies.add(artifact.getFile());
            } else if (artifact.getExtension().equalsIgnoreCase("zip")) {
                skinDependencies.add(artifact.getFile());
            } else if (artifact.getExtension().equalsIgnoreCase(JAR)) {
                jarDependencies.add(artifact);
            }
        }

        // Step: Copy the JARs in WEB-INF/lib
        File webInfDirectory = new File(targetWARDirectory, "WEB-INF");
        File libDirectory = new File(webInfDirectory, "lib");
        copyJARs(testConfiguration, jarDependencies, libDirectory);

        // Step: Add the webapp resources (web.xml, templates VM files, etc)
        copyWebappResources(testConfiguration, warDependencies, targetWARDirectory);

        // Step: Add XWiki configuration files (depends on the selected DB for the hibernate one)
        LOGGER.info("Generating configuration files for database [{}]...", testConfiguration.getDatabase());
        this.configurationFilesGenerator.generate(testConfiguration, webInfDirectory, xwikiVersion,
            this.artifactResolver);

        // Step: Add the JDBC driver for the selected DB
        LOGGER.info("Copying JDBC driver for database [{}]...", testConfiguration.getDatabase());
        File jdbcDriverFile = getJDBCDriver(testConfiguration.getDatabase(), this.artifactResolver);
        if (testConfiguration.isDebug()) {
            LOGGER.info("... JDBC driver file: " + jdbcDriverFile);
        }
        XWikiFileUtils.copyFile(jdbcDriverFile, libDirectory);

        // Step: Unzip the Flamingo skin
        unzipSkin(testConfiguration, skinDependencies, targetWARDirectory);
    }

    private void copyWebappResources(TestConfiguration testConfiguration, List<File> warDependencies,
        File targetWARDirectory) throws Exception
    {
        LOGGER.info("Expanding WAR dependencies ...");
        for (File file : warDependencies) {
            // Unzip the WARs in the target directory
            if (testConfiguration.isDebug()) {
                LOGGER.info("... Unzipping WAR: " + file);
            }
            XWikiFileUtils.unzip(file, targetWARDirectory);
        }
    }

    private void copyJARs(TestConfiguration testConfiguration, List<Artifact> jarDependencies, File libDirectory)
        throws Exception
    {
        LOGGER.info("Copying JAR dependencies ...");
        XWikiFileUtils.createDirectory(libDirectory);
        for (Artifact artifact : jarDependencies) {
            if (testConfiguration.isDebug()) {
                LOGGER.info("... Copying JAR: " + artifact.getFile());
            }
            XWikiFileUtils.copyFile(artifact.getFile(), libDirectory);
            if (testConfiguration.isDebug()) {
                LOGGER.info("... Generating XED file for: " + artifact.getFile());
            }
            generateXED(artifact, libDirectory, this.mavenResolver);
        }
    }

    private void unzipSkin(TestConfiguration testConfiguration, List<File> skinDependencies, File targetWARDirectory)
        throws Exception
    {
        LOGGER.info("Copying Skin resources ...");
        File skinsDirectory = new File(targetWARDirectory, "skins");
        for (File file : skinDependencies) {
            if (testConfiguration.isDebug()) {
                LOGGER.info("... Unzipping skin: " + file);
            }
            XWikiFileUtils.unzip(file, skinsDirectory);
        }
    }

    private File getJDBCDriver(Database database, ArtifactResolver resolver) throws Exception
    {
        File driver;
        Artifact artifact;
        switch (database) {
            case MYSQL:
                artifact = new DefaultArtifact("mysql", "mysql-connector-java", JAR, "5.1.24");
                driver = resolver.resolveArtifact(artifact).getArtifact().getFile();
                break;
            case HSQLDB_EMBEDDED:
                artifact = new DefaultArtifact("org.hsqldb", "hsqldb", JAR, "2.4.1");
                driver = resolver.resolveArtifact(artifact).getArtifact().getFile();
                break;
            default:
                throw new RuntimeException(
                    String.format("Failed to get JDBC driver. Database [%s] not supported yet!", database));
        }
        return driver;
    }

    private void generateXED(Artifact artifact, File directory, MavenResolver resolver) throws Exception
    {
        Artifact pomArtifact = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), "pom",
            artifact.getVersion());
        Model model = resolver.getModelFromPOMArtifact(pomArtifact);
        File path = new File(directory, artifact.getArtifactId() + '-' + artifact.getBaseVersion() + ".xed");
        this.extensionHelper.serializeExtension(path, RepositoryUtils.toArtifact(artifact), model);
    }
}
