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
package org.xwiki.webjars.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import javax.inject.Provider;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.filesystem.FilesystemExportContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FilesystemResourceReferenceSerializer}.
 *
 * @version $Id$
 */
@ComponentTest
@SuppressWarnings("checkstyle:MultipleStringLiterals")
class FilesystemResourceReferenceSerializerTest
{
    private static final File BASEDIR = new File(System.getProperty("java.io.tmpdir"), "xwikitest");

    private static final String FONTAWESOME_VERSION = loadFontAwesomeVersion();

    private static final String WEBJAR_PREFIX = "webjars/font-awesome/" + FONTAWESOME_VERSION;

    @MockComponent
    private Provider<FilesystemExportContext> exportContextProvider;

    @InjectMockComponents
    private FilesystemResourceReferenceSerializer serializer;

    private ClassLoader originalThreadContextClassLoader;

    /**
     * Load the Font Awesome version from the WebJar's pom.properties file.
     *
     * @return the Font Awesome version
     */
    private static String loadFontAwesomeVersion()
    {
        // Try to load from the WebJar's pom.properties file
        String propertiesPath = "META-INF/maven/org.webjars/font-awesome/pom.properties";
        try (InputStream is = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream(propertiesPath))
        {
            if (is == null) {
                throw new RuntimeException("Could not find Font Awesome pom.properties at " + propertiesPath);
            }

            Properties properties = new Properties();
            properties.load(is);
            return properties.getProperty("version");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Font Awesome version from " + propertiesPath, e);
        }
    }

    @BeforeEach
    void setUp() throws Exception
    {
        this.originalThreadContextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        FileUtils.deleteDirectory(BASEDIR);
    }

    @AfterEach
    void tearDown()
    {
        Thread.currentThread().setContextClassLoader(this.originalThreadContextClassLoader);
    }

    @Test
    void serialize() throws Exception
    {
        FilesystemExportContext exportContext = new FilesystemExportContext();
        exportContext.setExportDir(BASEDIR);

        when(this.exportContextProvider.get()).thenReturn(exportContext);

        WebJarsResourceReference reference = new WebJarsResourceReference("wiki:wiki", Arrays.asList(
            "font-awesome", FONTAWESOME_VERSION, "webfonts/fa-regular-400.woff2"));

        // Verify that the returned URL is ok
        assertEquals(WEBJAR_PREFIX + "/webfonts/fa-regular-400.woff2",
            this.serializer.serialize(reference).serialize());

        // Also verify that the resource has been copied!
        assertTrue(new File(BASEDIR, WEBJAR_PREFIX + "/webfonts/fa-regular-400.woff2").exists());
    }

    @Test
    void serializeWithCSSPathAdjustments() throws Exception
    {
        FilesystemExportContext exportContext = new FilesystemExportContext();
        exportContext.setExportDir(BASEDIR);
        exportContext.pushCSSParentLevels(3);

        when(this.exportContextProvider.get()).thenReturn(exportContext);

        WebJarsResourceReference reference = new WebJarsResourceReference("wiki:wiki", Arrays.asList(
            "font-awesome", FONTAWESOME_VERSION, "webfonts/fa-regular-400.woff2"));

        // Verify that the returned URL is ok
        assertEquals("../../../" + WEBJAR_PREFIX + "/webfonts/fa-regular-400.woff2",
            this.serializer.serialize(reference).serialize());

        // Also verify that the resource has been copied!
        assertTrue(new File(BASEDIR, WEBJAR_PREFIX + "/webfonts/fa-regular-400.woff2").exists());
    }

    @Test
    void serializeWithCSSPathAdjustmentsWithDocParentLevels() throws Exception
    {
        FilesystemExportContext exportContext = new FilesystemExportContext();
        exportContext.setExportDir(BASEDIR);
        exportContext.setDocParentLevels(2);

        when(this.exportContextProvider.get()).thenReturn(exportContext);

        WebJarsResourceReference reference = new WebJarsResourceReference("wiki:wiki", Arrays.asList(
            "font-awesome", FONTAWESOME_VERSION, "webfonts/fa-regular-400.woff2"));

        // Verify that the returned URL is ok
        assertEquals("../../" + WEBJAR_PREFIX + "/webfonts/fa-regular-400.woff2",
            this.serializer.serialize(reference).serialize());

        // Also verify that the resource has been copied!
        assertTrue(new File(BASEDIR, WEBJAR_PREFIX + "/webfonts/fa-regular-400.woff2").exists());
    }

    @Test
    void serializeCSSResourceWithURLsInIt() throws Exception
    {
        FilesystemExportContext exportContext = new FilesystemExportContext();
        exportContext.setExportDir(BASEDIR);

        when(this.exportContextProvider.get()).thenReturn(exportContext);

        WebJarsResourceReference reference = new WebJarsResourceReference("wiki:wiki", Arrays.asList(
            "font-awesome", FONTAWESOME_VERSION, "css/all.min.css"));

        assertEquals(WEBJAR_PREFIX + "/css/all.min.css", this.serializer.serialize(reference).serialize());

        // Also verify that the resources haves been copied!
        assertTrue(new File(BASEDIR, WEBJAR_PREFIX + "/css/all.min.css").exists());
        assertTrue(new File(BASEDIR, WEBJAR_PREFIX + "/webfonts/fa-regular-400.woff2").exists());
        assertTrue(new File(BASEDIR, WEBJAR_PREFIX + "/webfonts/fa-solid-900.woff2").exists());
        assertTrue(new File(BASEDIR, WEBJAR_PREFIX + "/webfonts/fa-brands-400.woff2").exists());
        assertTrue(new File(BASEDIR, WEBJAR_PREFIX + "/webfonts/fa-v4compatibility.woff2").exists());
    }
}
