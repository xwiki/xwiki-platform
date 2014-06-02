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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiEngineContext;
import com.xpn.xwiki.web.XWikiRequest;

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
 * @since 6.1M1
 * @version $Id$
 */
public class DefaultLESSSkinFileCompilerTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultLESSSkinFileCompiler> mocker =
            new MockitoComponentMockingRule(DefaultLESSSkinFileCompiler.class);

    private LESSCompiler lessCompiler;

    private LESSSkinFileCache cache;

    private WikiDescriptorManager wikiDescriptorManager;

    private Provider<XWikiContext> xcontextProvider;

    private XWikiContext xcontext;

    private XWiki xwiki;

    private XWikiEngineContext engineContext;

    private DocumentReferenceResolver<String> referenceResolver;

    private EntityReferenceSerializer<String> referenceSerializer;

    @Before
    public void setUp() throws Exception
    {
        lessCompiler = mocker.getInstance(LESSCompiler.class);
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        referenceResolver = mocker.getInstance(new DefaultParameterizedType(null, DocumentReferenceResolver.class,
                        String.class));
        referenceSerializer = mocker.getInstance(new DefaultParameterizedType(null, EntityReferenceSerializer.class,
                String.class));        
        cache = mocker.getInstance(LESSSkinFileCache.class);
        xcontextProvider = mocker.getInstance(new DefaultParameterizedType(null, Provider.class, XWikiContext.class));
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        xwiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        engineContext = mock(XWikiEngineContext.class);
        when(xwiki.getEngineContext()).thenReturn(engineContext);
        XWikiRequest request = mock(XWikiRequest.class);
        when(xcontext.getRequest()).thenReturn(request);
        when(request.getParameter("colorTheme")).thenReturn("myColorTheme");
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("wikiId");
        when(xwiki.getBaseSkin(xcontext)).thenReturn("skin");
        DocumentReference colorThemeReference = new DocumentReference("xwiki", "XWiki", "MyColorTheme");
        when(referenceResolver.resolve("myColorTheme")).thenReturn(colorThemeReference);
        when(referenceSerializer.serialize(colorThemeReference)).thenReturn("xwiki:ColorTheme.MyColorTheme");
        when(xwiki.exists(colorThemeReference, xcontext)).thenReturn(true);
    }

    private void prepareMocksForCompilation() throws Exception
    {
        when(engineContext.getRealPath("/skins/skin/less")).thenReturn("~/");

        FileInputStream is = new FileInputStream(getClass().getResource("/style2.less").getFile());
        when(engineContext.getResourceAsStream("/skins/skin/less/style2.less")).thenReturn(is);

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
        verify(cache).get(eq("style2.less"), eq("wikiId"), eq("skin"), eq("xwiki:ColorTheme.MyColorTheme"));
        verify(cache).set(eq("style2.less"), eq("wikiId"), eq("skin"), eq("xwiki:ColorTheme.MyColorTheme"), eq("OUTPUT"));
    }

    @Test
    public void compileSkinFileWhenInCache() throws Exception
    {
        // Mock
        when(cache.get("style2.less", "wikiId", "skin", "xwiki:ColorTheme.MyColorTheme")).thenReturn("OUTPUT");

        // Test
        assertEquals("OUTPUT", mocker.getComponentUnderTest().compileSkinFile("style2.less", false));

        // Verify
        verify(lessCompiler, never()).compile(anyString(), any(Path[].class));

    }

    @Test
    public void compileSkinFileWhenInCacheButForce() throws Exception
    {
        // Mock
        when(cache.get("style2.less", "wikiId", "skin", "xwiki:ColorTheme.MyColorTheme")).thenReturn("OLD OUTPUT");
        prepareMocksForCompilation();

        // Test
        assertEquals("OUTPUT", mocker.getComponentUnderTest().compileSkinFile("style2.less", true));

        // Verify
        verify(cache).set(eq("style2.less"), eq("wikiId"), eq("skin"), eq("xwiki:ColorTheme.MyColorTheme"), eq("OUTPUT"));
    }

    @Test
    public void compileSkinFileWhenColorThemeDoesNotExist() throws Exception
    {
        // Mock
        when(cache.get("style2.less", "wikiId", "skin", "default")).thenReturn("DEFAULT COLOR THEME");
        DocumentReference colorThemeReference = new DocumentReference("xwiki", "XWiki", "invalidColorTheme");
        when(referenceResolver.resolve("invalidColorTheme")).thenReturn(colorThemeReference);
        when(xwiki.exists(colorThemeReference, xcontext)).thenReturn(false);
        prepareMocksForCompilation();
        XWikiRequest request = mock(XWikiRequest.class);
        when(xcontext.getRequest()).thenReturn(request);
        when(request.getParameter("colorTheme")).thenReturn("invalidColorTheme");

        // Test
        assertEquals("DEFAULT COLOR THEME", mocker.getComponentUnderTest().compileSkinFile("style2.less", false));
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
        } catch(LESSCompilerException e) {
            exceptionCaught = e;
        }

        // Verify
        assertNotNull(exceptionCaught);
        assertEquals(exception, exceptionCaught.getCause());
        assertEquals("Failed to compile the file [style2.less] with LESS.", exceptionCaught.getMessage());
    }
}
