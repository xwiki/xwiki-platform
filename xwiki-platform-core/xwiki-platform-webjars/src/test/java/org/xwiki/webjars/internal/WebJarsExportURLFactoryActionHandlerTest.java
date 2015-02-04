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
import java.net.URL;
import java.net.URLEncoder;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.ExportURLFactoryContext;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WebJarsExportURLFactoryActionHandler}.
 *
 * @version $Id$
 * @since 6.2RC1
 */
public class WebJarsExportURLFactoryActionHandlerTest
{
    private static final File BASEDIR = new File(System.getProperty("java.io.tmpdir"), "xwikitest");

    @Rule
    public MockitoComponentMockingRule<WebJarsExportURLFactoryActionHandler> mocker =
        new MockitoComponentMockingRule<>(WebJarsExportURLFactoryActionHandler.class);

    private ClassLoader originalThreadContextClassLoader;

    @Before
    public void setUp() throws Exception
    {
        this.originalThreadContextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(WebJarsExportURLFactoryActionHandlerTest.class.getClassLoader());
        FileUtils.deleteDirectory(BASEDIR);
    }

    @After
    public void tearDown()
    {
        Thread.currentThread().setContextClassLoader(this.originalThreadContextClassLoader);
    }

    @Test
    public void createURL() throws Exception
    {
        XWikiContext xcontext = mock(XWikiContext.class);
        ExportURLFactoryContext factoryContext = new ExportURLFactoryContext();
        factoryContext.setExportDir(BASEDIR);
        URL result = this.mocker.getComponentUnderTest().createURL("resources", "path", "value="
            + URLEncoder.encode("angular-paginate-anything/2.5.3/paginate-anything.js", "UTF-8"), null, "xwiki",
                xcontext, factoryContext);

        // Verify that the returned URL is ok
        assertEquals("file://webjars/META-INF/resources/webjars/angular-paginate-anything/2.5.3/paginate-anything.js",
            result.toExternalForm());

        // Also verify that the resource has been copied!
        assertTrue(new File(BASEDIR,
            "webjars/META-INF/resources/webjars/angular-paginate-anything/2.5.3/paginate-anything.js").exists());
    }
}
