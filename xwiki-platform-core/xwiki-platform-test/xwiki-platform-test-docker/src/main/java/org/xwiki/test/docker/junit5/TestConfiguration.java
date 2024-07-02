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

import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
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

    private Browser browser;

    private Database database;

    private ServletEngine servletEngine;

    private boolean verbose;

    private boolean debug;

    private boolean offline;

    private String servletEngineTag;

    private String databaseTag;

    private String jdbcDriverVersion;

    private boolean vnc;

    private boolean wcag;

    private boolean wcagStopOnError;

    private Properties properties;

    private List<ExtensionOverride> extensionOverrides;

    private Set<ArtifactCoordinate> extraJARs;

    private boolean resolveExtraJARs;

    private List<Integer> sshPorts;

    private List<String> profiles;

    private boolean office;

    private List<ServletEngine> forbiddenServletEngines;

    private Properties databaseCommands;

    private PropertiesMerger propertiesMerger = new PropertiesMerger();

    private boolean saveDatabaseData;

    private boolean savePermanentDirectoryData;

    private List<String> servletEngineNetworkAliases;

    /**
     * @param testConfiguration the configuration to merge with the current one
     * @throws DockerTestException when a merge error occurs
     */
    public void merge(TestConfiguration testConfiguration) throws DockerTestException
    {
        mergeBrowser(testConfiguration.getBrowser());
        mergeDatabase(testConfiguration.getDatabase());
        mergeServletEngine(testConfiguration.getServletEngine());
        mergeVerbose(testConfiguration.isVerbose());
        mergeDebug(testConfiguration.isDebug());
        mergeOffline(testConfiguration.isOffline());
        mergeDatabaseTag(testConfiguration.getDatabaseTag());
        mergeServletEngineTag(testConfiguration.getServletEngineTag());
        mergeJDBCDriverVersion(testConfiguration.getJDBCDriverVersion());
        mergeVNC(testConfiguration.vnc());
        mergeWCAG(testConfiguration.isWCAG());
        mergeWCAGStopOnError(testConfiguration.shouldWCAGStopOnError());
        mergeProperties(testConfiguration.getProperties());
        mergeExtraJARs(testConfiguration.getExtraJARs());
        mergeResolveExtraJARs(testConfiguration.isResolveExtraJARs());
        mergeExtensionOverrides(testConfiguration.getExtensionOverrides());
        mergeSSHPorts(testConfiguration.getSSHPorts());
        mergeProfiles(testConfiguration.getProfiles());
        mergeOffice(testConfiguration.isOffice());
        mergeForbiddenServletEngines(testConfiguration.getForbiddenServletEngines());
        mergeDatabaseCommands(testConfiguration.getDatabaseCommands());
        mergeSaveDatabaseData(testConfiguration.isDatabaseDataSaved());
        mergeSavePermanentDirectoryData(testConfiguration.isPermanentDirectoryDataSaved());
        mergeServletEngineNetworkAliases(testConfiguration.getServletEngineNetworkAliases());
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

    private void mergeVerbose(boolean verbose)
    {
        if (!isVerbose() && verbose) {
            this.verbose = true;
        }
    }

    private void mergeDebug(boolean debug)
    {
        if (!isDebug() && debug) {
            this.debug = true;
        }
    }

    private void mergeOffline(boolean offline)
    {
        if (!isOffline() && offline) {
            this.offline = true;
        }
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

    private void mergeVNC(boolean vnc)
    {
        if (!vnc() && vnc) {
            this.vnc = true;
        }
    }

    /**
     * @since 15.2RC1
     */
    private void mergeWCAG(boolean wcag)
    {
        this.wcag = isWCAG() || wcag;
    }

    private void mergeWCAGStopOnError(boolean wcagStopOnError)
    {
        this.wcagStopOnError = shouldWCAGStopOnError() || wcagStopOnError;
    }

    private void mergeOffice(boolean office)
    {
        if (!isOffice() && office) {
            this.office = true;
        }
    }

    private void mergeProperties(Properties properties) throws DockerTestException
    {
        this.properties = this.propertiesMerger.merge(getProperties(), properties, false);
    }

    private void mergeDatabaseCommands(Properties databaseCommands) throws DockerTestException
    {
        this.databaseCommands = this.propertiesMerger.merge(getDatabaseCommands(), databaseCommands, false);
    }

    private void mergeExtraJARs(Collection<ArtifactCoordinate> extraJARs)
    {
        Set<ArtifactCoordinate> mergedExtraJARs = getExtraJARs();
        if (extraJARs != null) {
            mergedExtraJARs.addAll(extraJARs);
        }
        this.extraJARs = mergedExtraJARs;
    }

    private void mergeResolveExtraJARs(boolean resolveExtraJARs)
    {
        if (!isResolveExtraJARs() && resolveExtraJARs) {
            this.resolveExtraJARs = true;
        }
    }

    private void mergeExtensionOverrides(List<ExtensionOverride> extensionOverrides)
    {
        List<ExtensionOverride> mergedExtensionOverrides = getExtensionOverrides();
        if (extensionOverrides != null) {
            mergedExtensionOverrides.addAll(extensionOverrides);
        }
        this.extensionOverrides = mergedExtensionOverrides;
    }

    private void mergeSSHPorts(List<Integer> sshPorts)
    {
        List<Integer> mergedSSHPorts = getSSHPorts();
        if (sshPorts != null) {
            mergedSSHPorts.addAll(sshPorts);
        }
        this.sshPorts = mergedSSHPorts;
    }

    private void mergeProfiles(List<String> profiles)
    {
        List<String> mergedProfiles = getProfiles();
        if (profiles != null) {
            mergedProfiles.addAll(profiles);
        }
        this.profiles = mergedProfiles;
    }

    private void mergeForbiddenServletEngines(List<ServletEngine> forbiddenServletEngines)
    {
        List<ServletEngine> mergedForbiddenServletEngines = getForbiddenServletEngines();
        if (forbiddenServletEngines != null) {
            mergedForbiddenServletEngines.addAll(forbiddenServletEngines);
        }
        this.forbiddenServletEngines = mergedForbiddenServletEngines;
    }

    private void mergeSaveDatabaseData(boolean saveDatabaseData)
    {
        if (!isDatabaseDataSaved() && saveDatabaseData) {
            this.saveDatabaseData = true;
        }
    }

    private void mergeSavePermanentDirectoryData(boolean savePermanentDirectoryData)
    {
        if (!isPermanentDirectoryDataSaved() && savePermanentDirectoryData) {
            this.savePermanentDirectoryData = true;
        }
    }

    private void mergeServletEngineNetworkAliases(List<String> aliases)
    {
        List<String> mergedAliases = getServletEngineNetworkAliases();
        if (aliases != null) {
            mergedAliases.addAll(aliases);
        }
        this.servletEngineNetworkAliases = mergedAliases;
    }

    /**
     * @return the browser to use
     */
    public Browser getBrowser()
    {
        return this.browser;
    }

    /**
     * @param browser see {@link #getBrowser()}
     */
    public void setBrowser(Browser browser)
    {
        this.browser = browser;
    }

    /**
     * @return the database to use
     */
    public Database getDatabase()
    {
        return this.database;
    }

    /**
     * @param database see {@link #getDatabase()}
     */
    public void setDatabase(Database database)
    {
        this.database = database;
    }
    /**
     * @return the Servlet engine to use
     */
    public ServletEngine getServletEngine()
    {
        return this.servletEngine;
    }

    /**
     * @param servletEngine see {@link #getServletEngine()}
     */
    public void setServletEngine(ServletEngine servletEngine)
    {
        this.servletEngine = servletEngine;
    }

    /**
     * @return true if the test should output verbose console logs or not
     */
    public boolean isVerbose()
    {
        return this.verbose;
    }

    /**
     * @param verbose see {@link #isVerbose()}
     */
    public void setVerbose(boolean verbose)
    {
        this.verbose = verbose;
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
     * @param debug see {@link #isDebug()}
     */
    public void setDebug(boolean debug)
    {
        this.debug = debug;
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
     * @param offline see {@link #isOffline()}
     */
    public void setOffline(boolean offline)
    {
        this.offline = offline;
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
     * @param databaseTag see {@link #getDatabaseTag()}
     */
    public void setDatabaseTag(String databaseTag)
    {
        this.databaseTag = databaseTag;
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
     * @param servletEngineTag see {@link #getServletEngineTag()}
     */
    public void setServletEngineTag(String servletEngineTag)
    {
        this.servletEngineTag = servletEngineTag;
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
     * @param jdbcDriverVersion see {@link #getJDBCDriverVersion()}
     */
    public void setJDBCDriverVersion(String jdbcDriverVersion)
    {
        this.jdbcDriverVersion = jdbcDriverVersion;
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
     * @param vnc see {@link #vnc()}
     */
    public void setVNC(boolean vnc)
    {
        this.vnc = vnc;
    }

    /**
     * @return true if WCAG rules should be checked.
     * @since 15.2RC1
     */
    public boolean isWCAG()
    {
        return this.wcag;
    }

    /**
     * @param wcag see {@link #isWCAG()}
     * @since 15.2RC1
     */
    public void setWCAG(boolean wcag)
    {
        this.wcag = wcag;
    }

    /**
     * @return {@code false} if WCAG validation should ignore errors, {@code true} otherwise.
     * @since 16.1.0
     */
    public boolean shouldWCAGStopOnError()
    {
        return this.wcagStopOnError;
    }

    /**
     * @param wcagStopOnError {@code false} if WCAG validation should ignore errors, {@code true} otherwise.
     * @since 16.1.0
     */
    public void setWCAGStopOnError(boolean wcagStopOnError)
    {
        this.wcagStopOnError = wcagStopOnError;
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
     * @param properties see {@link #getProperties()}
     */
    public void setProperties(Properties properties)
    {
        this.properties = properties;
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
     * @param databaseCommands see {@link #getDatabaseCommands()}
     */
    public void setDatabaseCommands(Properties databaseCommands)
    {
        this.databaseCommands = databaseCommands;
    }

    /**
     * @return the list of extra JARs to add to the {@code WEB-INF/lib} directory
     * @since 10.11RC1
     */
    public Set<ArtifactCoordinate> getExtraJARs()
    {
        return this.extraJARs;
    }

    /**
     * @param extraJARs see {@link #getExtraJARs()}
     */
    public void setExtraJARs(Set<ArtifactCoordinate> extraJARs)
    {
        this.extraJARs = extraJARs;
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
     * @param resolveExtraJARs see {@link #isResolveExtraJARs()}
     */
    public void setResolveExtraJARs(boolean resolveExtraJARs)
    {
        this.resolveExtraJARs = resolveExtraJARs;
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
     * @param extensionOverrides see {@link #getExtensionOverrides()}
     */
    public void setExtensionOverrides(List<ExtensionOverride> extensionOverrides)
    {
        this.extensionOverrides = extensionOverrides;
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
     * @param sshPorts see {@link #getSSHPorts()}
     */
    public void setSSHPorts(List<Integer> sshPorts)
    {
        this.sshPorts = sshPorts;
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
     * @param profiles see {@link #getProfiles()}
     */
    public void setProfiles(List<String> profiles)
    {
        this.profiles = profiles;
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
     * @param office see {@link #isOffice()}
     */
    public void setOffice(boolean office)
    {
        this.office = office;
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

    /**
     * @param forbiddenServletEngines see {@link #getForbiddenServletEngines()}
     */
    public void setForbiddenServletEngines(List<ServletEngine> forbiddenServletEngines)
    {
        this.forbiddenServletEngines = forbiddenServletEngines;
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
     * @param saveDatabaseData see {@link #isDatabaseDataSaved()}
     */
    public void setSaveDatabaseData(boolean saveDatabaseData)
    {
        this.saveDatabaseData = saveDatabaseData;
    }

    /**
     * @return true if the XWiki permanent directory should be mapped to a local directory on the host computer so that
     *         it can be accessed once the test is finished, for debugging purposes
     * @since 14.5
     */
    public boolean isPermanentDirectoryDataSaved()
    {
        return this.savePermanentDirectoryData;
    }

    /**
     * @param savePermanentDirectoryData see {@link #isPermanentDirectoryDataSaved()}
     * @since 14.5
     */
    public void setSavePermanentDirectoryData(boolean savePermanentDirectoryData)
    {
        this.savePermanentDirectoryData = savePermanentDirectoryData;
    }

    /**
     * @return the list of network aliases to use for the servlet engine Docker container
     * @since 15.10.12
     * @since 16.4.1
     * @since 16.6.0RC1
     */
    public List<String> getServletEngineNetworkAliases()
    {
        return this.servletEngineNetworkAliases;
    }

    /**
     * @param servletEngineNetworkAliases see {@link #getServletEngineNetworkAliases()}
     * @since 15.10.12
     * @since 16.4.1
     * @since 16.6.0RC1
     */
    public void setServletEngineNetworkAliases(List<String> servletEngineNetworkAliases)
    {
        this.servletEngineNetworkAliases = servletEngineNetworkAliases;
    }
}
