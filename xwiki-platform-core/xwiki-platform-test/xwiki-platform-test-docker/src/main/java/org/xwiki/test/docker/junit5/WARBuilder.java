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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.test.docker.junit5.database.Database;
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

    private TestConfiguration testConfiguration;

    private ArtifactResolver artifactResolver;

    private MavenResolver mavenResolver;

    private File targetWARDirectory;

    /**
     * Initialize an XWiki environment (ECM, etc).
     *
     * @param testConfiguration the configuration to build (database, debug mode, etc)
     * @param targetWARDirectory the target driedctory where the expanded WAR will be generated
     * @param artifactResolver the resolver to resolve artifacts from Maven repositories
     * @param mavenResolver the resolver to read Maven POMs
     * @param repositoryResolver the resolver to create Maven repositories and sessions
     */
    public WARBuilder(TestConfiguration testConfiguration, File targetWARDirectory, ArtifactResolver artifactResolver,
        MavenResolver mavenResolver, RepositoryResolver repositoryResolver)
    {
        this.testConfiguration = testConfiguration;
        this.targetWARDirectory = targetWARDirectory;
        this.artifactResolver = artifactResolver;
        this.mavenResolver = mavenResolver;
        this.configurationFilesGenerator = new ConfigurationFilesGenerator(testConfiguration, repositoryResolver);

        if (!isAlreadyBuilt()) {
            LOGGER.info("XWiki WAR is not fully built in [{}], (re)building it!", targetWARDirectory);
            // TODO: extract code from ExtensionMojo so that we don't have to depend on a maven plugin....
            try {
                this.extensionHelper = ExtensionMojoHelper.create(null,
                    new File(String.format("%s/xwiki-data/", testConfiguration.getOutputDirectory())));
            } catch (MojoExecutionException e) {
                throw new RuntimeException("Failed to initialize resolver", e);
            }
        } else {
            LOGGER.info("XWiki WAR is already built in [{}], rebuilding only the minimum!", targetWARDirectory);
        }
    }

    /**
     * Generates a minimal XWiki WAR that is expanded in the passed target directory.
     * <p>
     * Note that dependencies from the module under test are not included in this WAR since they'll be installed as
     * Extensions in {@link ExtensionInstaller} (thus proving that they can be installed as Extensions!).
     *
     * @throws Exception in case of error
     */
    public void build() throws Exception
    {
        // Create a minimal XWiki WAR that doesn't contain any dependencies from the module under test (those
        // dependencies will be installed as extensions in ExtensionInstaller).

        // Step: Find the version of the XWiki JARs that we'll resolve to populate the minimal WAR in WEB-INF/lib
        LOGGER.info("Finding version ...");
        String xwikiVersion = this.mavenResolver.getPlatformVersion();
        LOGGER.info("Found version = [{}]", xwikiVersion);

        File webInfDirectory = new File(this.targetWARDirectory, "WEB-INF");

        if (!isAlreadyBuilt()) {

            // Step: Gather all the required JARs for the minimal WAR
            LOGGER.info("Resolving distribution dependencies ...");
            Collection<ArtifactResult> artifactResults = this.artifactResolver.getDistributionDependencies(xwikiVersion,
                computeExtraArtifacts(this.testConfiguration));
            List<File> warDependencies = new ArrayList<>();
            List<Artifact> jarDependencies = new ArrayList<>();
            List<File> skinDependencies = new ArrayList<>();
            for (ArtifactResult artifactResult : artifactResults) {
                Artifact artifact = artifactResult.getArtifact();
                // Note: we ignore XAR dependencies since they'll be provisioned as Extensions in ExtensionInstaller
                if (artifact.getExtension().equalsIgnoreCase("war")) {
                    warDependencies.add(artifact.getFile());
                    // Generate the XED file for the main WAR
                    if (artifact.getArtifactId().equals("xwiki-platform-web")) {
                        File xedFile = new File(this.targetWARDirectory, "META-INF/extension.xed");
                        xedFile.getParentFile().mkdirs();
                        generateXED(artifact, xedFile, this.mavenResolver);
                    }
                } else if (artifact.getExtension().equalsIgnoreCase("zip")) {
                    skinDependencies.add(artifact.getFile());
                } else if (artifact.getExtension().equalsIgnoreCase(JAR)) {
                    jarDependencies.add(artifact);
                }
            }

            // Step: Copy the JARs in WEB-INF/lib
            File libDirectory = new File(webInfDirectory, "lib");
            copyJARs(this.testConfiguration, jarDependencies, libDirectory);

            // Step: Add the webapp resources (web.xml, templates VM files, etc)
            copyWebappResources(this.testConfiguration, warDependencies, this.targetWARDirectory);

            // Step: Add the JDBC driver for the selected DB
            copyJDBCDriver(libDirectory);

            // Step: Unzip the Flamingo skin
            unzipSkin(testConfiguration, skinDependencies, targetWARDirectory);

            // Mark it as having been built successfully
            touchMarkerFile();
        }

        // Step: Add XWiki configuration files (depends on the selected DB for the hibernate one)
        LOGGER.info("Generating configuration files for database [{}]...", testConfiguration.getDatabase());
        this.configurationFilesGenerator.generate(webInfDirectory, xwikiVersion, this.artifactResolver);
    }

    private void copyJDBCDriver(File libDirectory) throws Exception
    {
        LOGGER.info("Copying JDBC driver for database [{}]...", this.testConfiguration.getDatabase());
        File jdbcDriverFile = getJDBCDriver(this.testConfiguration.getDatabase(), this.artifactResolver);
        if (this.testConfiguration.isVerbose()) {
            LOGGER.info("... JDBC driver file: " + jdbcDriverFile);
        }
        DockerTestUtils.copyFile(jdbcDriverFile, libDirectory);
    }

    private List<Artifact> computeExtraArtifacts(TestConfiguration testConfiguration) throws Exception
    {
        List<Artifact> artifacts = new ArrayList<>();
        if (!testConfiguration.getExtraJARs().isEmpty()) {
            for (List<String> jarData : testConfiguration.getExtraJARs()) {
                LOGGER.info("Adding extra JAR to WEB-INF/lib: [{}:{}]", jarData.get(0), jarData.get(1));
                Artifact artifact = new DefaultArtifact(jarData.get(0), jarData.get(1), JAR,
                    this.mavenResolver.getPlatformVersion());
                artifacts.add(artifact);
            }
        }

        // Add the Clover JAR if it's defined in the current pom.xml since it's needed when the clover profile is
        // enabled. Note that we need this since by default we don't add any JAR to WEB-INF/lib (since we install
        // module artifacts as XWiki Extensions).
        Model model = this.mavenResolver.getModelFromCurrentPOM();
        for (Dependency dependency : model.getDependencies()) {
            if (dependency.getArtifactId().equals("clover") && dependency.getGroupId().equals("org.openclover")) {
                artifacts.add(this.mavenResolver.convertToArtifact(dependency));
            }
        }

        return artifacts;
    }

    private void copyWebappResources(TestConfiguration testConfiguration, List<File> warDependencies,
        File targetWARDirectory) throws Exception
    {
        LOGGER.info("Expanding WAR dependencies ...");
        for (File file : warDependencies) {
            // Unzip the WARs in the target directory
            if (testConfiguration.isVerbose()) {
                LOGGER.info("... Unzipping WAR: " + file);
            }
            DockerTestUtils.unzip(file, targetWARDirectory);
        }
    }

    private void copyJARs(TestConfiguration testConfiguration, List<Artifact> jarDependencies, File libDirectory)
        throws Exception
    {
        LOGGER.info("Copying JAR dependencies ...");
        DockerTestUtils.createDirectory(libDirectory);
        for (Artifact artifact : jarDependencies) {
            if (testConfiguration.isVerbose()) {
                LOGGER.info("... Copying JAR: " + artifact.getFile());
            }
            DockerTestUtils.copyFile(artifact.getFile(), libDirectory);
            if (testConfiguration.isVerbose()) {
                LOGGER.info("... Generating XED file for: " + artifact.getFile());
            }
            generateXEDForJAR(artifact, libDirectory, this.mavenResolver);
        }
    }

    private void unzipSkin(TestConfiguration testConfiguration, List<File> skinDependencies, File targetWARDirectory)
        throws Exception
    {
        LOGGER.info("Copying Skin resources ...");
        File skinsDirectory = new File(targetWARDirectory, "skins");
        for (File file : skinDependencies) {
            if (testConfiguration.isVerbose()) {
                LOGGER.info("... Unzipping skin: " + file);
            }
            DockerTestUtils.unzip(file, skinsDirectory);
        }
    }

    private File getJDBCDriver(Database database, ArtifactResolver resolver) throws Exception
    {
        Artifact artifact;
        switch (database) {
            case MYSQL:
                String mysqlDriverVersion = this.testConfiguration.getJDBCDriverVersion() != null
                    ? this.testConfiguration.getJDBCDriverVersion() : "5.1.45";
                artifact = new DefaultArtifact("mysql", "mysql-connector-java", JAR, mysqlDriverVersion);
                break;
            case MARIADB:
                String mariadbDriverVersion = this.testConfiguration.getJDBCDriverVersion() != null
                    ? this.testConfiguration.getJDBCDriverVersion() : "2.3.0";
                artifact = new DefaultArtifact("org.mariadb.jdbc", "mariadb-java-client", JAR, mariadbDriverVersion);
                break;
            case POSTGRESQL:
                String pgsqlDriverVersion = this.testConfiguration.getJDBCDriverVersion() != null
                    ? this.testConfiguration.getJDBCDriverVersion() : "42.1.4";
                artifact = new DefaultArtifact("org.postgresql", "postgresql", JAR, pgsqlDriverVersion);
                break;
            case HSQLDB_EMBEDDED:
                String hsqldbDriverVersion = this.testConfiguration.getJDBCDriverVersion() != null
                    ? this.testConfiguration.getJDBCDriverVersion() : "2.4.1";
                artifact = new DefaultArtifact("org.hsqldb", "hsqldb", JAR, hsqldbDriverVersion);
                break;
            default:
                throw new RuntimeException(
                    String.format("Failed to get JDBC driver. Database [%s] not supported yet!", database));
        }

        return resolver.resolveArtifact(artifact).getArtifact().getFile();
    }

    private void generateXEDForJAR(Artifact artifact, File targetDirectory, MavenResolver resolver) throws Exception
    {
        File targetXEDFile =
            new File(targetDirectory, artifact.getArtifactId() + '-' + artifact.getBaseVersion() + ".xed");
        generateXED(artifact, targetXEDFile, resolver);
    }

    private void generateXED(Artifact artifact, File targetXEDFile, MavenResolver resolver) throws Exception
    {
        Artifact pomArtifact = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), "pom",
            artifact.getVersion());
        Model model = resolver.getModelFromPOMArtifact(pomArtifact);
        this.extensionHelper.serializeExtension(targetXEDFile, RepositoryUtils.toArtifact(artifact), model);
    }

    private boolean isAlreadyBuilt()
    {
        return getMarkerFile().exists();
    }

    private void touchMarkerFile() throws IOException
    {
        File markerFile = getMarkerFile();
        if (!markerFile.exists()) {
            new FileOutputStream(markerFile).close();
        }
    }

    private File getMarkerFile()
    {
        return new File(this.targetWARDirectory, "build.marker");
    }
}
