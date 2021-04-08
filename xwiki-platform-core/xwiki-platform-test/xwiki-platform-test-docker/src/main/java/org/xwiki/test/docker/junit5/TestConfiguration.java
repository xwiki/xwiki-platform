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
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.extension.Extension;
import org.xwiki.test.docker.internal.junit5.DockerTestUtils;
import org.xwiki.test.docker.internal.junit5.configuration.PropertiesMerger;
import org.xwiki.test.docker.junit5.browser.Browser;
import org.xwiki.test.docker.junit5.database.Database;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.integration.maven.ArtifactCoordinate;
import org.xwiki.tool.extension.ExtensionOverride;

/**
 * Configuration options for the test.
 *
 * @version $Id$
 * @since 10.9
 */
public class TestConfiguration
{
    private static final String DEFAULT = "default";

    private static final String BROWSER_PROPERTY = "xwiki.test.ui.browser";

    private static final String DATABASE_PROPERTY = "xwiki.test.ui.database";

    private static final String DATABASE_PREFIX_COMMAND = "xwiki.test.ui.database.commands.";

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

    private boolean offline;

    private String servletEngineTag;

    private String databaseTag;

    private String jdbcDriverVersion;

    private boolean vnc;

    private Properties properties;

    private List<ExtensionOverride> extensionOverrides;

    private List<ArtifactCoordinate> extraJARs;

    private boolean resolveExtraJARs;

    private List<Integer> sshPorts;

    private List<String> profiles;

    private boolean office;

    private List<ServletEngine> forbiddenServletEngines;

    private Properties databaseCommands;

    private PropertiesMerger propertiesMerger = new PropertiesMerger();

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
        resolveResolveExtraJARs();
        resolveExtensionOverrides();
        resolveSSHPorts();
        resolveProfiles();
        resolveOffice();
        resolveForbiddenServletEngines();
        resolveDatabaseCommands();
    }

    /**
     * @param testConfiguration the configuration to merge with the current one
     */
    public void merge(TestConfiguration testConfiguration) throws DockerTestException
    {
        mergeBrowser(testConfiguration.getBrowser());
        mergeDatabase(testConfiguration.getDatabase());
        mergeServletEngine(testConfiguration.getServletEngine());
        mergeVerbose(testConfiguration.isVerbose());
        mergeDebug(testConfiguration.isDebug());
        mergeSaveDatabaseData(testConfiguration.isDatabaseDataSaved());
        mergeOffline(testConfiguration.isOffline());
        mergeDatabaseTag(testConfiguration.getDatabaseTag());
        mergeServletEngineTag(testConfiguration.getServletEngineTag());
        mergeJDBCDriverVersion(testConfiguration.getJDBCDriverVersion());
        mergeVNC(testConfiguration.vnc());
        mergeProperties(testConfiguration.getProperties());
        mergeExtraJARs(testConfiguration.getExtraJARs());
        mergeResolveExtraJARs(testConfiguration.isResolveExtraJARs());
        mergeExtensionOverrides(testConfiguration.getExtensionOverrides());
        mergeSSHPorts(testConfiguration.getSSHPorts());
        mergeProfiles(testConfiguration.getProfiles());
        mergeOffice(testConfiguration.isOffice());
        mergeForbiddenServletEngines(testConfiguration.getForbiddenServletEngines());
        mergeDatabaseCommands(testConfiguration.getDatabaseCommands());
    }

    /**
     * Resolve the passed Enum property by getting the value from the System property and if not found, from the {@link
     * UITest} annotation, and fallbacking to the passed default value if not found.
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

    private void mergeBrowser(Browser browser) throws DockerTestException
    {
        if (getBrowser() != null) {
            if (browser != null && !getBrowser().equals(browser)) {
                throw new DockerTestException(
                    String.format("Cannot merge browser [%s] since it was already specified as [%s]", browser,
                        getBrowser()));
            } else {
                this.browser = getBrowser();
            }
        } else {
            this.browser = browser;
        }
    }

    private void resolveDatabase()
    {
        this.database = resolve(Database.class, this.uiTestAnnotation.database(), DATABASE_PROPERTY);
    }

    private void mergeDatabase(Database database) throws DockerTestException
    {
        if (getDatabase() != null) {
            if (database != null && !getDatabase().equals(database)) {
                throw new DockerTestException(
                    String.format("Cannot merge database [%s] since it was already specified as [%s]", database,
                        getDatabase()));
            } else {
                this.database = getDatabase();
            }
        } else {
            this.database = database;
        }
    }

    private void resolveServletEngine()
    {
        this.servletEngine = resolve(ServletEngine.class, this.uiTestAnnotation.servletEngine(),
            SERVLETENGINE_PROPERTY);
    }

    private void mergeServletEngine(ServletEngine servletEngine) throws DockerTestException
    {
        if (getServletEngine() != null) {
            if (servletEngine != null && !getServletEngine().equals(servletEngine)) {
                throw new DockerTestException(
                     String.format("Cannot merge Servlet engine [%s] since it was already specified as [%s]",
                     servletEngine, getServletEngine()));
            } else {
                this.servletEngine = getServletEngine();
            }
        } else {
            this.servletEngine = servletEngine;
        }
    }

    private void resolveVerbose()
    {
        boolean isVerbose;
        // Always display verbose logs for debugging when inside a container.
        if (DockerTestUtils.isInAContainer()) {
            isVerbose = true;
        } else {
            isVerbose = resolve(this.uiTestAnnotation.verbose(), VERBOSE_PROPERTY);
        }
        this.verbose = isVerbose;
    }

    private void mergeVerbose(boolean verbose)
    {
        if (!isVerbose() && verbose) {
            this.verbose = true;
        }
    }

    private void resolveDebug()
    {
        this.debug = resolve(this.uiTestAnnotation.debug(), DEBUG_PROPERTY);
    }

    private void mergeDebug(boolean debug)
    {
        if (!isDebug() && debug) {
            this.debug = true;
        }
    }

    private void resolveSaveDatabaseData()
    {
        this.saveDatabaseData = resolve(this.uiTestAnnotation.saveDatabaseData(), SAVEDBDATA_PROPERTY);
    }

    private void mergeSaveDatabaseData(boolean saveDatabaseData)
    {
        if (!isDatabaseDataSaved() && saveDatabaseData) {
            this.saveDatabaseData = true;
        }
    }

    private void resolveOffline()
    {
        this.offline = resolve(this.uiTestAnnotation.offline(), OFFLINE_PROPERTY);
    }

    private void mergeOffline(boolean offline)
    {
        if (!isOffline() && offline) {
            this.offline = true;
        }
    }

    private void resolveDatabaseTag()
    {
        this.databaseTag = resolve(this.uiTestAnnotation.databaseTag(), DATABASETAG_PROPERTY);
    }

    private void mergeDatabaseTag(String databaseTag) throws DockerTestException
    {
        if (getDatabaseTag() != null) {
            if (databaseTag != null && !getDatabaseTag().equals(databaseTag)) {
                throw new DockerTestException(
                    String.format("Cannot merge database tag [%s] since it was already specified as [%s]",
                        databaseTag, getDatabaseTag()));
            } else {
                this.databaseTag = getDatabaseTag();
            }
        } else {
            this.databaseTag = databaseTag;
        }
    }

    private void resolveServletEngineTag()
    {
        this.servletEngineTag = resolve(this.uiTestAnnotation.servletEngineTag(), SERVLETENGINETAG_PROPERTY);
    }

    private void mergeServletEngineTag(String servletEngineTag) throws DockerTestException
    {
        if (getServletEngineTag() != null) {
            if (servletEngineTag != null && !getServletEngineTag().equals(servletEngineTag)) {
                throw new DockerTestException(
                    String.format("Cannot merge Servlet engine tag [%s] since it was already specified as [%s]",
                        servletEngineTag, getServletEngineTag()));
            } else {
                this.servletEngineTag = getServletEngineTag();
            }
        } else {
            this.servletEngineTag = servletEngineTag;
        }
    }

    private void resolveJDBCDriverVersion()
    {
        this.jdbcDriverVersion = resolve(this.uiTestAnnotation.jdbcDriverVersion(), JDBCDRIVERVERSION_PROPERTY);
    }

    private void mergeJDBCDriverVersion(String jdbcDriverVersion) throws DockerTestException
    {
        if (getJDBCDriverVersion() != null) {
            if (jdbcDriverVersion != null && !getJDBCDriverVersion().equals(jdbcDriverVersion)) {
                throw new DockerTestException(
                    String.format("Cannot merge JDBC driver version [%s] since it was already specified as [%s]",
                        jdbcDriverVersion, getJDBCDriverVersion()));
            } else {
                this.jdbcDriverVersion = getJDBCDriverVersion();
            }
        } else {
            this.jdbcDriverVersion = jdbcDriverVersion;
        }
    }

    private void resolveVNC()
    {
        this.vnc = resolve(this.uiTestAnnotation.vnc(), VNC_PROPERTY);
    }

    private void mergeVNC(boolean vnc)
    {
        if (!vnc() && vnc) {
            this.vnc = true;
        }
    }

    private void resolveOffice()
    {
        this.office = resolve(this.uiTestAnnotation.office(), OFFICE_PROPERTY);
    }

    private void mergeOffice(boolean office)
    {
        if (!isOffice() && office) {
            this.office = true;
        }
    }

    private void resolveProperties()
    {
        this.properties = resolveGenericProperties(this.uiTestAnnotation.properties(), PROPERTIES_PREFIX_PROPERTY);
    }

    private void mergeProperties(Properties properties) throws DockerTestException
    {
        this.properties = this.propertiesMerger.merge(getProperties(), properties, false);
    }

    private void resolveDatabaseCommands()
    {
        this.databaseCommands = resolveGenericProperties(this.uiTestAnnotation.databaseCommands(),
            DATABASE_PREFIX_COMMAND);
    }

    private void mergeDatabaseCommands(Properties databaseCommands) throws DockerTestException
    {
        this.databaseCommands = this.propertiesMerger.merge(getDatabaseCommands(), databaseCommands, false);
    }

    private Properties resolveGenericProperties(String[] propertiesAsArray)
    {
        return resolveGenericProperties(propertiesAsArray, null);
    }

    private Properties resolveGenericProperties(String[] propertiesAsArray, String prefix)
    {
        Properties newProperties = new Properties();
        for (String propertyAsString : propertiesAsArray) {
            int pos = propertyAsString.indexOf('=');
            if (pos > -1) {
                newProperties.setProperty(propertyAsString.substring(0, pos), propertyAsString.substring(pos + 1));
            }
        }

        if (prefix != null) {
            for (String key : System.getProperties().stringPropertyNames()) {
                if (key.startsWith(prefix)) {
                    String propertyAsString = StringUtils.substringAfter(key, prefix);
                    newProperties.setProperty(propertyAsString, System.getProperty(key));
                }
            }
        }

        return newProperties;
    }

    private void resolveExtraJARs()
    {
        List<ArtifactCoordinate> artifactCoordinates = new ArrayList<>();
        for (String coordinate : this.uiTestAnnotation.extraJARs()) {
            artifactCoordinates.add(ArtifactCoordinate.parseArtifacts(coordinate));
        }
        this.extraJARs = artifactCoordinates;
    }

    private void mergeExtraJARs(List<ArtifactCoordinate> extraJARs)
    {
        List<ArtifactCoordinate> mergedExtraJARs = getExtraJARs();
        mergedExtraJARs.addAll(extraJARs);
        this.extraJARs = mergedExtraJARs;
    }

    private void resolveResolveExtraJARs()
    {
        this.resolveExtraJARs = this.uiTestAnnotation.resolveExtraJARs();
    }

    private void mergeResolveExtraJARs(boolean resolveExtraJARs)
    {
        if (!isResolveExtraJARs() && resolveExtraJARs) {
            this.resolveExtraJARs = true;
        }
    }

    private void resolveExtensionOverrides()
    {
        List<ExtensionOverride> overrides = new ArrayList<>();
        for (org.xwiki.test.docker.junit5.ExtensionOverride overrideAnnotation : this.uiTestAnnotation
            .extensionOverrides()) {
            ExtensionOverride override = new ExtensionOverride();
            override.put(Extension.FIELD_ID, overrideAnnotation.extensionId());
            override.putAll((Map) resolveGenericProperties(overrideAnnotation.overrides()));
            overrides.add(override);
        }
        this.extensionOverrides = overrides;
    }

    private void mergeExtensionOverrides(List<ExtensionOverride> extensionOverrides)
    {
        List<ExtensionOverride> mergedExtensionOverrides = getExtensionOverrides();
        mergedExtensionOverrides.addAll(extensionOverrides);
        this.extensionOverrides = mergedExtensionOverrides;
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

    private void mergeSSHPorts(List<Integer> sshPorts)
    {
        List<Integer> mergedSSHPorts = getSSHPorts();
        mergedSSHPorts.addAll(sshPorts);
        this.sshPorts = mergedSSHPorts;
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

    private void mergeProfiles(List<String> profiles)
    {
        List<String> mergedProfiles = getProfiles();
        mergedProfiles.addAll(profiles);
        this.profiles = mergedProfiles;
    }

    private void resolveForbiddenServletEngines()
    {
        List<ServletEngine> newForbiddenServletEngines = new ArrayList<>();
        if (this.uiTestAnnotation.forbiddenEngines().length > 0) {
            newForbiddenServletEngines.addAll(Arrays.asList(this.uiTestAnnotation.forbiddenEngines()));
        }
        this.forbiddenServletEngines = newForbiddenServletEngines;
    }

    private void mergeForbiddenServletEngines(List<ServletEngine> forbiddenServletEngines)
    {
        List<ServletEngine> mergedForbiddenServletEngines = getForbiddenServletEngines();
        mergedForbiddenServletEngines.addAll(forbiddenServletEngines);
        this.forbiddenServletEngines = mergedForbiddenServletEngines;
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
     * be saved and reused for another run
     * @since 10.10RC1
     */
    public boolean isDatabaseDataSaved()
    {
        return this.saveDatabaseData;
    }

    /**
     * @return true if the Maven resolving is done in offline mode (i.e. you need to have the required artifacts in your
     * local repository). False by default to avoid developer problems but should be set to true in the CI to improve
     * performance of functional tests
     * @since 10.10RC1
     */
    public boolean isOffline()
    {
        return this.offline;
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
     * depending on the database)
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
     * @return the list of configuration properties to use when generating the XWiki configuration files such as as
     * {@code xwiki.properties} (check {@code xwiki.properties.vm} to find the list of supported properties)
     * @since 10.10RC1
     */
    public Properties getProperties()
    {
        return this.properties;
    }

    /**
     * @return the list of database docker commands to use and that will override default commands (example of command
     * {@code character-set-server=utf8mb4}
     * @since 11.2RC1
     */
    public Properties getDatabaseCommands()
    {
        return this.databaseCommands;
    }

    /**
     * @return the list of extra JARs to add to the {@code WEB-INF/lib} directory
     * @since 10.11RC1
     */
    public List<ArtifactCoordinate> getExtraJARs()
    {
        return this.extraJARs;
    }

    /**
     * @return true if extra JARs version should be resolved when missing, see {@link UITest#resolveExtraJARs()}
     * @since 12.5RC1
     */
    public boolean isResolveExtraJARs()
    {
        return this.resolveExtraJARs;
    }

    /**
     * @return the overrides of the extensions descriptors
     * @since 11.6RC1
     */
    public List<ExtensionOverride> getExtensionOverrides()
    {
        return this.extensionOverrides;
    }

    /**
     * @return the list of ports that should be SSH-forwarded when connecting from a Docker container to the host (i.e.
     * when using the {@code host.testcontainers.internal} host name). This is in addition to port {@code 8080} which is
     * always added. For example if you need XWiki to send a mail to a SMTP server running on port 3025 on the host, you
     * should add port 3025 to the list.
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
     * generated XWiki configuration - XWiki WAR file, etc)
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
     * system property is not defined then construct an output directory name based on the defined configuration so that
     * we can run different configurations one after another without them overriding each other. The {@code
     * maven.build.dir} system property is there to allow controlling where the Maven output directory is located when
     * running from Maven.
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
     * then the test will be skipped
     * @since 10.11RC1
     */
    public List<ServletEngine> getForbiddenServletEngines()
    {
        return this.forbiddenServletEngines;
    }
}
