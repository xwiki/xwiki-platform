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
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.test.docker.junit5.database.Database;
import org.xwiki.text.StringUtils;

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

    private TestConfiguration testConfiguration;

    private RepositoryResolver repositoryResolver;

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
     *        right version)
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
                        LOGGER.info("... Generating: " + outputFile);
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
    }

    private VelocityContext createVelocityContext()
    {
        Properties properties = new Properties();

        // Add default properties
        properties.putAll(getDefaultConfigurationProperties());

        // Add user-specified properties (with possible overrides for default properties)
        properties.putAll(this.testConfiguration.getProperties());

        VelocityContext context = new VelocityContext((Map) properties);
        return context;
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
                + "        com.xpn.xwiki.plugin.skinx.CssSkinExtensionPlugin,\\"
                + "        com.xpn.xwiki.plugin.skinx.CssSkinFileExtensionPlugin,\\"
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
            repositories.add("maven-xwiki:maven:http://nexus.xwiki.org/nexus/content/groups/public");
            // Allow snapshot extensions to be resolved too when not offline
            // Note that the xwiki-commons-extension-repository-maven-snapshots artifact is added in
            // WARBuilder when resolving distribution artifacts.
            repositories.add("maven-xwiki-snapshot:maven:http://nexus.xwiki.org/nexus/content/groups/public-snapshots");
        }

        props.setProperty("xwikiExtensionRepositories", StringUtils.join(repositories, ','));

        // Set the permanent directory but only when using docker as otherwise it's already available locally.
        if (!this.testConfiguration.getServletEngine().isOutsideDocker()) {
            props.setProperty("xwikiPropertiesEnvironmentPermanentDirectory",
                this.testConfiguration.getServletEngine().getPermanentDirectory());
        }

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
                "com.mysql.jdbc.Driver",
                "org.hibernate.dialect.MySQL5InnoDBDialect",
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
                "org.hibernate.dialect.PostgreSQLDialect",
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
                "org.hibernate.dialect.HSQLDialect",
                null,
                null,
                null)));
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
        props.setProperty("xwikiDbDialect", dbProperties.get(5));
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

        return props;
    }
}
