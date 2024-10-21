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
package org.xwiki.test.docker.internal.junit5.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.test.docker.internal.junit5.DockerTestUtils;
import org.xwiki.test.docker.junit5.DockerTestException;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.database.Database;
import org.xwiki.test.integration.maven.ArtifactResolver;
import org.xwiki.test.integration.maven.RepositoryResolver;

/**
 * Generate XWiki config files for a given database and a given version of XWiki.
 *
 * @version $Id$
 * @since 10.9
 */
public class ConfigurationFilesGenerator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationFilesGenerator.class);

    private static final String JAR = "jar";

    private static final String VM_EXTENSION = ".vm";

    private static final String DB_USERNAME = "xwiki";

    private static final String DB_PASSWORD = DB_USERNAME;

    private static final String SKIN = "flamingo";

    private static final String LOGBACK_FILE = "logback.xml";

    private static final String LOGBACK_OVERRIDE_FILE = "logback-override.xml";

    private TestConfiguration testConfiguration;

    private RepositoryResolver repositoryResolver;

    private PropertiesMerger propertiesMerger = new PropertiesMerger();

    /**
     * @param testConfiguration the configuration to build (database, debug mode, etc)
     * @param repositoryResolver the resolver to create Maven repositories and sessions
     */
    public ConfigurationFilesGenerator(TestConfiguration testConfiguration, RepositoryResolver repositoryResolver)
    {
        this.testConfiguration = testConfiguration;
        this.repositoryResolver = repositoryResolver;
    }

    /**
     * @param configurationFileTargetDirectory the location where to generate the config files
     * @param version the XWiki version for which to generate config files (used to get the config resources for the
     * right version)
     * @param resolver the artifact resolver to use (can contain resolved artifacts in cache)
     * @throws Exception if an error occurs during config generation
     */
    public void generate(File configurationFileTargetDirectory, String version, ArtifactResolver resolver)
        throws Exception
    {
        VelocityContext context = createVelocityContext();
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
                    if (this.testConfiguration.isVerbose()) {
                        LOGGER.info("... Generating: {}", outputFile);
                    }
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

        // Copy a logback config file for testing. This allows putting overrides in it that are needed only for the
        // tests. Only do this in the CI for now (or if verbose is true) since this is currently used for debugging
        // problems.
        if (DockerTestUtils.isInAContainer() || this.testConfiguration.isVerbose()) {
            copyLogbackConfigFile(configurationFileTargetDirectory);
        }
    }

    private void copyLogbackConfigFile(File configurationFileTargetDirectory) throws Exception
    {
        File outputDirectory = new File(configurationFileTargetDirectory, "classes");
        File outputFile = new File(outputDirectory, LOGBACK_FILE);
        if (this.testConfiguration.isVerbose()) {
            LOGGER.info("... Generating logging configuration: {}", outputFile);
        }
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            // Allows modules to override the default logback config by providing a LOGBACK_OVERRIDE_FILE file in
            // src/it/test/resources.
            InputStream is = getClass().getClassLoader().getResourceAsStream(LOGBACK_OVERRIDE_FILE);
            if (is == null) {
                is = getClass().getClassLoader().getResourceAsStream(LOGBACK_FILE);
            }
            IOUtils.copy(is, fos);
        }
    }

    private VelocityContext createVelocityContext() throws DockerTestException
    {
        Properties properties = this.propertiesMerger.merge(getDefaultConfigurationProperties(),
            this.testConfiguration.getProperties(), true);
        return new VelocityContext((Map) properties);
    }

    private Properties getDefaultConfigurationProperties()
    {
        Properties props = new Properties();

        // Enable superadmin user
        props.put("xwikiCfgSuperadminPassword", "pass");

        // Default configuration data for hibernate.cfg.xml
        props.putAll(getDatabaseConfigurationProperties());

        // Default configuration data for xwiki.cfg
        props.setProperty("xwikiCfgPlugins",
            "com.xpn.xwiki.plugin.skinx.JsSkinExtensionPlugin,\\"
                + "        com.xpn.xwiki.plugin.skinx.JsSkinFileExtensionPlugin,\\"
                + "        com.xpn.xwiki.plugin.skinx.JsResourceSkinExtensionPlugin,\\"
                + "        com.xpn.xwiki.plugin.skinx.CssSkinExtensionPlugin,\\"
                + "        com.xpn.xwiki.plugin.skinx.CssSkinFileExtensionPlugin,\\"
                + "        com.xpn.xwiki.plugin.skinx.CssResourceSkinExtensionPlugin,\\"
                + "        com.xpn.xwiki.plugin.skinx.LinkExtensionPlugin");
        props.setProperty("xwikiCfgVirtualUsepath", "1");
        props.setProperty("xwikiCfgEditCommentMandatory", "0");
        props.setProperty("xwikiCfgDefaultSkin", SKIN);
        props.setProperty("xwikiCfgDefaultBaseSkin", SKIN);
        props.setProperty("xwikiCfgEncoding", "UTF-8");

        // Default configuration data for xwiki.properties

        // Configure the extension repositories.
        // We configure the local maven repo to be the first repo in the list for performance reasons.
        // If we're offline then only configure the local maven repo. This is to improve XWiki performances and also
        // to control the build environment so that we don't depend on any external service.

        // If we're inside a docker container, the local Maven repo is at /root/.m2/repository. This is
        // configured in ServletContainerExecutor.
        List<String> repositories = new ArrayList<>();
        String localRepo;
        if (this.testConfiguration.getServletEngine().isOutsideDocker()) {
            localRepo = this.repositoryResolver.getSession().getLocalRepository().getBasedir().toString();
        } else {
            localRepo = "/root/.m2/repository";
        }
        repositories.add(String.format("maven-local:maven:file://%s", localRepo));

        if (!this.repositoryResolver.getSession().isOffline()) {
            repositories.add("maven-xwiki:maven:https://nexus-snapshots.xwiki.org/repository/public-proxy");
            // Allow snapshot extensions to be resolved too when not offline
            // Note that the xwiki-commons-extension-repository-maven-snapshots artifact is added in
            // WARBuilder when resolving distribution artifacts.
            repositories.add(
                "maven-xwiki-snapshot:maven:https://nexus-snapshots.xwiki.org/repository/snapshots");
        }

        props.setProperty("xwikiExtensionRepositories", StringUtils.join(repositories, ','));

        // Set the permanent directory but only when using docker as otherwise it's already available locally.
        if (!this.testConfiguration.getServletEngine().isOutsideDocker()) {
            props.setProperty("xwikiPropertiesEnvironmentPermanentDirectory",
                this.testConfiguration.getServletEngine().getPermanentDirectory());
        }

        // Disable the Distribution Wizard by default (so that test-generated distributions that include the
        // xwiki-platform-extension-distribution JAR dependency don't get the DW by default; as a consequence the
        // main and subwikis will be empty by default). If you need to test the DW, set these properties to true.
        props.setProperty("xwikiPropertiesAutomaticStartOnMainWiki", Boolean.FALSE.toString());
        props.setProperty("xwikiPropertiesAutomaticStartOnWiki", Boolean.FALSE.toString());

        return props;
    }

    private Properties getDatabaseConfigurationProperties()
    {
        Properties props = new Properties();

        // Default configuration data for hibernate.cfg.xml
        String ipAddress = this.testConfiguration.getDatabase().getIP();
        int port = this.testConfiguration.getDatabase().getPort();
        if (this.testConfiguration.getDatabase().equals(Database.MYSQL)) {
            props.putAll(getDBProperties(Arrays.asList(
                "mysql",
                String.format("jdbc:mysql://%s:%s/xwiki?useSSL=false", ipAddress, port),
                DB_USERNAME,
                DB_PASSWORD,
                "com.mysql.cj.jdbc.Driver",
                null,
                null,
                null,
                null)));
        } else if (this.testConfiguration.getDatabase().equals(Database.MARIADB)) {
            props.putAll(getDBProperties(Arrays.asList(
                "mariadb",
                String.format("jdbc:mariadb://%s:%s/xwiki?useSSL=false", ipAddress, port),
                DB_USERNAME,
                DB_PASSWORD,
                "org.mariadb.jdbc.Driver",
                null,
                null,
                null,
                null)));

        } else if (this.testConfiguration.getDatabase().equals(Database.POSTGRESQL)) {
            props.putAll(getDBProperties(Arrays.asList(
                "pgsql",
                String.format("jdbc:postgresql://%s:%s/xwiki", ipAddress, port),
                DB_USERNAME,
                DB_PASSWORD,
                "org.postgresql.Driver",
                null,
                "schema",
                "xwiki.postgresql.hbm.xml",
                null)));
        } else if (this.testConfiguration.getDatabase().equals(Database.HSQLDB_EMBEDDED)) {
            props.putAll(getDBProperties(Arrays.asList(
                "hsqldb",
                "jdbc:hsqldb:file:${environment.permanentDirectory}/database/xwiki_db;shutdown=true",
                "sa",
                "",
                "org.hsqldb.jdbcDriver",
                null,
                null,
                null,
                null)));
        } else if (this.testConfiguration.getDatabase().equals(Database.ORACLE)) {
            props.putAll(getDBProperties(Arrays.asList(
                "oracle",
                String.format("jdbc:oracle:thin:@%s:%s/XWIKI", ipAddress, port),
                DB_USERNAME,
                DB_PASSWORD,
                "oracle.jdbc.driver.OracleDriver",
                null,
                null,
                "xwiki.oracle.hbm.xml",
                "feeds.oracle.hbm.xml")));
        } else {
            throw new RuntimeException(
                String.format("Failed to generate Hibernate config. Database [%s] not supported yet!",
                    this.testConfiguration.getDatabase()));
        }

        return props;
    }

    private Properties getDBProperties(List<String> dbProperties)
    {
        Properties props = new Properties();
        props.setProperty("xwikiDb", dbProperties.get(0));
        props.setProperty("xwikiDbConnectionUrl", dbProperties.get(1));
        props.setProperty("xwikiDbConnectionUsername", dbProperties.get(2));
        props.setProperty("xwikiDbConnectionPassword", dbProperties.get(3));
        props.setProperty("xwikiDbConnectionDriverClass", dbProperties.get(4));
        if (dbProperties.get(5) != null) {
            props.setProperty("xwikiDbDialect", dbProperties.get(5));
        }
        if (dbProperties.get(6) != null) {
            props.setProperty("xwikiDbVirtualMode", dbProperties.get(6));
        }
        String xwikiDbHbmXwiki;
        if (dbProperties.get(7) != null) {
            xwikiDbHbmXwiki = dbProperties.get(7);
        } else {
            xwikiDbHbmXwiki = "xwiki.hbm.xml";
        }
        props.setProperty("xwikiDbHbmXwiki", xwikiDbHbmXwiki);
        String xwikiDbHbmFeeds;
        if (dbProperties.get(8) != null) {
            xwikiDbHbmFeeds = dbProperties.get(8);
        } else {
            xwikiDbHbmFeeds = "feeds.hbm.xml";
        }
        props.setProperty("xwikiDbHbmFeeds", xwikiDbHbmFeeds);

        // Increase the connection pool size since it appears that on slow CI agents, the default 50 connections is too
        // small, especially at startup. I haven't proved it but I think we're doing more stuff at startup that require
        // DB connections. compared  to in the past. When the tests succeed, xwiki starts in about 1mn20s. When they
        // fail xwiki takes over 5mn to start, showing how slow the machine is. When it succeeds the connection cool
        // is used up to 40 connections (out of 50) so already close to the max. When it fails, it goes quickly to 50.
        // Basically the connections are not released fast enough becauase the SQL queries take longer to execute.
        // Thus trying with 300 max connections to see if that's the problem.
        props.setProperty("xwikiDbDbcpMaxTotal", "300");

        // Set the xwikiDbDbcpMaxOpenPreparedStatements to unlimited (default) since we saw the message
        // "Data source rejected establishment of connection,  message from server: "Too many connections"" and at
        // that time the number of connections (95) was well under the maximum allowed (300). Thus, as a hunch, we're
        // trying to increase the pooled statement maximum too.
        props.setProperty("xwikiDbDbcpMaxOpenPreparedStatements", "-1");

        return props;
    }
}
