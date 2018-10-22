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
 * @since 10.9RC1
 */
public class ConfigurationFilesGenerator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationFilesGenerator.class);

    private static final String JAR = "jar";

    private static final String VM_EXTENSION = ".vm";

    private static final String DB_USERNAME = "xwiki";

    private static final String DB_PASSWORD = DB_USERNAME;

    private static final String SKIN = "flamingo";

    /**
     * @param testConfiguration the configuration (database, debug mode, etc)
     * @param configurationFileTargetDirectory the location where to generate the config files
     * @param version the XWiki version for which to generate config files (used to get the config resources for the
     *        right version)
     * @param resolver the artifact resolver to use (can contain resolved artifacts in cache)
     * @throws Exception if an error occurs during config generation
     */
    public void generate(TestConfiguration testConfiguration, File configurationFileTargetDirectory, String version,
        ArtifactResolver resolver) throws Exception
    {
        VelocityContext context = createVelocityContext(new Properties(), testConfiguration.getDatabase());
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
                    if (testConfiguration.isDebug()) {
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
}
