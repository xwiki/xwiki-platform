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
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.lesscss.internal.DefaultLESSSkinFileCompiler}.
 *
 * @since 6.1M1
 * @version $Id$
 */
public class DefaultLESSSkinFileCompilerTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultLESSSkinFileCompiler> mocker =
            new MockitoComponentMockingRule<>(DefaultLESSSkinFileCompiler.class);

    private LESSCompiler lessCompiler;

    private LESSSkinFileCache cache;

    private Provider<XWikiContext> xcontextProvider;

    private CurrentColorThemeGetter currentColorThemeGetter;

    private SkinDirectoryGetter skinDirectoryGetter;

    private XWikiContext xcontext;

    private XWiki xwiki;

    private XWikiEngineContext engineContext;

    private LESSContext lessContext;

    @Before
    public void setUp() throws Exception
    {
        lessCompiler = mocker.getInstance(LESSCompiler.class);
        cache = mocker.getInstance(LESSSkinFileCache.class);
        currentColorThemeGetter = mocker.getInstance(CurrentColorThemeGetter.class);
        lessContext = mocker.getInstance(LESSContext.class);
        skinDirectoryGetter = mocker.getInstance(SkinDirectoryGetter.class);
        xcontextProvider = mocker.getInstance(new DefaultParameterizedType(null, Provider.class, XWikiContext.class));
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        xwiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        engineContext = mock(XWikiEngineContext.class);
        when(xwiki.getEngineContext()).thenReturn(engineContext);

        when(xwiki.getSkin(xcontext)).thenReturn("skin");

        when(currentColorThemeGetter.getCurrentColorTheme("default")).thenReturn("wikiId:ColorTheme.MyColorTheme");
        when(skinDirectoryGetter.getSkinDirectory("skin")).thenReturn("/skins/skin");
        when(skinDirectoryGetter.getSkinDirectory("flamingo")).thenReturn("/skins/flamingo");

        when(lessContext.isCacheDisabled()).thenReturn(false);
    }

    private void prepareMocksForCompilation() throws Exception
    {
        when(engineContext.getRealPath("/skins/skin/less")).thenReturn(getClass().getResource("/").getPath());
        when(engineContext.getRealPath("/skins/skin/less/style2.less")).thenReturn(
                getClass().getResource("/style2.less").getPath());

        FileInputStream is = new FileInputStream(getClass().getResource("/style2.less").getFile());
        when(engineContext.getResourceAsStream("/skins/skin/less/style2.less")).thenReturn(is);

        when(xwiki.parseContent("// My LESS file", xcontext)).thenReturn("// My LESS file pre-parsed by Velocity");

        Path[] expectedPaths = { Paths.get(getClass().getResource("/").getPath()) };
        when(lessCompiler.compile(eq("// My LESS file pre-parsed by Velocity"),
                eq(expectedPaths))).thenReturn("OUTPUT");
    }

    private void prepareMocksForCompilationOnFlamingo() throws Exception
    {
        when(engineContext.getRealPath("/skins/flamingo/less")).thenReturn(getClass().getResource("/").getPath());
        when(engineContext.getRealPath("/skins/flamingo/less/style2.less")).thenReturn(
                getClass().getResource("/style2.less").getPath());


        FileInputStream is = new FileInputStream(getClass().getResource("/style2.less").getFile());
        when(engineContext.getResourceAsStream("/skins/flamingo/less/style2.less")).thenReturn(is);

        when(xwiki.parseContent("// My LESS file", xcontext)).thenReturn("// My LESS file pre-parsed by Velocity");

        Path[] expectedPaths = { Paths.get(getClass().getResource("/").getPath()) };
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
        verify(cache).get(eq("style2.less"), eq("skin"), eq("wikiId:ColorTheme.MyColorTheme"));
        verify(cache).set(eq("style2.less"), eq("skin"), eq("wikiId:ColorTheme.MyColorTheme"),
                eq("OUTPUT"));
    }

    @Test
    public void compileSkinFileWhenInCache() throws Exception
    {
        // Mock
        when(cache.get("style2.less", "skin", "wikiId:ColorTheme.MyColorTheme")).thenReturn("OUTPUT");

        // Test
        assertEquals("OUTPUT", mocker.getComponentUnderTest().compileSkinFile("style2.less", false));

        // Verify
        verify(lessCompiler, never()).compile(anyString(), any(Path[].class));
    }

    @Test
    public void compileSkinFileWhenInCacheButForce() throws Exception
    {
        // Mock
        when(cache.get("style2.less", "skin", "wikiId:ColorTheme.MyColorTheme")).thenReturn("OLD OUTPUT");
        prepareMocksForCompilation();

        // Test
        assertEquals("OUTPUT", mocker.getComponentUnderTest().compileSkinFile("style2.less", true));

        // Verify
        verify(cache).set(eq("style2.less"), eq("skin"), eq("wikiId:ColorTheme.MyColorTheme"), eq("OUTPUT"));
    }

    @Test
    public void compileSkinFileWhenInCacheButCacheDisabled() throws Exception
    {
        // Mock
        when(cache.get("style2.less", "skin", "wikiId:ColorTheme.MyColorTheme")).thenReturn("OLD OUTPUT");
        prepareMocksForCompilation();
        when(lessContext.isCacheDisabled()).thenReturn(true);

        // Test
        assertEquals("OUTPUT", mocker.getComponentUnderTest().compileSkinFile("style2.less", true));

        // Verify
        verify(cache, never()).set(eq("style2.less"), eq("skin"), eq("wikiId:ColorTheme.MyColorTheme"), eq("OUTPUT"));
        verify(cache, never()).get(eq("style2.less"), eq("skin"), eq("wikiId:ColorTheme.MyColorTheme"));
    }

    @Test
    public void compileSkinFileWhenInCacheButHTMLExport() throws Exception
    {
        // Mock
        when(cache.get("style2.less", "skin", "wikiId:ColorTheme.MyColorTheme")).thenReturn("OLD OUTPUT");
        prepareMocksForCompilation();
        when(lessContext.isHtmlExport()).thenReturn(true);

        // Test
        assertEquals("OLD OUTPUT", mocker.getComponentUnderTest().compileSkinFile("style2.less", false));

        // Verify
        verify(cache, never()).set(anyString(), anyString(), anyString(), anyString());
        verifyZeroInteractions(lessCompiler);
    }

    @Test
    public void compileSkinFileWhenExceptionWithLESS() throws Exception
    {
        // Mock
        prepareMocksForCompilation();

        Exception exception = new LESSCompilerException("Exception with LESS", null);
        when(lessCompiler.compile(anyString(), any(Path[].class))).thenThrow(exception);

        // Test
        Exception exceptionCaught = null;
        try {
            mocker.getComponentUnderTest().compileSkinFile("style2.less", true);
        } catch (LESSCompilerException e) {
            exceptionCaught = e;
        }

        // Verify
        assertNotNull(exceptionCaught);
        assertEquals(exception, exceptionCaught.getCause());
        assertEquals("Failed to compile the file [style2.less] with LESS.", exceptionCaught.getMessage());
        verify(xcontext, never()).put(anyString(), anyString());
    }

    @Test
    public void compileSkinFileOnOtherSkin() throws Exception
    {
        // Mocks
        prepareMocksForCompilationOnFlamingo();

        // Test
        assertEquals("OUTPUT", mocker.getComponentUnderTest().compileSkinFile("style2.less", "flamingo", false));

        // Verify
        verify(xcontext).put("skin", "flamingo");
        verify(xcontext).put("skin", "skin");
    }

    @Test
    public void compileSkinFileOnOtherSkinWithException() throws Exception
    {
        // Mocks
        prepareMocksForCompilationOnFlamingo();

        Exception exception = new LESSCompilerException("Exception with LESS", null);
        when(lessCompiler.compile(anyString(), any(Path[].class))).thenThrow(exception);

        // Test
        Exception exceptionCaught = null;
        try {
            mocker.getComponentUnderTest().compileSkinFile("style2.less", "flamingo", true);
        } catch(LESSCompilerException e) {
            exceptionCaught = e;
        }

        // Verify
        assertNotNull(exceptionCaught);
        assertEquals(exception, exceptionCaught.getCause());
        assertEquals("Failed to compile the file [style2.less] with LESS.", exceptionCaught.getMessage());
    }

    @Test
    public void compileSkinFileWhenDirectoryDoesNotExist() throws Exception
    {
        when(engineContext.getRealPath("/skins/flamingo/less")).thenReturn("ighgzuheubigvugvbzekvbzekvuzkkkhguiiiii");

        // Test
        Exception exceptionCaught = null;
        try {
            mocker.getComponentUnderTest().compileSkinFile("style2.less", "flamingo", true);
        } catch(LESSCompilerException e) {
            exceptionCaught = e;
        }

        // Verify
        assertNotNull(exceptionCaught);
        assertEquals("The path [/skins/flamingo/less] is not a directory or does not exists.",
                exceptionCaught.getCause().getMessage());
        assertEquals("Failed to compile the file [style2.less] with LESS.", exceptionCaught.getMessage());
    }

    @Test
    public void compileSkinFileWhenFileDoesNotExist() throws Exception
    {
        prepareMocksForCompilation();

        when(engineContext.getRealPath("/skins/skin/less/style3.less")).thenReturn("ezuizfgyzyfgzerkyfgzerukygfzerkuy");

        // Test
        Exception exceptionCaught = null;
        try {
            mocker.getComponentUnderTest().compileSkinFile("style3.less", true);
        } catch(LESSCompilerException e) {
            exceptionCaught = e;
        }

        // Verify
        assertNotNull(exceptionCaught);
        assertEquals("The path [/skins/skin/less/style3.less] is not a file or does not exists.",
                exceptionCaught.getCause().getMessage());
        assertEquals("Failed to compile the file [style3.less] with LESS.", exceptionCaught.getMessage());
    }
}
