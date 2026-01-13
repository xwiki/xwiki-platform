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
import java.util.Arrays;

import javax.inject.Provider;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.url.filesystem.FilesystemExportContext;

import static org.junit.Assert.*;

public class FilesystemResourceReferenceSerializerTest
{
    private static final File BASEDIR = new File(System.getProperty("java.io.tmpdir"), "xwikitest");

    @Rule
    public MockitoComponentMockingRule<FilesystemResourceReferenceSerializer> mocker =
        new MockitoComponentMockingRule<>(FilesystemResourceReferenceSerializer.class);

    private ClassLoader originalThreadContextClassLoader;

    @Before
    public void setUp() throws Exception
    {
        this.originalThreadContextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        FileUtils.deleteDirectory(BASEDIR);
    }

    @After
    public void tearDown()
    {
        Thread.currentThread().setContextClassLoader(this.originalThreadContextClassLoader);
    }

    @Test
    public void serialize() throws Exception
    {
        FilesystemExportContext exportContext = new FilesystemExportContext();
        exportContext.setExportDir(BASEDIR);

        Provider<FilesystemExportContext> exportContextProvider = this.mocker.getInstance(new DefaultParameterizedType(
            null, Provider.class, FilesystemExportContext.class));
        Mockito.when(exportContextProvider.get()).thenReturn(exportContext);

        WebJarsResourceReference reference = new WebJarsResourceReference("wiki:wiki", Arrays.asList(
            "font-awesome", "7.0.1", "webfonts/fa-regular-400.woff2"));

        // Verify that the returned URL is ok
        assertEquals("webjars/font-awesome/7.0.1/webfonts/fa-regular-400.woff2",
            this.mocker.getComponentUnderTest().serialize(reference).serialize());

        // Also verify that the resource has been copied!
        assertTrue(new File(BASEDIR, "webjars/font-awesome/7.0.1/webfonts/fa-regular-400.woff2").exists());
    }

    @Test
    public void serializeWithCSSPathAdjustments() throws Exception
    {
        FilesystemExportContext exportContext = new FilesystemExportContext();
        exportContext.setExportDir(BASEDIR);
        exportContext.pushCSSParentLevels(3);

        Provider<FilesystemExportContext> exportContextProvider = this.mocker.getInstance(new DefaultParameterizedType(
            null, Provider.class, FilesystemExportContext.class));
        Mockito.when(exportContextProvider.get()).thenReturn(exportContext);

        WebJarsResourceReference reference = new WebJarsResourceReference("wiki:wiki", Arrays.asList(
            "font-awesome", "7.0.1", "webfonts/fa-regular-400.woff2"));

        // Verify that the returned URL is ok
        assertEquals("../../../webjars/font-awesome/7.0.1/webfonts/fa-regular-400.woff2",
            this.mocker.getComponentUnderTest().serialize(reference).serialize());

        // Also verify that the resource has been copied!
        assertTrue(new File(BASEDIR, "webjars/font-awesome/7.0.1/webfonts/fa-regular-400.woff2").exists());
    }

    @Test
    public void serializeWithCSSPathAdjustmentsWithDocParentLevels() throws Exception
    {
        FilesystemExportContext exportContext = new FilesystemExportContext();
        exportContext.setExportDir(BASEDIR);
        exportContext.setDocParentLevels(2);

        Provider<FilesystemExportContext> exportContextProvider = this.mocker.getInstance(new DefaultParameterizedType(
            null, Provider.class, FilesystemExportContext.class));
        Mockito.when(exportContextProvider.get()).thenReturn(exportContext);

        WebJarsResourceReference reference = new WebJarsResourceReference("wiki:wiki", Arrays.asList(
            "font-awesome", "7.0.1", "webfonts/fa-regular-400.woff2"));

        // Verify that the returned URL is ok
        assertEquals("../../webjars/font-awesome/7.0.1/webfonts/fa-regular-400.woff2",
            this.mocker.getComponentUnderTest().serialize(reference).serialize());

        // Also verify that the resource has been copied!
        assertTrue(new File(BASEDIR, "webjars/font-awesome/7.0.1/webfonts/fa-regular-400.woff2").exists());
    }

    @Test
    public void serializeCSSResourceWithURLsInIt() throws Exception
    {
        FilesystemExportContext exportContext = new FilesystemExportContext();
        exportContext.setExportDir(BASEDIR);

        Provider<FilesystemExportContext> exportContextProvider = this.mocker.getInstance(new DefaultParameterizedType(
            null, Provider.class, FilesystemExportContext.class));
        Mockito.when(exportContextProvider.get()).thenReturn(exportContext);

        WebJarsResourceReference reference = new WebJarsResourceReference("wiki:wiki", Arrays.asList(
            "font-awesome", "7.0.1", "css/font-awesome.min.css"));

        assertEquals("webjars/font-awesome/7.0.1/css/font-awesome.min.css",
            this.mocker.getComponentUnderTest().serialize(reference).serialize());

        // Also verify that the resources haves been copied!
        assertTrue(new File(BASEDIR, "webjars/font-awesome/7.0.1/css/all.min.css").exists());
        assertTrue(new File(BASEDIR, "webjars/font-awesome/7.0.1/css/v4-shims.min.css").exists());
        assertTrue(new File(BASEDIR, "webjars/font-awesome/7.0.1/webfonts/fa-regular-400.woff2").exists());

    }
}
