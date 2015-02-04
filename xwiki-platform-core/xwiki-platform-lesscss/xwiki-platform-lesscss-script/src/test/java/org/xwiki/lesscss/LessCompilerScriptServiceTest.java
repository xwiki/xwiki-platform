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
package org.xwiki.lesscss;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.lesscss.cache.ColorThemeCache;
import org.xwiki.lesscss.cache.LESSResourcesCache;
import org.xwiki.lesscss.colortheme.ColorTheme;
import org.xwiki.lesscss.colortheme.ColorThemeReferenceFactory;
import org.xwiki.lesscss.colortheme.LESSColorThemeConverter;
import org.xwiki.lesscss.colortheme.NamedColorThemeReference;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.lesscss.compiler.LESSSkinFileCompiler;
import org.xwiki.lesscss.skin.FSSkinReference;
import org.xwiki.lesscss.skin.SkinReferenceFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.lesscss.LessCompilerScriptService}.
 *
 * @since 6.1M1
 * @version $Id$
 */
public class LessCompilerScriptServiceTest
{
    @Rule
    public MockitoComponentMockingRule<LessCompilerScriptService> mocker =
            new MockitoComponentMockingRule<>(LessCompilerScriptService.class);

    private LESSSkinFileCompiler lessCompiler;

    private LESSResourcesCache lessCache;

    private ColorThemeCache colorThemeCache;

    private LESSColorThemeConverter lessColorThemeConverter;

    private AuthorizationManager authorizationManager;

    private Provider<XWikiContext> xcontextProvider;

    private XWikiContext xcontext;

    private SkinReferenceFactory skinReferenceFactory;

    private ColorThemeReferenceFactory colorThemeReferenceFactory;

    @Before
    public void setUp() throws Exception
    {
        lessCompiler = mocker.getInstance(LESSSkinFileCompiler.class);
        lessColorThemeConverter = mocker.getInstance(LESSColorThemeConverter.class);
        lessCache = mocker.getInstance(LESSResourcesCache.class);
        colorThemeCache = mocker.getInstance(ColorThemeCache.class);
        authorizationManager = mocker.getInstance(AuthorizationManager.class);
        skinReferenceFactory = mocker.getInstance(SkinReferenceFactory.class);
        colorThemeReferenceFactory = mocker.getInstance(ColorThemeReferenceFactory.class);
        xcontextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
    }

    @Test
    public void compileSkinFile() throws Exception
    {
        // Test
        mocker.getComponentUnderTest().compileSkinFile("style.less");
        // Verify
        verify(lessCompiler).compileSkinFile("style.less", false);

        // Test
        mocker.getComponentUnderTest().compileSkinFile("style2.less", true);
        // Verify
        verify(lessCompiler).compileSkinFile("style2.less", true);

        // Test
        mocker.getComponentUnderTest().compileSkinFile("style3.less", false);
        // Verify
        verify(lessCompiler).compileSkinFile("style3.less", false);
    }

    @Test
    public void compileSkinFileWithOtherSkin() throws Exception
    {
        // Test
        mocker.getComponentUnderTest().compileSkinFile("style4.less", "skin1");
        // Verify
        verify(lessCompiler).compileSkinFile("style4.less", "skin1", false);

        // Test
        mocker.getComponentUnderTest().compileSkinFile("style4.less", "skin2");
        // Verify
        verify(lessCompiler).compileSkinFile("style4.less", "skin2", false);

        // Test
        mocker.getComponentUnderTest().compileSkinFile("style.less", "skin3", false);
        // Verify
        verify(lessCompiler).compileSkinFile("style.less", "skin3", false);

        // Test
        mocker.getComponentUnderTest().compileSkinFile("style.less", "skin4", true);
        // Verify
        verify(lessCompiler).compileSkinFile("style.less", "skin4", true);
    }

    @Test
    public void compileSkinFileWhenException() throws Exception
    {
        // Mocks
        Exception exception = new LESSCompilerException("Exception with LESS", null);
        when(lessCompiler.compileSkinFile("style.less", false)).thenThrow(exception);

        // Test
        String result = mocker.getComponentUnderTest().compileSkinFile("style.less", false);

        // Verify
        assertEquals("LESSCompilerException: Exception with LESS", result);
    }

    @Test
    public void compileSkinFileWithOtherSkinWhenException() throws Exception
    {
        // Mocks
        Exception exception = new LESSCompilerException("Exception with LESS", null);
        when(lessCompiler.compileSkinFile("style.less", "flamingo", false)).thenThrow(exception);

        // Test
        String result = mocker.getComponentUnderTest().compileSkinFile("style.less", "flamingo", false);

        // Verify
        assertEquals("LESSCompilerException: Exception with LESS", result);
    }

    @Test
    public void getColorThemeFromSkinFile() throws Exception
    {
        // Test
        mocker.getComponentUnderTest().getColorThemeFromSkinFile("style.less");
        // Verify
        verify(lessColorThemeConverter).getColorThemeFromSkinFile("style.less", false);
    }

    @Test
    public void getColorThemeFromSkinFileWithOtherSkin() throws Exception
    {
        // Test
        mocker.getComponentUnderTest().getColorThemeFromSkinFile("style.less", "flamingo");
        // Verify
        verify(lessColorThemeConverter).getColorThemeFromSkinFile("style.less", "flamingo", false);
    }

    @Test
    public void getColorThemeFromSkinFileWithException() throws Exception
    {
        // Mocks
        Exception exception = new LESSCompilerException("Exception with LESS", null);
        when(lessColorThemeConverter.getColorThemeFromSkinFile("style.less", false)).thenThrow(exception);

        // Test
        ColorTheme result = mocker.getComponentUnderTest().getColorThemeFromSkinFile("style.less");

        // Verify
        assertEquals(0, result.size());
    }

    @Test
    public void getColorThemeFromSkinFileWithOtherSkinAndException() throws Exception
    {
        // Mocks
        Exception exception = new LESSCompilerException("Exception with LESS", null);
        when(lessColorThemeConverter.getColorThemeFromSkinFile("style.less", "flamingo", false)).thenThrow(exception);

        // Test
        ColorTheme result = mocker.getComponentUnderTest().getColorThemeFromSkinFile("style.less", "flamingo");

        // Verify
        assertEquals(0, result.size());
    }

    @Test
    public void clearCacheWithRights() throws Exception
    {
        // Mocks
        XWikiDocument doc = mock(XWikiDocument.class);
        DocumentReference authorReference = new DocumentReference("wiki", "Space", "User");
        when(xcontext.getDoc()).thenReturn(doc);
        when(doc.getAuthorReference()).thenReturn(authorReference);
        DocumentReference currentDocReference = new DocumentReference("wiki", "Space", "Page");
        when(doc.getDocumentReference()).thenReturn(currentDocReference);

        when(authorizationManager.hasAccess(Right.PROGRAM, authorReference, currentDocReference)).thenReturn(true);

        // Tests
        assertTrue(mocker.getComponentUnderTest().clearCache());

        // Verify
        verify(lessCache).clear();
        verify(colorThemeCache).clear();
    }

    @Test
    public void clearCacheWithoutRights() throws Exception
    {
        // Mocks
        XWikiDocument doc = mock(XWikiDocument.class);
        DocumentReference authorReference = new DocumentReference("wiki", "Space", "User");
        when(xcontext.getDoc()).thenReturn(doc);
        when(doc.getAuthorReference()).thenReturn(authorReference);
        DocumentReference currentDocReference = new DocumentReference("wiki", "Space", "Page");
        when(doc.getDocumentReference()).thenReturn(currentDocReference);

        when(authorizationManager.hasAccess(Right.PROGRAM, authorReference, currentDocReference)).thenReturn(false);

        // Tests
        assertFalse(mocker.getComponentUnderTest().clearCache());

        // Verify
        verifyZeroInteractions(lessCache);
        verifyZeroInteractions(colorThemeCache);
    }

    @Test
    public void clearCacheFromColorThemeWithRights() throws Exception
    {
        // Mocks
        XWikiDocument doc = mock(XWikiDocument.class);
        DocumentReference authorReference = new DocumentReference("wiki", "Space", "User");
        when(xcontext.getDoc()).thenReturn(doc);
        when(doc.getAuthorReference()).thenReturn(authorReference);
        DocumentReference currentDocReference = new DocumentReference("wiki", "Space", "Page");
        when(doc.getDocumentReference()).thenReturn(currentDocReference);

        when(authorizationManager.hasAccess(Right.PROGRAM, authorReference, currentDocReference)).thenReturn(true);

        when(colorThemeReferenceFactory.createReference("colorTheme")).thenReturn(
                new NamedColorThemeReference("colorTheme"));

        // Tests
        assertTrue(mocker.getComponentUnderTest().clearCacheFromColorTheme("colorTheme"));

        // Verify
        verify(lessCache).clearFromColorTheme(eq(new NamedColorThemeReference("colorTheme")));
        verify(colorThemeCache).clearFromColorTheme(eq(new NamedColorThemeReference("colorTheme")));
    }

    @Test
    public void clearCacheFromColorThemeWithoutRights() throws Exception
    {
        // Mocks
        XWikiDocument doc = mock(XWikiDocument.class);
        DocumentReference authorReference = new DocumentReference("wiki", "Space", "User");
        when(xcontext.getDoc()).thenReturn(doc);
        when(doc.getAuthorReference()).thenReturn(authorReference);
        DocumentReference currentDocReference = new DocumentReference("wiki", "Space", "Page");
        when(doc.getDocumentReference()).thenReturn(currentDocReference);

        when(authorizationManager.hasAccess(Right.PROGRAM, authorReference, currentDocReference)).thenReturn(false);

        // Tests
        assertFalse(mocker.getComponentUnderTest().clearCacheFromColorTheme("colorTheme"));

        // Verify
        verifyZeroInteractions(lessCache);
        verifyZeroInteractions(colorThemeCache);
    }

    @Test
    public void clearCacheFromSkinWithRights() throws Exception
    {
        // Mocks
        XWikiDocument doc = mock(XWikiDocument.class);
        DocumentReference authorReference = new DocumentReference("wiki", "Space", "User");
        when(xcontext.getDoc()).thenReturn(doc);
        when(doc.getAuthorReference()).thenReturn(authorReference);
        DocumentReference currentDocReference = new DocumentReference("wiki", "Space", "Page");
        when(doc.getDocumentReference()).thenReturn(currentDocReference);

        when(authorizationManager.hasAccess(Right.PROGRAM, authorReference, currentDocReference)).thenReturn(true);
        when(skinReferenceFactory.createReference("skin")).thenReturn(new FSSkinReference("skin"));

        // Tests
        assertTrue(mocker.getComponentUnderTest().clearCacheFromSkin("skin"));

        // Verify
        verify(lessCache).clearFromSkin(eq(new FSSkinReference("skin")));
        verify(colorThemeCache).clearFromSkin(eq(new FSSkinReference("skin")));
    }

    @Test
    public void clearCacheFromSkinWithoutRights() throws Exception
    {
        // Mocks
        XWikiDocument doc = mock(XWikiDocument.class);
        DocumentReference authorReference = new DocumentReference("wiki", "Space", "User");
        when(xcontext.getDoc()).thenReturn(doc);
        when(doc.getAuthorReference()).thenReturn(authorReference);
        DocumentReference currentDocReference = new DocumentReference("wiki", "Space", "Page");
        when(doc.getDocumentReference()).thenReturn(currentDocReference);

        when(authorizationManager.hasAccess(Right.PROGRAM, authorReference, currentDocReference)).thenReturn(false);

        // Tests
        assertFalse(mocker.getComponentUnderTest().clearCacheFromSkin("skin"));

        // Verify
        verifyZeroInteractions(lessCache);
        verifyZeroInteractions(colorThemeCache);
    }

}
