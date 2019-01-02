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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final Pattern ARTIFACT_COORD_PATTERN = Pattern.compile("([^: ]+):([^: ]+)");

    private static final String DEFAULT = "default";

    private static final String BROWSER_PROPERTY = "xwiki.test.ui.browser";

    private static final String DATABASE_PROPERTY = "xwiki.test.ui.database";

    private static final String SERVLETENGINE_PROPERTY = "xwiki.test.ui.servletEngine";

    private static final String VERBOSE_PROPERTY = "xwiki.test.ui.verbose";

    private static final String DEBUG_PROPERTY = "xwiki.test.ui.debug";

    private static final String SAVEDBDATA_PROPERTY = "xwiki.test.ui.saveDatabaseData";

    private static final String OFFLINE_PROPERTY = "xwiki.test.ui.offline";

    private static final String DATABASETAG_PROPERTY = "xwiki.test.ui.databaseTag";

    private static final String SERVLETENGINETAG_PROPERTY = "xwiki.test.ui.servletEngineTag";

    private static final String JDBCDRIVERVERSION_PROPERTY = "xwiki.test.ui.jdbcDriverVersion";

    private static final String VNC_PROPERTY = "xwiki.test.ui.vnc";

    private static final String PROPERTIES_PREFIX_PROPERTY = "xwiki.test.ui.properties.";

    private static final String PROFILES_PROPERTY = "xwiki.test.ui.profiles";

    private static final String OFFICE_PROPERTY = "xwiki.test.ui.office";

    private UITest uiTestAnnotation;

    private Browser browser;

    private Database database;

    private ServletEngine servletEngine;

    private boolean verbose;

    private boolean debug;

    private boolean saveDatabaseData;

    private boolean isOffline;

    private String servletEngineTag;

    private String databaseTag;

    private String jdbcDriverVersion;

    private boolean vnc;

    private Properties properties;

    private List<List<String>> extraJARs;

    private List<Integer> sshPorts;

    private List<String> profiles;

    private boolean office;

    private List<ServletEngine> forbiddenServletEngines;

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
        resolveDebug();
        resolveSaveDatabaseData();
        resolveOffline();
        resolveDatabaseTag();
        resolveServletEngineTag();
        resolveJDBCDriverVersion();
        resolveVNC();
        resolveProperties();
        resolveExtraJARs();
        resolveSSHPorts();
        resolveProfiles();
        resolveOffice();
        resolveForbiddenServletEngines();
    }

    /**
     * Resolve the passed Enum property by getting the value from the System property and if not found, from the
     * {@link UITest} annotation, and fallbacking to the passed default value if not found.
     *
     * @param enumType the type of the enum for which we want to resolve the value.
     * @param annotationValue the {@link UITest} annotation parameter value to use if no System property is defined
     * @param propertyName the name of the System property key which might contain a value for this enum.
     * @param <T> type of the value necessarily extends enum.
     * @return the resolved value following the strategy described above.
     */
    private <T extends Enum> T resolve(Class<T> enumType, T annotationValue, String propertyName)
    {
        T result = annotationValue;
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null) {
            result = (T) Enum.valueOf(enumType, propertyValue.toUpperCase());
        }
        return result;
    }

    /**
     * Resolve the passed Boolean property by getting the value from the System property and if not found, from the
     * {@link UITest} annotation, and fallbacking to the passed default value if not found.
     *
     * @param annotationValue the {@link UITest} annotation parameter value to use if no System property is defined
     * @param propertyName the name of the System property key which might contain a value for this boolean.
     * @return the resolved value following the strategy described above.
     */
    private Boolean resolve(Boolean annotationValue, String propertyName)
    {
        Boolean result = annotationValue;
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null) {
            result = Boolean.valueOf(propertyValue);
        }
        return result;
    }

    /**
     * Resolve the passed String property by getting the value from the System property and if not found, from the
     * {@link UITest} annotation, and fallbacking to the passed default value if not found.
     *
     * @param annotationValue the {@link UITest} annotation parameter value to use if no System property is defined
     * @param propertyName the name of the System property key which might contain a value for this string.
     * @return the resolved value following the strategy described above.
     */
    private String resolve(String annotationValue, String propertyName)
    {
        String result = StringUtils.isEmpty(annotationValue) ? null : annotationValue;
        String propertyValue = System.getProperty(propertyName);
        if (!StringUtils.isEmpty(propertyValue)) {
            result = propertyValue;
        }
        return result;
    }

    private void resolveBrowser()
    {
        this.browser = resolve(Browser.class, this.uiTestAnnotation.browser(), BROWSER_PROPERTY);
    }

    private void resolveDatabase()
    {
        this.database = resolve(Database.class, this.uiTestAnnotation.database(), DATABASE_PROPERTY);
    }

    private void resolveServletEngine()
    {
        this.servletEngine = resolve(ServletEngine.class, this.uiTestAnnotation.servletEngine(),
            SERVLETENGINE_PROPERTY);
    }

    private void resolveVerbose()
    {
        this.verbose = resolve(this.uiTestAnnotation.verbose(), VERBOSE_PROPERTY);
    }

    private void resolveDebug()
    {
        this.debug = resolve(this.uiTestAnnotation.debug(), DEBUG_PROPERTY);
    }

    private void resolveSaveDatabaseData()
    {
        this.saveDatabaseData = resolve(this.uiTestAnnotation.saveDatabaseData(), SAVEDBDATA_PROPERTY);
    }

    private void resolveOffline()
    {
        this.isOffline = resolve(this.uiTestAnnotation.offline(), OFFLINE_PROPERTY);
    }

    private void resolveDatabaseTag()
    {
        this.databaseTag = resolve(this.uiTestAnnotation.databaseTag(), DATABASETAG_PROPERTY);
    }

    private void resolveServletEngineTag()
    {
        this.servletEngineTag = resolve(this.uiTestAnnotation.servletEngineTag(), SERVLETENGINETAG_PROPERTY);
    }

    private void resolveJDBCDriverVersion()
    {
        this.jdbcDriverVersion = resolve(this.uiTestAnnotation.jdbcDriverVersion(), JDBCDRIVERVERSION_PROPERTY);
    }

    private void resolveVNC()
    {
        this.vnc = resolve(this.uiTestAnnotation.vnc(), VNC_PROPERTY);
    }

    private void resolveOffice()
    {
        this.office = resolve(this.uiTestAnnotation.office(), OFFICE_PROPERTY);
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

    private void resolveExtraJARs()
    {
        List<List<String>> newExtraJARs = new ArrayList<>();
        for (String coordinate : this.uiTestAnnotation.extraJARs()) {
            Matcher matcher = ARTIFACT_COORD_PATTERN.matcher(coordinate);
            if (!matcher.matches()) {
                throw new IllegalArgumentException(String.format("Bad artifact coordinates [%s]", coordinate));
            }
            List<String> jarCoordinates = new ArrayList<>();
            jarCoordinates.add(matcher.group(1));
            jarCoordinates.add(matcher.group(2));
            newExtraJARs.add(jarCoordinates);
        }
        this.extraJARs = newExtraJARs;
    }

    private void resolveSSHPorts()
    {
        List<Integer> newSSHPorts = new ArrayList<>();
        newSSHPorts.add(8080);
        for (int sshPort : this.uiTestAnnotation.sshPorts()) {
            newSSHPorts.add(sshPort);
        }
        this.sshPorts = newSSHPorts;
    }

    private void resolveProfiles()
    {
        List<String> newProfiles = new ArrayList<>();
        if (this.uiTestAnnotation.profiles().length > 0) {
            newProfiles.addAll(Arrays.asList(this.uiTestAnnotation.profiles()));
        } else {
            newProfiles.addAll(Arrays.asList(System.getProperty(PROFILES_PROPERTY, "").split(",")));
        }
        this.profiles = newProfiles;
    }

    private void resolveForbiddenServletEngines()
    {
        List<ServletEngine> newForbiddenServletEngines = new ArrayList<>();
        if (this.uiTestAnnotation.forbiddenEngines().length > 0) {
            newForbiddenServletEngines.addAll(Arrays.asList(this.uiTestAnnotation.forbiddenEngines()));
        }
        this.forbiddenServletEngines = newForbiddenServletEngines;
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
     * @return true if the test should output debug console logs or not
     * @since 10.11RC1
     */
    public boolean isDebug()
    {
        return this.debug;
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
     * @return the list of extra JARs to add to the {@code WEB-INF/lib} directory, specified as a List of Strings in
     *         the following order: group id, artifact id.
     * @since 10.11RC1
     */
    public List<List<String>> getExtraJARs()
    {
        return this.extraJARs;
    }

    /**
     * @return the list of ports that should be SSH-forwarded when connecting from a Docker container to the
     *         host (i.e. when using the {@code host.testcontainers.internal} host name). This is in addition to port
     *         {@code 8080} which is always added. For example if you need XWiki to send a mail to a SMTP server
     *         running on port 3025 on the host, you should add port 3025 to the list.
     * @since 10.11RC1
     */
    public List<Integer> getSSHPorts()
    {
        return this.sshPorts;
    }

    /**
     * @return the list of Maven profiles to activate when resolving dependencies for the current POM.
     * @since 10.11RC1
     */
    public List<String> getProfiles()
    {
        return this.profiles;
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

    /**
     * @return true if an office container must be provided to run the tests.
     * @since 10.11RC1
     */
    public boolean isOffice()
    {
        return this.office;
    }

    /**
     * @return the list of Servlet Engines on which this test must not be executed. If the Servlet Engine is selected
     *         then the test will be skipped
     * @since 10.11RC1
     */
    public List<ServletEngine> getForbiddenServletEngines()
    {
        return this.forbiddenServletEngines;
    }
}
