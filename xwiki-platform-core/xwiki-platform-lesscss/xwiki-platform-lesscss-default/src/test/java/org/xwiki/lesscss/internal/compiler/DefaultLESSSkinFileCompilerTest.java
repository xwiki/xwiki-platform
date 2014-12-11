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
package org.xwiki.lesscss.internal.compiler;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.lesscss.compiler.LESSCompiler;
import org.xwiki.lesscss.cache.LESSResourcesCache;
import org.xwiki.lesscss.cache.LESSResourcesCache;
import org.xwiki.lesscss.internal.colortheme.CurrentColorThemeGetter;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiEngineContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.lesscss.internal.compiler.DefaultLESSSkinFileCompiler}.
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

    private LESSResourcesCache cache;

    private WikiDescriptorManager wikiDescriptorManager;

    private Provider<XWikiContext> xcontextProvider;

    private CurrentColorThemeGetter currentColorThemeGetter;

    private DocumentReferenceResolver<String> referenceResolver;

    private XWikiContext xcontext;

    private XWiki xwiki;

    private XWikiEngineContext engineContext;

    @Before
    public void setUp() throws Exception
    {
        lessCompiler = mocker.getInstance(LESSCompiler.class);
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        cache = mocker.getInstance(LESSResourcesCache.class);
        currentColorThemeGetter = mocker.getInstance(CurrentColorThemeGetter.class);
        referenceResolver = mocker.getInstance(new DefaultParameterizedType(null, DocumentReferenceResolver.class,
                String.class));
        xcontextProvider = mocker.getInstance(new DefaultParameterizedType(null, Provider.class, XWikiContext.class));
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        xwiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        engineContext = mock(XWikiEngineContext.class);
        when(xwiki.getEngineContext()).thenReturn(engineContext);

        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("wikiId");
        when(xwiki.getSkin(xcontext)).thenReturn("skin");

        when(currentColorThemeGetter.getCurrentColorTheme("default")).thenReturn("wikiId:ColorTheme.MyColorTheme");

    }
    /*

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
        assertEquals("Failed to getResult the file [style2.less] with LESS.", exceptionCaught.getMessage());
        verify(xcontext, never()).put(anyString(), anyString());
    }

    @Test
    public void compileSkinFileWhenSkinIsOnDB() throws Exception
    {
        // Mocks
        prepareMocksForCompilation();
        when(xwiki.getSkin(xcontext)).thenReturn("XWiki.DefaultSkin");
        DocumentReference skinDocRef = new DocumentReference("wikiId", "XWiki", "DefaultSkin");
        WikiReference mainWikiReference = new WikiReference("wikiId");
        when(referenceResolver.resolve(eq("XWiki.DefaultSkin"), eq(mainWikiReference))).thenReturn(skinDocRef);
        when(xwiki.exists(skinDocRef, xcontext)).thenReturn(true);
        DocumentReference skinClassRef = new DocumentReference("wikiId", "XWiki", "XWikiSkins");
        XWikiDocument skinDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(skinDocRef), eq(xcontext))).thenReturn(skinDoc);
        BaseObject skinObj = mock(BaseObject.class);
        when(skinDoc.getXObject(eq(skinClassRef))).thenReturn(skinObj);
        when(skinObj.getStringValue(eq("baseskin"))).thenReturn("skin");

        // Test
        assertEquals("OUTPUT", mocker.getComponentUnderTest().compileSkinFile("style2.less", false));

        // Verify

        verify(cache).get(eq("style2.less"), eq("XWiki.DefaultSkin"), eq("wikiId:ColorTheme.MyColorTheme"));
        verify(cache).set(eq("style2.less"), eq("XWiki.DefaultSkin"), eq("wikiId:ColorTheme.MyColorTheme"),
                eq("OUTPUT"));
        verify(xcontext, never()).put(anyString(), anyString());
    }

    @Test
    public void compileSkinFileWhenSkinIsOnDBWithBaseSkinLoop() throws Exception
    {
        // Mocks
        prepareMocksForCompilation();
        when(xwiki.getSkin(xcontext)).thenReturn("XWiki.DefaultSkin");
        DocumentReference skinDocRef = new DocumentReference("wikiId", "XWiki", "DefaultSkin");
        WikiReference wikiReference = new WikiReference("wikiId");
        when(referenceResolver.resolve(eq("XWiki.DefaultSkin"), eq(wikiReference))).thenReturn(skinDocRef);
        when(xwiki.exists(skinDocRef, xcontext)).thenReturn(true);
        DocumentReference skinClassRef = new DocumentReference("wikiId", "XWiki", "XWikiSkins");
        XWikiDocument skinDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(skinDocRef), eq(xcontext))).thenReturn(skinDoc);
        BaseObject skinObj = mock(BaseObject.class);
        when(skinDoc.getXObject(eq(skinClassRef))).thenReturn(skinObj);
        when(skinObj.getStringValue(eq("baseskin"))).thenReturn("XWiki.DefaultSkin2");

        DocumentReference skinDocRef2 = new DocumentReference("wikiId", "XWiki", "DefaultSkin2");
        when(referenceResolver.resolve(eq("XWiki.DefaultSkin2"), eq(wikiReference))).thenReturn(skinDocRef2);
        when(xwiki.exists(skinDocRef2, xcontext)).thenReturn(true);
        XWikiDocument skinDoc2 = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(skinDocRef2), eq(xcontext))).thenReturn(skinDoc2);
        BaseObject skinObj2 = mock(BaseObject.class);
        when(skinDoc2.getXObject(eq(skinClassRef))).thenReturn(skinObj2);
        when(skinObj2.getStringValue(eq("baseskin"))).thenReturn("XWiki.DefaultSkin");

        // Test
        Exception exception = null;
        try {
            mocker.getComponentUnderTest().compileSkinFile("style2.less", false);
        } catch(Exception e) {
            exception = e;
        }

        // Verify
        assertNotNull(exception);
        assertEquals("Infinite loop of 'baseskin' dependencies [[XWiki.DefaultSkin, XWiki.DefaultSkin2]].",
                exception.getCause().getMessage());
        verify(xcontext, never()).put(anyString(), anyString());
    }

    @Test
    public void compileSkinFileWhenSkinIsOnDBWithNoBaseSkin() throws Exception
    {
        // Mocks
        prepareMocksForCompilation();
        when(xwiki.getSkin(xcontext)).thenReturn("XWiki.DefaultSkin");
        DocumentReference skinDocRef = new DocumentReference("wikiId", "XWiki", "DefaultSkin");
        WikiReference wikiReference = new WikiReference("wikiId");
        when(referenceResolver.resolve(eq("XWiki.DefaultSkin"), eq(wikiReference))).thenReturn(skinDocRef);
        when(xwiki.exists(skinDocRef, xcontext)).thenReturn(true);
        DocumentReference skinClassRef = new DocumentReference("wikiId", "XWiki", "XWikiSkins");
        XWikiDocument skinDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(skinDocRef), eq(xcontext))).thenReturn(skinDoc);
        BaseObject skinObj = mock(BaseObject.class);
        when(skinDoc.getXObject(eq(skinClassRef))).thenReturn(skinObj);
        when(skinObj.getStringValue(eq("baseskin"))).thenReturn(" ");

        // Test
        Exception exception = null;
        try {
            mocker.getComponentUnderTest().compileSkinFile("style2.less", false);
        } catch(Exception e) {
            exception = e;
        }

        // Verify
        assertNotNull(exception);
        assertEquals("Failed to get the base skin of the skin [XWiki.DefaultSkin].", exception.getCause().getMessage());
        verify(xcontext, never()).put(anyString(), anyString());
    }

    @Test
    public void compileSkinFileWhenWithException() throws Exception
    {
        // Mocks
        prepareMocksForCompilation();
        when(xwiki.getSkin(xcontext)).thenReturn("XWiki.DefaultSkin");
        DocumentReference skinDocRef = new DocumentReference("wikiId", "XWiki", "DefaultSkin");
        WikiReference wikiReference = new WikiReference("wikiId");
        when(referenceResolver.resolve(eq("XWiki.DefaultSkin"), eq(wikiReference))).thenReturn(skinDocRef);
        when(xwiki.exists(skinDocRef, xcontext)).thenReturn(true);
        XWikiException exceptionOnLoadingDoc = new XWikiException();
        when(xwiki.getDocument(eq(skinDocRef), eq(xcontext))).thenThrow(exceptionOnLoadingDoc);

        // Test
        Exception exception = null;
        try {
            mocker.getComponentUnderTest().compileSkinFile("style2.less", false);
        } catch(Exception e) {
            exception = e;
        }

        // Verify
        assertNotNull(exception);
        assertEquals("Failed to get the document [wikiId:XWiki.DefaultSkin].", exception.getCause().getMessage());
        verify(xcontext, never()).put(anyString(), anyString());
    }

    @Test
    public void compileSkinFileWhenSkinDocumentHasNoObject() throws Exception
    {
        // Mocks
        prepareMocksForCompilationOnFlamingo();
        when(xwiki.getSkin(xcontext)).thenReturn("flamingo");
        DocumentReference skinDocRef = new DocumentReference("wikiId", "Main", "flamingo");
        WikiReference wikiReference = new WikiReference("wikiId");
        when(referenceResolver.resolve(eq("flamingo"), eq(wikiReference))).thenReturn(skinDocRef);
        when(xwiki.exists(skinDocRef, xcontext)).thenReturn(true);
        XWikiDocument skinDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(skinDocRef), eq(xcontext))).thenReturn(skinDoc);

        // Test
        assertEquals("OUTPUT", mocker.getComponentUnderTest().compileSkinFile("style2.less", false));

        // Verify
        verify(cache).get(eq("style2.less"), eq("flamingo"), eq("wikiId:ColorTheme.MyColorTheme"));
        verify(cache).set(eq("style2.less"), eq("flamingo"), eq("wikiId:ColorTheme.MyColorTheme"), eq("OUTPUT"));
        verify(xcontext, never()).put(anyString(), anyString());
    }

    @Test
    public void compileSkinFileOnSubwiki() throws Exception
    {
       // Mocks
        prepareMocksForCompilation();
        when(xwiki.getSkin(xcontext)).thenReturn("XWiki.DefaultSkin");
        WikiReference currentWikiReference = new WikiReference("wikiId");

        when(cache.get(eq("style.less"), eq("XWiki.DefaultSkin"), eq("wikiId:ColorTheme.MyColorTheme"))).
                thenReturn("SUBWIKI OUTPUT");

        // Test
        assertEquals("SUBWIKI OUTPUT", mocker.getComponentUnderTest().compileSkinFile("style.less", false));
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
        assertEquals("Failed to getResult the file [style2.less] with LESS.", exceptionCaught.getMessage());
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
        assertEquals("Failed to getResult the file [style2.less] with LESS.", exceptionCaught.getMessage());
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
        assertEquals("Failed to getResult the file [style3.less] with LESS.", exceptionCaught.getMessage());
    }

    @Test
    public void compileSkinFileOnSubwikiWhenSkinIsOnMainWiki() throws Exception
    {
        // Mocks
        prepareMocksForCompilation();
        when(xwiki.getSkin(xcontext)).thenReturn("mainWiki:XWiki.DefaultSkin");

        DocumentReference skinDocRef = new DocumentReference("mainWiki", "XWiki", "DefaultSkin");
        when(referenceResolver.resolve(eq("mainWiki:XWiki.DefaultSkin"), any(WikiReference.class)))
                .thenReturn(skinDocRef);
        when(xwiki.exists(eq(skinDocRef), any(XWikiContext.class))).thenReturn(true);
        XWikiDocument skinDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(skinDocRef), any(XWikiContext.class))).thenReturn(skinDoc);
        DocumentReference skinClassRef = new DocumentReference("mainWiki", "XWiki", "XWikiSkins");
        BaseObject skinObj = mock(BaseObject.class);
        when(skinDoc.getXObject(eq(skinClassRef))).thenReturn(skinObj);
        when(skinObj.getStringValue("baseskin")).thenReturn("skin");

        // Test
        assertEquals("OUTPUT", mocker.getComponentUnderTest().compileSkinFile("style2.less", false));
    }
    */
}
