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
import java.util.Arrays;
import java.util.List;
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
        VelocityContext context = createVelocityContext(new Properties());
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
                    if (this.testConfiguration.isDebug()) {
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

    private VelocityContext createVelocityContext(Properties projectProperties)
    {
        Properties properties = new Properties();
        properties.putAll(getDefaultConfigurationProperties());
        for (Object key : projectProperties.keySet()) {
            properties.put(key.toString(), projectProperties.get(key).toString());
        }
        VelocityContext context = new VelocityContext(properties);
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

        // Configure the extension repositories to have only the local maven repository. This is to improve XWiki
        // performances and also to control the build environment so that we don't depend on any external service.
        // We need the local maven repo to provision the XARs from the module being tested.
        // Do this only if we're offline, otherwise fetch from the usual repos including the remote snapshot repo in
        // order to get the latest version.
        if (this.repositoryResolver.getSession().isOffline()) {
            // If we're inside a docker container, the local Maven repo is at /root/.m2/repository. This is
            // configured in ServletContainerExecutor.
            String localRepo;
            if (this.testConfiguration.getServletEngine().isOutsideDocker()) {
                localRepo = this.repositoryResolver.getSession().getLocalRepository().getBasedir().toString();
            } else {
                localRepo = "/root/.m2/repository";
            }
            props.setProperty("xwikiExtensionRepositories", String.format("maven-local:maven:file://%s", localRepo));
        }

        // TODO: Allow users to provide properties that will override the default here....

        return props;
    }

    private Properties getDatabaseConfigurationProperties()
    {
        Properties props = new Properties();

        // Default configuration data for hibernate.cfg.xml
        String ipAddress = this.testConfiguration.getDatabase().getIpAddress();
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
