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
package org.xwiki.lesscss.internal;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.lesscss.LESSCompiler;
import org.xwiki.lesscss.LESSCompilerException;
import org.xwiki.lesscss.LESSSkinFileCache;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiEngineContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.lesscss.internal.DefaultLESSSkinFileCompiler}.
 *
 * @since 6.1M2
 * @version $Id$
 */
public class DefaultLESSSkinFileCompilerTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultLESSSkinFileCompiler> mocker =
            new MockitoComponentMockingRule(DefaultLESSSkinFileCompiler.class);

    private LESSCompiler lessCompiler;

    private LESSSkinFileCache cache;

    private Provider<XWikiContext> xcontextProvider;

    private XWikiContext xcontext;

    private XWiki xwiki;

    private XWikiEngineContext engineContext;

    @Before
    public void setUp() throws Exception
    {
        lessCompiler = mocker.getInstance(LESSCompiler.class);
        cache = mocker.getInstance(LESSSkinFileCache.class);
        xcontextProvider = mocker.getInstance(new DefaultParameterizedType(null, Provider.class, XWikiContext.class));
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        xwiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        engineContext = mock(XWikiEngineContext.class);
        when(xwiki.getEngineContext()).thenReturn(engineContext);
    }

    private void prepareMocksForCompilation() throws Exception
    {
        when(xwiki.getBaseSkin(xcontext)).thenReturn("flamingo");
        when(engineContext.getRealPath("/skins/flamingo/less")).thenReturn("~/");

        FileInputStream is = new FileInputStream(getClass().getResource("/style2.less").getFile());
        when(engineContext.getResourceAsStream("/skins/flamingo/less/style2.less")).thenReturn(is);

        when(xwiki.parseContent("// My LESS file", xcontext)).thenReturn("// My LESS file pre-parsed by Velocity");

        Path[] expectedPaths = { Paths.get("~/") };
        when(lessCompiler.compile(eq("// My LESS file pre-parsed by Velocity"),
                eq(expectedPaths))).thenReturn("OUTPUT");
    }

    @Test
    public void compileSkinFile() throws Exception
    {
        // Mocks
        prepareMocksForCompilation();

        // Test
        assertEquals("OUTPUT", mocker.getComponentUnderTest().compileSkinFile("style2.less", false));

        // Verify
        verify(cache).get(eq("style2.less"));
        verify(cache).set(eq("style2.less"), eq("OUTPUT"));
    }

    @Test
    public void compileSkinFileWhenInCache() throws Exception
    {
        // Mock
        when(cache.get("style2.less")).thenReturn("OUTPUT");

        // Test
        assertEquals("OUTPUT", mocker.getComponentUnderTest().compileSkinFile("style2.less", false));

        // Verify
        verify(lessCompiler, never()).compile(anyString(), any(Path[].class));

    }

    @Test
    public void compileSkinFileWhenInCacheButForce() throws Exception
    {
        // Mock
        when(cache.get("style2.less")).thenReturn("OLD OUTPUT");
        prepareMocksForCompilation();

        // Test
        assertEquals("OUTPUT", mocker.getComponentUnderTest().compileSkinFile("style2.less", true));

        // Verify
        verify(cache).set(eq("style2.less"), eq("OUTPUT"));
    }

    @Test
    public void compileSkinFileWhenExceptionWithLESS() throws Exception
    {
        // Mock
        prepareMocksForCompilation();

        Exception exception = new LESSCompilerException("Exception with LESS", null);
        when(lessCompiler.compile(anyString(), any(Path[].class))).thenThrow(exception);

        // Test
        Exception exceptionCaughed = null;
        try {
            mocker.getComponentUnderTest().compileSkinFile("style2.less", true);
        } catch(LESSCompilerException e) {
            exceptionCaughed = e;
        }

        // Verify
        assertNotNull(exceptionCaughed);
        assertEquals(exception, exceptionCaughed.getCause());
        assertEquals("Failed to compile the file [style2.less] with LESS.", exceptionCaughed.getMessage());
    }
}
