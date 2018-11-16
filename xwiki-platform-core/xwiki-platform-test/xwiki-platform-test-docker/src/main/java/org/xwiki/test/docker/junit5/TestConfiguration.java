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

import java.util.Properties;

import org.xwiki.test.docker.junit5.browser.Browser;
import org.xwiki.test.docker.junit5.database.Database;
import org.xwiki.test.docker.junit5.servletEngine.ServletEngine;
import org.xwiki.text.StringUtils;

/**
 * Configuration options for the test.
 *
 * @version $Id$
 * @since 10.9
 */
public class TestConfiguration
{
    private static final String DEFAULT = "default";

    private static final String TRUE = "true";

    private static final String FALSE = "false";

    private static final String BROWSER_PROPERTY = "xwiki.test.ui.browser";

    private static final String DATABASE_PROPERTY = "xwiki.test.ui.database";

    private static final String SERVLETENGINE_PROPERTY = "xwiki.test.ui.servletEngine";

    private static final String VERBOSE_PROPERTY = "xwiki.test.ui.verbose";

    private static final String SAVEDBDATA_PROPERTY = "xwiki.test.ui.saveDatabaseData";

    private static final String OFFLINE_PROPERTY = "xwiki.test.ui.offline";

    private static final String DATABASETAG_PROPERTY = "xwiki.test.ui.databaseTag";

    private static final String SERVLETENGINETAG_PROPERTY = "xwiki.test.ui.servletEngineTag";

    private static final String JDBCDRIVERVERSION_PROPERTY = "xwiki.test.ui.jdbcDriverVersion";

    private static final String VNC_PROPERTY = "xwiki.test.ui.vnc";

    private static final String PROPERTIES_PREFIX_PROPERTY = "xwiki.test.ui.properties.";

    private UITest uiTestAnnotation;

    private Browser browser;

    private Database database;

    private ServletEngine servletEngine;

    private boolean verbose;

    private boolean saveDatabaseData;

    private boolean isOffline;

    private String servletEngineTag;

    private String databaseTag;

    private String jdbcDriverVersion;

    private boolean vnc;

    private Properties properties;

    /**
     * @param uiTestAnnotation the annotation from which to extract the configuration
     */
    public TestConfiguration(UITest uiTestAnnotation)
    {
        this.uiTestAnnotation = uiTestAnnotation;
        resolveBrowser();
        resolveDatabase();
        resolveServletEngine();
        resolveVerbose();
        resolveSaveDatabaseData();
        resolveOffline();
        resolveDatabaseTag();
        resolveServletEngineTag();
        resolveJDBCDriverVersion();
        resolveVNC();
        resolveProperties();
    }

    private void resolveBrowser()
    {
        Browser newBrowser = this.uiTestAnnotation.browser();
        if (newBrowser == Browser.SYSTEM) {
            newBrowser = Browser.valueOf(System.getProperty(BROWSER_PROPERTY, Browser.FIREFOX.name()).toUpperCase());
        }
        this.browser = newBrowser;
    }

    private void resolveDatabase()
    {
        Database newDatabase = this.uiTestAnnotation.database();
        if (newDatabase == Database.SYSTEM) {
            newDatabase = Database.valueOf(System.getProperty(DATABASE_PROPERTY,
                Database.HSQLDB_EMBEDDED.name()).toUpperCase());
        }
        this.database = newDatabase;
    }

    private void resolveServletEngine()
    {
        ServletEngine newServletEngine = this.uiTestAnnotation.servletEngine();
        if (newServletEngine == ServletEngine.SYSTEM) {
            newServletEngine = ServletEngine.valueOf(System.getProperty(SERVLETENGINE_PROPERTY,
                ServletEngine.JETTY_STANDALONE.name()).toUpperCase());
        }
        this.servletEngine = newServletEngine;
    }

    private void resolveVerbose()
    {
        boolean newVerbose = this.uiTestAnnotation.verbose();
        if (!newVerbose) {
            newVerbose = Boolean.valueOf(System.getProperty(VERBOSE_PROPERTY, FALSE));
        }
        this.verbose = newVerbose;
    }

    private void resolveSaveDatabaseData()
    {
        boolean newSaveDatabaseData = this.uiTestAnnotation.saveDatabaseData();
        if (!newSaveDatabaseData) {
            newSaveDatabaseData = Boolean.valueOf(System.getProperty(SAVEDBDATA_PROPERTY, FALSE));
        }
        this.saveDatabaseData = newSaveDatabaseData;
    }

    private void resolveOffline()
    {
        boolean newOffline = this.uiTestAnnotation.offline();
        if (!newOffline) {
            newOffline = Boolean.valueOf(System.getProperty(OFFLINE_PROPERTY, FALSE));
        }
        this.isOffline = newOffline;
    }

    private void resolveDatabaseTag()
    {
        String newDatabaseTag = this.uiTestAnnotation.databaseTag();
        if (StringUtils.isEmpty(newDatabaseTag)) {
            newDatabaseTag = System.getProperty(DATABASETAG_PROPERTY);
        }
        this.databaseTag = newDatabaseTag;
    }

    private void resolveServletEngineTag()
    {
        String newServletEngineTag = this.uiTestAnnotation.servletEngineTag();
        if (StringUtils.isEmpty(newServletEngineTag)) {
            newServletEngineTag = System.getProperty(SERVLETENGINETAG_PROPERTY);
        }
        this.servletEngineTag = newServletEngineTag;
    }

    private void resolveJDBCDriverVersion()
    {
        String newJDBCDriverVersion = this.uiTestAnnotation.jdbcDriverVersion();
        if (StringUtils.isEmpty(newJDBCDriverVersion)) {
            newJDBCDriverVersion = System.getProperty(JDBCDRIVERVERSION_PROPERTY);
        }
        this.jdbcDriverVersion = newJDBCDriverVersion;
    }

    private void resolveVNC()
    {
        boolean newVNC = this.uiTestAnnotation.vnc();
        if (newVNC) {
            newVNC = Boolean.valueOf(System.getProperty(VNC_PROPERTY, TRUE));
        }
        this.vnc = newVNC;
    }

    private void resolveProperties()
    {
        String[] propertiesAsArray = this.uiTestAnnotation.properties();
        Properties newProperties = new Properties();
        for (String propertyAsString : propertiesAsArray) {
            int pos = propertyAsString.indexOf('=');
            if (pos > -1) {
                newProperties.setProperty(propertyAsString.substring(0, pos), propertyAsString.substring(pos + 1));
            }
        }
        for (String key : System.getProperties().stringPropertyNames()) {
            if (key.startsWith(PROPERTIES_PREFIX_PROPERTY)) {
                String propertyAsString = StringUtils.substringAfter(key, PROPERTIES_PREFIX_PROPERTY);
                newProperties.setProperty(propertyAsString, System.getProperty(key));
            }
        }
        this.properties = newProperties;
    }

    /**
     * @return the browser to use
     */
    public Browser getBrowser()
    {
        return this.browser;
    }

    /**
     * @return the database to use
     */
    public Database getDatabase()
    {
        return this.database;
    }

    /**
     * @return the Servlet engine to use
     */
    public ServletEngine getServletEngine()
    {
        return this.servletEngine;
    }

    /**
     * @return true if the test should output verbose console logs or not
     */
    public boolean isVerbose()
    {
        return this.verbose;
    }

    /**
     * @return true true if the database data should be mapped to a local directory on the host computer so that it can
     *         be saved and reused for another run
     * @since 10.10RC1
     */
    public boolean isDatabaseDataSaved()
    {
        return this.saveDatabaseData;
    }

    /**
     * @return true if the Maven resolving is done in offline mode (i.e. you need to have the required artifacts in
     *         your local repository). False by default to avoid developer problems but should be set to true in the
     *         CI to improve performance of functional tests
     * @since 10.10RC1
     */
    public boolean isOffline()
    {
        return this.isOffline;
    }

    /**
     * @return the docker image tag to use (if not specified, uses the default from TestContainers)
     * @since 10.10RC1
     */
    public String getDatabaseTag()
    {
        return this.databaseTag;
    }

    /**
     * @return the docker image tag to use (if not specified, uses the "latest" tag)
     * @since 10.10RC1
     */
    public String getServletEngineTag()
    {
        return this.servletEngineTag;
    }

    /**
     * @return the version of the JDBC driver to use for the selected database (if not specified, uses a default version
     *         depending on the database)
     * @since 10.10RC1
     */
    public String getJDBCDriverVersion()
    {
        return this.jdbcDriverVersion;
    }

    /**
     * @return true if VNC container is started and recording is done and saved on test exit
     * @since 10.10RC1
     */
    public boolean vnc()
    {
        return this.vnc;
    }

    /**
     * @return the list of configuration properties to use when generating the XWiki configuration files such as
     *         as {@code xwiki.properties} (check {@code xwiki.properties.vm} to find the list of supported properties)
     * @since 10.10RC1
     */
    public Properties getProperties()
    {
        return this.properties;
    }

    /**
     * @return the String representation of the configuration (used for example as a directory name where to save the
     *         generated XWiki configuration - XWiki WAR file, etc)
     * @since 10.10RC1
     */
    public String getName()
    {
        return String.format("%s-%s-%s-%s-%s-%s",
            getDatabase().name().toLowerCase(),
            StringUtils.isEmpty(getDatabaseTag()) ? DEFAULT : getDatabaseTag(),
            StringUtils.isEmpty(getJDBCDriverVersion()) ? DEFAULT : getDatabaseTag(),
            getServletEngine().name().toLowerCase(),
            StringUtils.isEmpty(getServletEngineTag()) ? DEFAULT : getServletEngineTag(),
            getBrowser().name().toLowerCase());
    }

    /**
     * @return the output directory where to output files required for running the tests. If the {@code maven.build.dir}
     *         system property is not defined then construct an output directory name based on the defined configuration
     *         so that we can run different configurations one after another without them overriding each other.
     *         The {@code maven.build.dir} system property is there to allow controlling where the Maven output
     *         directory is located when running from Maven.
     */
    public String getOutputDirectory()
    {
        String outputDirectory;
        String mavenBuildDir = System.getProperty("maven.build.dir");
        if (mavenBuildDir == null) {
            outputDirectory = String.format("./target/%s", getName());
        } else {
            outputDirectory = mavenBuildDir;
        }
        return outputDirectory;
    }
}
