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
package org.xwiki.test.docker.internal.junit5;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.test.docker.internal.junit5.configuration.ConfigurationFilesGenerator;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.database.Database;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.integration.maven.ArtifactResolver;
import org.xwiki.test.integration.maven.MavenResolver;
import org.xwiki.test.integration.maven.RepositoryResolver;
import org.xwiki.tool.extension.util.ExtensionMojoHelper;

import static org.xwiki.test.docker.internal.junit5.FileTestUtils.copyDirectory;
import static org.xwiki.test.docker.internal.junit5.FileTestUtils.copyFile;
import static org.xwiki.test.docker.internal.junit5.FileTestUtils.createDirectory;
import static org.xwiki.test.docker.internal.junit5.FileTestUtils.unzip;

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

                LOGGER.info("Using the following extension overrides: {}", testConfiguration.getExtensionOverrides());
                this.extensionHelper.setExtensionOverrides(testConfiguration.getExtensionOverrides());
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
            List<Artifact> extraArtifacts =this.mavenResolver.convertToArtifacts(this.testConfiguration.getExtraJARs(),
                this.testConfiguration.isResolveExtraJARs());
            this.mavenResolver.addCloverJAR(extraArtifacts);
            Collection<ArtifactResult> artifactResults = this.artifactResolver.getDistributionDependencies(xwikiVersion,
                extraArtifacts);
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
            File webInfLibDirectory = new File(webInfDirectory, "lib");
            copyJARs(this.testConfiguration, jarDependencies, webInfLibDirectory);

            // Step: Copy target/classes to WEB-INF/classes to allow docker tests to provide custom java code that is
            // deployed in the custom WAR.
            File webInfClassesDirectory = new File(webInfDirectory, "classes");
            copyClasses(webInfClassesDirectory);

            // Step: Add the webapp resources (web.xml, templates VM files, etc)
            copyWebappResources(this.testConfiguration, warDependencies, this.targetWARDirectory);

            // Step: Add the JDBC driver for the selected DB
            copyJDBCDriver(webInfLibDirectory);

            // Step: Unzip the Flamingo skin
            unzipSkin(testConfiguration, skinDependencies, targetWARDirectory);

            // In order to work around issue https://jira.xwiki.org/browse/XWIKI-18335 with Jetty 10+, we replace
            // jetty-web.xml with an overridden version when we're deploying on Jetty 10+
            handleJetty10(webInfDirectory);

            // Mark it as having been built successfully
            touchMarkerFile();
        }

        // Step: Add XWiki configuration files (depends on the selected DB for the hibernate one)
        LOGGER.info("Generating configuration files for database [{}]...", testConfiguration.getDatabase());
        this.configurationFilesGenerator.generate(webInfDirectory, xwikiVersion, this.artifactResolver);
    }

    private void handleJetty10(File webInfDirectory) throws Exception
    {
        ServletEngine engine = this.testConfiguration.getServletEngine();
        String tag = this.testConfiguration.getServletEngineTag();
        if (engine == ServletEngine.JETTY && extractJettyVersionFromDockerTag(tag) >= 10) {
            // Override the jetty-web.xml
            copyJettyWebFile(webInfDirectory);
        }
    }

    private void copyJettyWebFile(File webInfDirectory) throws Exception
    {
        File outputFile = new File(webInfDirectory, "jetty-web.xml");
        if (this.testConfiguration.isVerbose()) {
            LOGGER.info("... Override jetty-web.xml since Jetty version is >= 10");
        }
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            InputStream is = getClass().getClassLoader().getResourceAsStream("jetty10-web.xml");
            IOUtils.copy(is, fos);
        }
    }

    private int extractJettyVersionFromDockerTag(String tag)
    {
        int result;
        // TODO: Latest is currently 9.4.x for Jetty. Change this when it's no longer the case.
        if (this.testConfiguration.getServletEngineTag() == null) {
            result = 9;
        } else {
            try {
                result = Integer.valueOf(this.testConfiguration.getServletEngineTag().substring(0, 2));
            } catch (Exception e) {
                // This can happen for example if we have "9-jre11" since "9-" will raise a NumberFormatException.
                // In this case consider we're on 9.
                result = 9;
            }
        }
        return result;
    }

    private void copyClasses(File webInfClassesDirectory) throws Exception
    {
        File sourceDirectory = new File("target/classes");
        if (sourceDirectory.exists()) {
            copyDirectory(new File("target/classes"), webInfClassesDirectory);
        }
    }

    private void copyJDBCDriver(File libDirectory) throws Exception
    {
        LOGGER.info("Copying JDBC driver for database [{}]...", this.testConfiguration.getDatabase());
        File jdbcDriverFile = getJDBCDriver(this.testConfiguration.getDatabase(), this.artifactResolver);
        if (this.testConfiguration.isVerbose()) {
            LOGGER.info("... JDBC driver file: {}", jdbcDriverFile);
        }
        copyFile(jdbcDriverFile, libDirectory);
    }

    private void copyWebappResources(TestConfiguration testConfiguration, List<File> warDependencies,
        File targetWARDirectory) throws Exception
    {
        LOGGER.info("Expanding WAR dependencies ...");
        for (File file : warDependencies) {
            // Unzip the WARs in the target directory
            if (testConfiguration.isVerbose()) {
                LOGGER.info("... Unzipping WAR: {}", file);
            }
            unzip(file, targetWARDirectory);
        }

        // Copy maven project webapp resources, if any.
        File webappResourceDirectory = new File("src/test/webapp");
        if (webappResourceDirectory.exists()) {
            copyDirectory(webappResourceDirectory, this.targetWARDirectory);
        }
    }

    private void copyJARs(TestConfiguration testConfiguration, List<Artifact> jarDependencies, File libDirectory)
        throws Exception
    {
        LOGGER.info("Copying JAR dependencies ...");
        createDirectory(libDirectory);
        for (Artifact artifact : jarDependencies) {
            if (testConfiguration.isDebug()) {
                LOGGER.info("... Copying JAR: {}", artifact.getFile());
            }
            copyFile(artifact.getFile(), libDirectory);
            if (testConfiguration.isDebug()) {
                LOGGER.info("... Generating XED file for: {}", artifact.getFile());
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
                LOGGER.info("... Unzipping skin: {}", file);
            }
            unzip(file, skinsDirectory);
        }
    }

    private File getJDBCDriver(Database database, ArtifactResolver resolver) throws Exception
    {
        Artifact artifact;

        // Note: If the JDBC driver version is specified as "pom" or null then extract the information from the current
        // POM.

        switch (database) {
            case MYSQL:
                String mysqlDriverVersion = isJDBCDriverSpecified(this.testConfiguration.getJDBCDriverVersion())
                    ? this.testConfiguration.getJDBCDriverVersion()
                    : this.mavenResolver.getPropertyFromCurrentPOM("mysql.version");
                artifact = new DefaultArtifact("mysql", "mysql-connector-java", JAR, mysqlDriverVersion);
                break;
            case MARIADB:
                String mariadbDriverVersion = isJDBCDriverSpecified(this.testConfiguration.getJDBCDriverVersion())
                    ? this.testConfiguration.getJDBCDriverVersion()
                    : this.mavenResolver.getPropertyFromCurrentPOM("mariadb.version");
                artifact = new DefaultArtifact("org.mariadb.jdbc", "mariadb-java-client", JAR, mariadbDriverVersion);
                break;
            case POSTGRESQL:
                String pgsqlDriverVersion = isJDBCDriverSpecified(this.testConfiguration.getJDBCDriverVersion())
                    ? this.testConfiguration.getJDBCDriverVersion()
                    : this.mavenResolver.getPropertyFromCurrentPOM("pgsql.version");
                artifact = new DefaultArtifact("org.postgresql", "postgresql", JAR, pgsqlDriverVersion);
                break;
            case HSQLDB_EMBEDDED:
                String hsqldbDriverVersion = isJDBCDriverSpecified(this.testConfiguration.getJDBCDriverVersion())
                    ? this.testConfiguration.getJDBCDriverVersion()
                    : this.mavenResolver.getPropertyFromCurrentPOM("hsqldb.version");
                artifact = new DefaultArtifact("org.hsqldb", "hsqldb", JAR, hsqldbDriverVersion);
                break;
            case ORACLE:
                String oracleDriverVersion = isJDBCDriverSpecified(this.testConfiguration.getJDBCDriverVersion())
                    ? this.testConfiguration.getJDBCDriverVersion()
                    : this.mavenResolver.getPropertyFromCurrentPOM("oracle.version");
                artifact = new DefaultArtifact("com.oracle.ojdbc", "ojdbc8", JAR, oracleDriverVersion);
                break;
            default:
                throw new RuntimeException(
                    String.format("Failed to get JDBC driver. Database [%s] not supported yet!", database));
        }

        return resolver.resolveArtifact(artifact).getArtifact().getFile();
    }

    private boolean isJDBCDriverSpecified(String jdbcDriverVersion)
    {
        return jdbcDriverVersion != null && !jdbcDriverVersion.equalsIgnoreCase("pom");
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
