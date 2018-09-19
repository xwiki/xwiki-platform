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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.FileUtils;
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
 * @since 10.9RC1
 */
public class WARBuilder
{
    private static final Logger LOGGER = LoggerFactory.getLogger(WARBuilder.class);

    private static final String JAR = "jar";

    private static final String VM_EXTENSION = ".vm";

    private static final String DB_USERNAME = "xwiki";

    private static final String DB_PASSWORD = DB_USERNAME;

    private static final String SKIN = "flamingo";

    private ExtensionMojoHelper extensionHelper;

    /**
     * Initialize an XWiki environment (ECM, etc).
     */
    public WARBuilder()
    {
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
     * @param database the database to generate a WAR for. Specifically this means bundling the right JDBC driver in
     * {@code WEB-INF/lib}
     * @param targetWARDirectory the target driedctory where the expanded WAR will be generated
     * @throws Exception in case of error
     */
    public void build(Database database, File targetWARDirectory) throws Exception
    {
        // Create a minimal XWiki WAR that doesn't contain any dependencies from the module under test (those
        // dependencies will be installed as extensions in ExtensionInstaller).

        // Step 1: Find the version of the XWiki JARs that we'll resolve to populate the minimal WAR in WEB-INF/lib
        LOGGER.debug("Finding version ...");
        ArtifactResolver artifactResolver = ArtifactResolver.getInstance();
        MavenResolver mavenResolver = MavenResolver.getInstance();
        String xwikiVersion = mavenResolver.getModelFromCurrentPOM().getVersion();
        LOGGER.debug("  -> Version = [{}]", xwikiVersion);

        // Step 2: Gather all the required JARs for the minimal WAR
        LOGGER.debug("Resolving distribution dependencies ...");
        Collection<ArtifactResult> artifactResults = artifactResolver.getDistributionDependencies(xwikiVersion);
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

        // Step 3: Since we want to be able to provision SNAPSHOT extensions, we need to configure the SNAPSHOT
        //         extension repository. We do that by adding a dependency which will inject it automatically in the
        //         default list of extension repositories.
        Artifact snapshotRepositoryArtifact = new DefaultArtifact("org.xwiki.commons",
            "xwiki-commons-extension-repository-maven-snapshots", JAR, xwikiVersion);
        jarDependencies.add(artifactResolver.resolveArtifact(snapshotRepositoryArtifact).getArtifact());

        // Step 4: Copy the JARs in WEB-INF/lib
        File webInfDirectory = new File(targetWARDirectory, "WEB-INF");
        File libDirectory = new File(webInfDirectory, "lib");
        copyJARs(jarDependencies, libDirectory);

        // Step 5: Add the webapp resources (web.xml, templates VM files, etc)
        copyWebappResources(warDependencies, targetWARDirectory);

        // Step 6: Add XWiki configuration files (depends on the selected DB for the hibernate one)
        LOGGER.debug("Generating configuration files for database [{}]...", database);
        generateConfigurationFiles(webInfDirectory, xwikiVersion, database, artifactResolver);

        // Step 7: Add the JDBC driver for the selected DB
        LOGGER.debug("Copying JDBC driver for database [{}]...", database);
        File jdbcDriverFile = getJDBCDriver(database, artifactResolver);
        LOGGER.debug("  ... JDBC driver file: " + jdbcDriverFile);
        copyFile(jdbcDriverFile, libDirectory);

        // Step 8: Unzip the Flamingo skin
        unzipSkin(skinDependencies, targetWARDirectory);
    }

    private void copyWebappResources(List<File> warDependencies, File targetWARDirectory) throws Exception
    {
        LOGGER.debug("Expanding WAR dependencies ...");
        for (File file : warDependencies) {
            // Unzip the WARs in the target directory
            LOGGER.debug("  ... Unzipping WAR: " + file);
            unzip(file, targetWARDirectory);
        }
    }

    private void copyJARs(List<Artifact> jarDependencies, File libDirectory) throws Exception
    {
        LOGGER.debug("Copying JAR dependencies ...");
        createDirectory(libDirectory);
        for (Artifact artifact : jarDependencies) {
            LOGGER.debug("  ... Copying JAR: " + artifact.getFile());
            copyFile(artifact.getFile(), libDirectory);
            LOGGER.debug("  ... Generating XED file for: " + artifact.getFile());
            generateXED(artifact, libDirectory, MavenResolver.getInstance());
        }
    }

    private void unzipSkin(List<File> skinDependencies, File targetWARDirectory) throws Exception
    {
        LOGGER.debug("Copying Skin resources ...");
        File skinsDirectory = new File(targetWARDirectory, "skins");
        for (File file : skinDependencies) {
            LOGGER.debug("  ... Unzipping skin: " + file);
            unzip(file, skinsDirectory);
        }
    }

    private void unzip(File source, File targetDirectory) throws Exception
    {
        createDirectory(targetDirectory);
        try {
            ZipUnArchiver unArchiver = new ZipUnArchiver();
            unArchiver.enableLogging(new ConsoleLogger(org.codehaus.plexus.logging.Logger.LEVEL_ERROR, "Package"));
            unArchiver.setSourceFile(source);
            unArchiver.setDestDirectory(targetDirectory);
            unArchiver.setOverwrite(true);
            unArchiver.extract();
        } catch (Exception e) {
            throw new Exception(
                String.format("Error unpacking file [%s] into [%s]", source, targetDirectory), e);
        }
    }

    private void createDirectory(File directory)
    {
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    private void copyFile(File source, File targetDirectory) throws Exception
    {
        try {
            FileUtils.copyFileToDirectoryIfModified(source, targetDirectory);
        } catch (IOException e) {
            throw new Exception(String.format("Failed to copy file [%] to [%]", source, targetDirectory),
                e);
        }
    }

    private void generateConfigurationFiles(File configurationFileTargetDirectory, String version, Database database,
        ArtifactResolver resolver) throws Exception
    {
        VelocityContext context = createVelocityContext(new Properties(), database);
        Artifact artifact = new DefaultArtifact("org.xwiki.platform", "xwiki-platform-tool-configuration-resources",
            JAR, version);
        File configurationJARFile = resolver.resolveArtifact(artifact).getArtifact().getFile();

        configurationFileTargetDirectory.mkdirs();

        try (JarInputStream jarInputStream = new JarInputStream(new FileInputStream(configurationJARFile))) {
            JarEntry entry;
            while ((entry = jarInputStream.getNextJarEntry()) != null) {
                if (entry.getName().endsWith(VM_EXTENSION)) {
                    String fileName = entry.getName().replace(VM_EXTENSION, "");
                    File outputFile = new File(configurationFileTargetDirectory, fileName);
                    LOGGER.debug("  ... Generating: " + outputFile);
                    // Note: Init is done once even if this method is called several times...
                    Velocity.init();
                    try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outputFile))) {
                        Velocity.evaluate(context, writer, "",
                            IOUtils.toString(jarInputStream, StandardCharsets.UTF_8));
                    }
                    jarInputStream.closeEntry();
                }
            }
        } catch (Exception e) {
            throw new Exception("Failed to extract configuration files", e);
        }
    }

    private VelocityContext createVelocityContext(Properties projectProperties, Database database)
    {
        Properties properties = new Properties();
        properties.putAll(getDefaultConfigurationProperties(database));
        for (Object key : projectProperties.keySet()) {
            properties.put(key.toString(), projectProperties.get(key).toString());
        }
        VelocityContext context = new VelocityContext(properties);
        return context;
    }

    private File getJDBCDriver(Database database, ArtifactResolver resolver) throws Exception
    {
        if (database.equals(Database.MYSQL)) {
            Artifact artifact = new DefaultArtifact("mysql", "mysql-connector-java", JAR, "5.1.24");
            return resolver.resolveArtifact(artifact).getArtifact().getFile();
        } else if (database.equals(Database.HSQLDB)) {
            Artifact artifact = new DefaultArtifact("org.hsqldb", "hsqldb", JAR, "2.4.1");
            return resolver.resolveArtifact(artifact).getArtifact().getFile();
        } else {
            throw new RuntimeException(
                String.format("Failed to get JDBC driver. Database [%s] not supported yet!", database));
        }
    }

    private Properties getDefaultConfigurationProperties(Database database)
    {
        Properties props = new Properties();

        // Enable superadmin user
        props.put("xwikiCfgSuperadminPassword", "pass");

        // Default configuration data for hibernate.cfg.xml
        if (database.equals(Database.MYSQL)) {
            props.putAll(getDBProperties(
                "jdbc:mysql://xwikidb:3306/xwiki?useSSL=false",
                DB_USERNAME,
                DB_PASSWORD,
                "com.mysql.jdbc.Driver",
                "org.hibernate.dialect.MySQL5InnoDBDialect"));
        } else if (database.equals(Database.HSQLDB)) {
            props.putAll(getDBProperties(
                "jdbc:hsqldb:file:${environment.permanentDirectory}/database/xwiki_db;shutdown=true",
                "sa",
                "",
                "org.hsqldb.jdbcDriver",
                "org.hibernate.dialect.HSQLDialect"));
        } else {
            throw new RuntimeException(
                String.format("Failed to generate Hibernate config. Database [%s] not supported yet!", database));
        }

        props.setProperty("xwikiDbHbmXwiki", "xwiki.hbm.xml");
        props.setProperty("xwikiDbHbmFeeds", "feeds.hbm.xml");

        // Default configuration data for xwiki.cfg
        props.setProperty("xwikiCfgPlugins",
            "com.xpn.xwiki.plugin.skinx.JsSkinExtensionPlugin,\\"
                + "        com.xpn.xwiki.plugin.skinx.JsSkinFileExtensionPlugin,\\"
                + "        com.xpn.xwiki.plugin.skinx.CssSkinExtensionPlugin,\\"
                + "        com.xpn.xwiki.plugin.skinx.CssSkinFileExtensionPlugin,\\"
                + "        com.xpn.xwiki.plugin.skinx.LinkExtensionPlugin");
        props.setProperty("xwikiCfgVirtualUsepath", "1");
        props.setProperty("xwikiCfgEditCommentMandatory", "0");
        props.setProperty("xwikiCfgDefaultSkin", SKIN);
        props.setProperty("xwikiCfgDefaultBaseSkin", SKIN);
        props.setProperty("xwikiCfgEncoding", "UTF-8");

        // Configure the extension repositories to have only the local maven repository. This is to improve XWiki
        // performances and also to control the build environment so that we don't depend on any external service.
        // We need the local maven repo to provision the XARs from the module being tested.
/*
        props.setProperty("xwikiExtensionRepositories",
            String.format("maven-local:maven:file://%s/.m2/repository", System.getProperty("user.home")));
*/
        return props;
    }

    private Properties getDBProperties(String xwikiDbConnectionUrl, String xwikiDbConnectionUsername,
        String xwikiDbConnectionPassword, String xwikiDbConnectionDriverClass, String xwikiDbDialect)
    {
        Properties props = new Properties();
        props.setProperty("xwikiDbConnectionUrl", xwikiDbConnectionUrl);
        props.setProperty("xwikiDbConnectionUsername", xwikiDbConnectionUsername);
        props.setProperty("xwikiDbConnectionPassword", xwikiDbConnectionPassword);
        props.setProperty("xwikiDbConnectionDriverClass", xwikiDbConnectionDriverClass);
        props.setProperty("xwikiDbDialect", xwikiDbDialect);
        return props;
    }

    private void generateXED(Artifact artifact, File directory, MavenResolver resolver) throws Exception
    {
        Artifact pomArtifact = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), "pom",
            artifact.getVersion());
        Model model = resolver.getModelFromPOMArtifact(pomArtifact);
        File path = new File(directory, artifact.getArtifactId() + '-' + artifact.getBaseVersion() + ".xed");
        this.extensionHelper.serializeExtension(path, model);
    }
}
