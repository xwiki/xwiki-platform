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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.test.docker.junit5.servletEngine.ServletEngine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestConfigurationTest
{
    @UITest
    public class EmptyAnnotation
    {
    }

    @UITest(servletEngine = ServletEngine.TOMCAT, verbose = true, databaseTag = "version")
    public class SampleAnnotation
    {
    }

    @UITest(extraJARs = { "a:b:c" })
    public class AnnotationWithExtraJARAndVersion
    {
    }

    @UITest(extraJARs = { "a:b" })
    public class AnnotationWithExtraJARWithoutVersion
    {
    }

    @BeforeEach
    public void setUp()
    {
        System.clearProperty("xwiki.test.ui.servletEngine");
        System.clearProperty("xwiki.test.ui.verbose");
        System.clearProperty("xwiki.test.ui.databaseTag");
    }

    @Test
    public void getConfigurationWhenDefault()
    {
        UITest uiTest = EmptyAnnotation.class.getAnnotation(UITest.class);

        TestConfiguration configuration = new TestConfiguration(uiTest);
        assertEquals(ServletEngine.JETTY_STANDALONE, configuration.getServletEngine());
        assertFalse(configuration.isVerbose());
        assertNull(configuration.getDatabaseTag());
    }

    @Test
    public void getConfigurationWhenInAnnotationAndNoSystemProperty()
    {
        UITest uiTest = SampleAnnotation.class.getAnnotation(UITest.class);

        TestConfiguration configuration = new TestConfiguration(uiTest);
        assertEquals(ServletEngine.TOMCAT, configuration.getServletEngine());
        assertTrue(configuration.isVerbose());
        assertEquals("version", configuration.getDatabaseTag());
    }

    @Test
    public void getConfigurationWhenInSystemPropertiesAndNotInAnnotation()
    {
        UITest uiTest = EmptyAnnotation.class.getAnnotation(UITest.class);
        System.setProperty("xwiki.test.ui.servletEngine", "jetty");
        System.setProperty("xwiki.test.ui.verbose", "true");
        System.setProperty("xwiki.test.ui.databaseTag", "version");

        TestConfiguration configuration = new TestConfiguration(uiTest);
        assertEquals(ServletEngine.JETTY, configuration.getServletEngine());
        assertTrue(configuration.isVerbose());
        assertEquals("version", configuration.getDatabaseTag());
    }

    @Test
    public void getConfigurationWhenInSystemPropertiesAndInAnnotation()
    {
        UITest uiTest = SampleAnnotation.class.getAnnotation(UITest.class);
        System.setProperty("xwiki.test.ui.servletEngine", "jetty");
        System.setProperty("xwiki.test.ui.verbose", "true");
        System.setProperty("xwiki.test.ui.databaseTag", "otherversion");

        // System properties win!
        TestConfiguration configuration = new TestConfiguration(uiTest);
        assertEquals(ServletEngine.JETTY, configuration.getServletEngine());
        assertTrue(configuration.isVerbose());
        assertEquals("otherversion", configuration.getDatabaseTag());
    }

    @Test
    public void getConfigurationWhenExtraJARWithVersion()
    {
        UITest uiTest = AnnotationWithExtraJARAndVersion.class.getAnnotation(UITest.class);
        TestConfiguration configuration = new TestConfiguration(uiTest);

        assertEquals("a", configuration.getExtraJARs().get(0).get(0));
        assertEquals("b", configuration.getExtraJARs().get(0).get(1));
        assertEquals("c", configuration.getExtraJARs().get(0).get(2));
    }

    @Test
    public void getConfigurationWhenExtraJARWithoutVersion()
    {
        UITest uiTest = AnnotationWithExtraJARWithoutVersion.class.getAnnotation(UITest.class);
        TestConfiguration configuration = new TestConfiguration(uiTest);

        assertEquals("a", configuration.getExtraJARs().get(0).get(0));
        assertEquals("b", configuration.getExtraJARs().get(0).get(1));
        assertNull(configuration.getExtraJARs().get(0).get(2));
    }
}
