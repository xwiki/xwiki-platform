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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.extension.Extension;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.browser.Browser;
import org.xwiki.test.docker.junit5.database.Database;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.integration.maven.ArtifactCoordinate;
import org.xwiki.tool.extension.ExtensionOverride;

/**
 * Generate a {@link org.xwiki.test.docker.junit5.TestConfiguration} from a {@link UITest} annotation data.
 *
 * @version $Id$
 */
public class UITestTestConfigurationResolver
{
    private static final String BROWSER_PROPERTY = "xwiki.test.ui.browser";

    private static final String DATABASE_PROPERTY = "xwiki.test.ui.database";

    private static final String DATABASE_PREFIX_COMMAND = "xwiki.test.ui.database.commands.";

    private static final String SERVLETENGINE_PROPERTY = "xwiki.test.ui.servletEngine";

    private static final String VERBOSE_PROPERTY = "xwiki.test.ui.verbose";

    private static final String DEBUG_PROPERTY = "xwiki.test.ui.debug";

    private static final String OFFLINE_PROPERTY = "xwiki.test.ui.offline";

    private static final String DATABASETAG_PROPERTY = "xwiki.test.ui.databaseTag";

    private static final String SERVLETENGINETAG_PROPERTY = "xwiki.test.ui.servletEngineTag";

    private static final String JDBCDRIVERVERSION_PROPERTY = "xwiki.test.ui.jdbcDriverVersion";

    private static final String VNC_PROPERTY = "xwiki.test.ui.vnc";

    private static final String WCAG_PROPERTY = "xwiki.test.ui.wcag";

    private static final String WCAG_STOP_ON_ERROR_PROPERTY = "xwiki.test.ui.wcagStopOnError";

    private static final String PROPERTIES_PREFIX_PROPERTY = "xwiki.test.ui.properties.";

    private static final String PROFILES_PROPERTY = "xwiki.test.ui.profiles";

    private static final String OFFICE_PROPERTY = "xwiki.test.ui.office";

    private static final String SAVEDBDATA_PROPERTY = "xwiki.test.ui.saveDatabaseData";

    private static final String SAVEPERMANENTDIRECTORY_PROPERTY = "xwiki.test.ui.savePermanentDirectoryData";

    private static final String SERVLET_ENGINE_NETWORK_ALIASES_PROPERTY = "xwiki.test.ui.servletEngineNetworkAliases";

    /**
     * @param uiTestAnnotation the annotation from which to extract the configuration
     * @return the constructed {@link TestConfiguration} object containing the full test configuration
     */
    public TestConfiguration resolve(UITest uiTestAnnotation)
    {
        TestConfiguration configuration = new TestConfiguration();
        configuration.setBrowser(resolveBrowser(uiTestAnnotation.browser()));
        configuration.setDatabase(resolveDatabase(uiTestAnnotation.database()));
        configuration.setServletEngine(resolveServletEngine(uiTestAnnotation.servletEngine()));
        configuration.setVerbose(resolveVerbose(uiTestAnnotation.verbose()));
        configuration.setDebug(resolveDebug(uiTestAnnotation.debug()));
        configuration.setOffline(resolveOffline(uiTestAnnotation.offline()));
        configuration.setDatabaseTag(resolveDatabaseTag(uiTestAnnotation.databaseTag()));
        configuration.setServletEngineTag(resolveServletEngineTag(uiTestAnnotation.servletEngineTag()));
        configuration.setJDBCDriverVersion(resolveJDBCDriverVersion(uiTestAnnotation.jdbcDriverVersion()));
        configuration.setVNC(resolveVNC(uiTestAnnotation.vnc()));
        configuration.setWCAG(resolveWCAG(uiTestAnnotation.wcag()));
        configuration.setWCAGStopOnError(resolveWCAGStopOnError(uiTestAnnotation.wcagStopOnError()));
        configuration.setProperties(resolveProperties(uiTestAnnotation.properties()));
        configuration.setExtraJARs(resolveExtraJARs(uiTestAnnotation.extraJARs()));
        configuration.setResolveExtraJARs(resolveResolveExtraJARs(uiTestAnnotation.resolveExtraJARs()));
        configuration.setExtensionOverrides(resolveExtensionOverrides(uiTestAnnotation.extensionOverrides()));
        configuration.setSSHPorts(resolveSSHPorts(uiTestAnnotation.sshPorts()));
        configuration.setProfiles(resolveCommaSeparatedValues(uiTestAnnotation.profiles(), PROFILES_PROPERTY));
        configuration.setOffice(resolveOffice(uiTestAnnotation.office()));
        configuration.setForbiddenServletEngines(resolveForbiddenServletEngines(uiTestAnnotation.forbiddenEngines()));
        configuration.setDatabaseCommands(resolveDatabaseCommands(uiTestAnnotation.databaseCommands()));
        configuration.setSaveDatabaseData(resolveSaveDatabaseData(uiTestAnnotation.saveDatabaseData()));
        configuration.setSavePermanentDirectoryData(resolveSavePermanentDirectoryData(
            uiTestAnnotation.savePermanentDirectoryData()));
        configuration.setServletEngineNetworkAliases(resolveCommaSeparatedValues(
            uiTestAnnotation.servletEngineNetworkAliases(), SERVLET_ENGINE_NETWORK_ALIASES_PROPERTY));
        return configuration;
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

    private Browser resolveBrowser(Browser browser)
    {
        return resolve(Browser.class, browser, BROWSER_PROPERTY);
    }

    private Database resolveDatabase(Database database)
    {
        return resolve(Database.class, database, DATABASE_PROPERTY);
    }

    private ServletEngine resolveServletEngine(ServletEngine servletEngine)
    {
        return resolve(ServletEngine.class, servletEngine, SERVLETENGINE_PROPERTY);
    }

    private boolean resolveVerbose(boolean verbose)
    {
        boolean isVerbose;
        // Always display verbose logs for debugging when inside a container.
        if (DockerTestUtils.isInAContainer()) {
            isVerbose = true;
        } else {
            isVerbose = resolve(verbose, VERBOSE_PROPERTY);
        }
        return isVerbose;
    }

    private boolean resolveDebug(boolean debug)
    {
        return resolve(debug, DEBUG_PROPERTY);
    }

    private boolean resolveOffline(boolean offline)
    {
        return resolve(offline, OFFLINE_PROPERTY);
    }

    private String resolveDatabaseTag(String databaseTag)
    {
        return resolve(databaseTag, DATABASETAG_PROPERTY);
    }

    private String resolveServletEngineTag(String servletEngineTag)
    {
        return resolve(servletEngineTag, SERVLETENGINETAG_PROPERTY);
    }

    private String resolveJDBCDriverVersion(String jdbcDriverVersion)
    {
        return resolve(jdbcDriverVersion, JDBCDRIVERVERSION_PROPERTY);
    }

    private boolean resolveVNC(boolean vnc)
    {
        return resolve(vnc, VNC_PROPERTY);
    }

    private boolean resolveWCAG(boolean wcag)
    {
        return resolve(wcag, WCAG_PROPERTY);
    }

    private boolean resolveWCAGStopOnError(boolean wcagStopOnError)
    {
        return resolve(wcagStopOnError, WCAG_STOP_ON_ERROR_PROPERTY);
    }

    private boolean resolveOffice(boolean office)
    {
        return resolve(office, OFFICE_PROPERTY);
    }

    private Properties resolveProperties(String[] properties)
    {
        return resolveGenericProperties(properties, PROPERTIES_PREFIX_PROPERTY);
    }

    private Properties resolveDatabaseCommands(String[] databaseCommands)
    {
        return resolveGenericProperties(databaseCommands, DATABASE_PREFIX_COMMAND);
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

    private Set<ArtifactCoordinate> resolveExtraJARs(String[] extraJARs)
    {
        Set<ArtifactCoordinate> artifactCoordinates = new LinkedHashSet<>();
        for (String coordinate : extraJARs) {
            artifactCoordinates.add(ArtifactCoordinate.parseArtifacts(coordinate));
        }
        return artifactCoordinates;
    }

    private boolean resolveResolveExtraJARs(boolean resolveExtraJARs)
    {
        return resolveExtraJARs;
    }

    private List<ExtensionOverride> resolveExtensionOverrides(
        org.xwiki.test.docker.junit5.ExtensionOverride[] extensionOverrides)
    {
        List<ExtensionOverride> overrides = new ArrayList<>();
        for (org.xwiki.test.docker.junit5.ExtensionOverride extensionOverride : extensionOverrides) {
            ExtensionOverride override = new ExtensionOverride();
            override.put(Extension.FIELD_ID, extensionOverride.extensionId());
            override.putAll((Map) resolveGenericProperties(extensionOverride.overrides()));
            overrides.add(override);
        }
        return overrides;
    }

    private List<Integer> resolveSSHPorts(int[] sshPorts)
    {
        List<Integer> newSSHPorts = new ArrayList<>();
        newSSHPorts.add(8080);
        for (int sshPort : sshPorts) {
            newSSHPorts.add(sshPort);
        }
        return newSSHPorts;
    }

    private List<String> resolveCommaSeparatedValues(String[] values, String systemProperty)
    {
        String[] actualValues = values.length > 0 ? values : System.getProperty(systemProperty, "").split("\\s*,\\s*");
        return Stream.of(actualValues).filter(StringUtils::isNotBlank)
            .collect(Collectors.toCollection(() -> new ArrayList<>()));
    }

    private List<ServletEngine> resolveForbiddenServletEngines(ServletEngine[] forbiddenServletEngines)
    {
        List<ServletEngine> newForbiddenServletEngines = new ArrayList<>();
        if (forbiddenServletEngines.length > 0) {
            newForbiddenServletEngines.addAll(Arrays.asList(forbiddenServletEngines));
        }
        return newForbiddenServletEngines;
    }

    private boolean resolveSaveDatabaseData(boolean saveDatabaseData)
    {
        return resolve(saveDatabaseData, SAVEDBDATA_PROPERTY);
    }

    private boolean resolveSavePermanentDirectoryData(boolean savePermanentDirectoryData)
    {
        return resolve(savePermanentDirectoryData, SAVEPERMANENTDIRECTORY_PROPERTY);
    }
}
