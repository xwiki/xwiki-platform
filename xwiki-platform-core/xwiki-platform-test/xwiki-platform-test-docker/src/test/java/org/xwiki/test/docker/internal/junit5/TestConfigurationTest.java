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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.test.docker.junit5.DockerTestException;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.browser.Browser;
import org.xwiki.test.docker.junit5.database.Database;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.integration.maven.ArtifactCoordinate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link TestConfiguration}.
 *
 * @version $Id$
 */
class TestConfigurationTest
{
    @UITest
    class EmptyAnnotation
    {
    }

    @UITest(servletEngine = ServletEngine.TOMCAT, verbose = true, databaseTag = "version")
    class SampleAnnotation
    {
    }

    @UITest(verbose = true, databaseTag = "version",
        properties = {
            "key1=value1",
            "xwikiCfgPlugins=value21, value22",
        },
        extraJARs = {
            "groupId1:artifactId1:type1:version1",
            "groupId2:artifactId2:type2:version2",
        }, resolveExtraJARs = true)
    class ToMergeMainAnnotation
    {
    }

    @UITest(
        properties = {
            "xwikiCfgPlugins=value23",
            "key3=value3"
        },
        extraJARs = {
            "groupId3:artifactId3:type3:version3"
        })
    class ToMergeOkAnnotation
    {
    }

    @UITest(
        properties = {
            "key1=othervalue"
        })
    class ToMergeNotOkAnnotation
    {
    }

    @BeforeEach
    void setUp()
    {
        System.clearProperty("xwiki.test.ui.servletEngine");
        System.clearProperty("xwiki.test.ui.verbose");
        System.clearProperty("xwiki.test.ui.databaseTag");
    }

    @Test
    void getConfigurationWhenDefault()
    {
        UITest uiTest = EmptyAnnotation.class.getAnnotation(UITest.class);
        UITestTestConfigurationResolver resolver = new UITestTestConfigurationResolver();
        TestConfiguration configuration = resolver.resolve(uiTest);
        assertEquals(ServletEngine.JETTY_STANDALONE, configuration.getServletEngine());
        assertEquals(Browser.FIREFOX, configuration.getBrowser());
        assertEquals(Database.HSQLDB_EMBEDDED, configuration.getDatabase());
        assertNull(configuration.getServletEngineTag());
        assertNull(configuration.getDatabaseTag());
    }

    @Test
    void getConfigurationWhenInAnnotationAndNoSystemProperty()
    {
        UITest uiTest = SampleAnnotation.class.getAnnotation(UITest.class);
        UITestTestConfigurationResolver resolver = new UITestTestConfigurationResolver();
        TestConfiguration configuration = resolver.resolve(uiTest);
        assertEquals(ServletEngine.TOMCAT, configuration.getServletEngine());
        assertTrue(configuration.isVerbose());
        assertEquals("version", configuration.getDatabaseTag());
    }

    @Test
    void getConfigurationWhenInSystemPropertiesAndNotInAnnotation()
    {
        UITest uiTest = EmptyAnnotation.class.getAnnotation(UITest.class);
        System.setProperty("xwiki.test.ui.servletEngine", "jetty");
        System.setProperty("xwiki.test.ui.verbose", "true");
        System.setProperty("xwiki.test.ui.databaseTag", "version");

        UITestTestConfigurationResolver resolver = new UITestTestConfigurationResolver();
        TestConfiguration configuration = resolver.resolve(uiTest);
        assertEquals(ServletEngine.JETTY, configuration.getServletEngine());
        assertTrue(configuration.isVerbose());
        assertEquals("version", configuration.getDatabaseTag());
    }

    @Test
    void getConfigurationWhenInSystemPropertiesAndInAnnotation()
    {
        UITest uiTest = SampleAnnotation.class.getAnnotation(UITest.class);
        System.setProperty("xwiki.test.ui.servletEngine", "jetty");
        System.setProperty("xwiki.test.ui.verbose", "true");
        System.setProperty("xwiki.test.ui.databaseTag", "otherversion");

        // System properties win!
        UITestTestConfigurationResolver resolver = new UITestTestConfigurationResolver();
        TestConfiguration configuration = resolver.resolve(uiTest);
        assertEquals(ServletEngine.JETTY, configuration.getServletEngine());
        assertTrue(configuration.isVerbose());
        assertEquals("otherversion", configuration.getDatabaseTag());
    }

    @Test
    void mergeConfigurationWhenOk() throws Exception
    {
        UITest uiTest = ToMergeMainAnnotation.class.getAnnotation(UITest.class);
        UITestTestConfigurationResolver resolver = new UITestTestConfigurationResolver();
        TestConfiguration configuration = resolver.resolve(uiTest);
        UITest uiTest2 = ToMergeOkAnnotation.class.getAnnotation(UITest.class);
        configuration.merge(resolver.resolve(uiTest2));

        assertTrue(configuration.isVerbose());
        assertEquals("version", configuration.getDatabaseTag());
        assertTrue(configuration.isResolveExtraJARs());
        assertThat(configuration.getProperties(), hasEntry("key1", "value1"));
        assertThat(configuration.getProperties(), hasEntry("xwikiCfgPlugins", "value21,value22,value23"));
        ArtifactCoordinate coordinate1 = new ArtifactCoordinate("groupId1", "artifactId1", "type1", "version1");
        ArtifactCoordinate coordinate2 = new ArtifactCoordinate("groupId2", "artifactId2", "type2", "version2");
        ArtifactCoordinate coordinate3 = new ArtifactCoordinate("groupId3", "artifactId3", "type3", "version3");
        assertThat(configuration.getExtraJARs(), contains(coordinate1, coordinate2, coordinate3));
    }

    @Test
    void mergeConfigurationWhenNotOk()
    {
        UITest uiTest = ToMergeMainAnnotation.class.getAnnotation(UITest.class);
        UITestTestConfigurationResolver resolver = new UITestTestConfigurationResolver();
        TestConfiguration configuration = resolver.resolve(uiTest);
        UITest uiTest2 = ToMergeNotOkAnnotation.class.getAnnotation(UITest.class);

        Throwable exception = assertThrows(DockerTestException.class, () -> {
            configuration.merge(resolver.resolve(uiTest2));
        });
        assertEquals("Cannot merge property [key1] = [othervalue] since it was already specified with value [value1]",
            exception.getMessage());
    }
}
