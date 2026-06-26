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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.lesscss.compiler.LESSCompiler;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.lesscss.internal.cache.ColorThemeCache;
import org.xwiki.lesscss.internal.cache.LESSResourcesCache;
import org.xwiki.lesscss.internal.colortheme.ColorTheme;
import org.xwiki.lesscss.internal.colortheme.ColorThemeReference;
import org.xwiki.lesscss.internal.colortheme.ColorThemeReferenceFactory;
import org.xwiki.lesscss.internal.colortheme.LESSColorThemeConverter;
import org.xwiki.lesscss.internal.skin.SkinReference;
import org.xwiki.lesscss.internal.skin.SkinReferenceFactory;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.lesscss.resources.LESSResourceReferenceFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.lesscss.LessCompilerScriptService}.
 *
 * @since 6.1M1
 * @version $Id$
 */
@ComponentTest
class LessCompilerScriptServiceTest
{
    @InjectMockComponents
    private LessCompilerScriptService lessCompilerScriptService;

    @MockComponent
    private LESSCompiler lessCompiler;

    @MockComponent
    private LESSResourceReferenceFactory lessResourceReferenceFactory;

    @MockComponent
    private LESSResourcesCache lessCache;

    @MockComponent
    private ColorThemeCache colorThemeCache;

    @MockComponent
    private LESSColorThemeConverter lessColorThemeConverter;

    @MockComponent
    private ContextualAuthorizationManager authorizationManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private SkinReferenceFactory skinReferenceFactory;

    @MockComponent
    private ColorThemeReferenceFactory colorThemeReferenceFactory;

    private XWikiContext xcontext;

    @BeforeEach
    void setUp()
    {
        this.xcontext = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
    }

    @Test
    void compileSkinFile() throws Exception
    {
        // Mock
        LESSResourceReference style = mock(LESSResourceReference.class);
        when(this.lessResourceReferenceFactory.createReferenceForSkinFile("style.less")).thenReturn(style);
        // Test
        this.lessCompilerScriptService.compileSkinFile("style.less");
        // Verify
        verify(this.lessCompiler).compile(style, false, true, false);

        // Mock
        LESSResourceReference style2 = mock(LESSResourceReference.class);
        when(this.lessResourceReferenceFactory.createReferenceForSkinFile("style2.less")).thenReturn(style2);
        // Test
        this.lessCompilerScriptService.compileSkinFile("style2.less", true);
        // Verify
        verify(this.lessCompiler).compile(style2, false, true, true);

        // Mock
        LESSResourceReference style3 = mock(LESSResourceReference.class);
        when(this.lessResourceReferenceFactory.createReferenceForSkinFile("style3.less")).thenReturn(style3);
        // Test
        this.lessCompilerScriptService.compileSkinFile("style3.less", false);
        // Verify
        verify(this.lessCompiler).compile(style3, false, true, false);
    }

    @Test
    void compileSkinFileWithOtherSkin() throws Exception
    {
        // Mock
        LESSResourceReference style = mock(LESSResourceReference.class);
        when(this.lessResourceReferenceFactory.createReferenceForSkinFile("style.less")).thenReturn(style);

        // Test
        this.lessCompilerScriptService.compileSkinFile("style.less", "skin1");
        // Verify
        verify(this.lessCompiler).compile(style, false, true, "skin1", false);

        // Test
        this.lessCompilerScriptService.compileSkinFile("style.less", "skin2");
        // Verify
        verify(this.lessCompiler).compile(style, false, true, "skin2", false);

        // Test
        this.lessCompilerScriptService.compileSkinFile("style.less", "skin3", false);
        // Verify
        verify(this.lessCompiler).compile(style, false, true, "skin3", false);

        // Test
        this.lessCompilerScriptService.compileSkinFile("style.less", "skin4", true);
        // Verify
        verify(this.lessCompiler).compile(style, false, true, "skin4", true);
    }

    @Test
    void compileSkinFileWhenException() throws Exception
    {
        // Mocks
        LESSResourceReference style = mock(LESSResourceReference.class);
        when(this.lessResourceReferenceFactory.createReferenceForSkinFile("style.less")).thenReturn(style);

        Exception exception = new LESSCompilerException("Exception with LESS", null);
        when(this.lessCompiler.compile(style, false, true, false)).thenThrow(exception);

        // Test
        String result = this.lessCompilerScriptService.compileSkinFile("style.less", false);

        // Verify
        assertEquals("LESSCompilerException: Exception with LESS", result);
    }

    @Test
    void compileSkinFileWithOtherSkinWhenException() throws Exception
    {
        // Mocks
        LESSResourceReference style = mock(LESSResourceReference.class);
        when(this.lessResourceReferenceFactory.createReferenceForSkinFile("style.less")).thenReturn(style);

        Exception exception = new LESSCompilerException("Exception with LESS", null);
        when(this.lessCompiler.compile(style, false, true, "flamingo", false)).thenThrow(exception);

        // Test
        String result = this.lessCompilerScriptService.compileSkinFile("style.less", "flamingo", false);

        // Verify
        assertEquals("LESSCompilerException: Exception with LESS", result);
    }

    @Test
    void getColorThemeFromSkinFile() throws Exception
    {
        // Test
        this.lessCompilerScriptService.getColorThemeFromSkinFile("style.less");
        // Verify
        verify(this.lessColorThemeConverter).getColorThemeFromSkinFile("style.less", false);
    }

    @Test
    void getColorThemeFromSkinFileWithOtherSkin() throws Exception
    {
        // Test
        this.lessCompilerScriptService.getColorThemeFromSkinFile("style.less", "flamingo");
        // Verify
        verify(this.lessColorThemeConverter).getColorThemeFromSkinFile("style.less", "flamingo", false);
    }

    @Test
    void getColorThemeFromSkinFileWithException() throws Exception
    {
        // Mocks
        Exception exception = new LESSCompilerException("Exception with LESS", null);
        when(this.lessColorThemeConverter.getColorThemeFromSkinFile("style.less", false)).thenThrow(exception);

        // Test
        ColorTheme result = this.lessCompilerScriptService.getColorThemeFromSkinFile("style.less");

        // Verify
        assertEquals(0, result.size());
    }

    @Test
    void getColorThemeFromSkinFileWithOtherSkinAndException() throws Exception
    {
        // Mocks
        Exception exception = new LESSCompilerException("Exception with LESS", null);
        when(this.lessColorThemeConverter.getColorThemeFromSkinFile("style.less", "flamingo", false))
            .thenThrow(exception);

        // Test
        ColorTheme result = this.lessCompilerScriptService.getColorThemeFromSkinFile("style.less", "flamingo");

        // Verify
        assertEquals(0, result.size());
    }

    @Test
    void clearCacheWithRights()
    {
        // Mocks
        XWikiDocument doc = mock(XWikiDocument.class);
        DocumentReference authorReference = new DocumentReference("wiki", "Space", "User");
        when(this.xcontext.getDoc()).thenReturn(doc);
        when(doc.getAuthorReference()).thenReturn(authorReference);
        DocumentReference currentDocReference = new DocumentReference("wiki", "Space", "Page");
        when(doc.getDocumentReference()).thenReturn(currentDocReference);

        when(this.authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(true);

        // Tests
        assertTrue(this.lessCompilerScriptService.clearCache());

        // Verify
        verify(this.lessCache).clear();
        verify(this.colorThemeCache).clear();
    }

    @Test
    void clearCacheWithoutRights()
    {
        // Mocks
        XWikiDocument doc = mock(XWikiDocument.class);
        DocumentReference authorReference = new DocumentReference("wiki", "Space", "User");
        when(this.xcontext.getDoc()).thenReturn(doc);
        when(doc.getAuthorReference()).thenReturn(authorReference);
        DocumentReference currentDocReference = new DocumentReference("wiki", "Space", "Page");
        when(doc.getDocumentReference()).thenReturn(currentDocReference);

        when(this.authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(false);

        // Tests
        assertFalse(this.lessCompilerScriptService.clearCache());

        // Verify
        verifyNoInteractions(this.lessCache);
        verifyNoInteractions(this.colorThemeCache);
    }

    @Test
    void clearCacheFromColorThemeWithRights() throws Exception
    {
        // Mocks
        XWikiDocument doc = mock(XWikiDocument.class);
        DocumentReference authorReference = new DocumentReference("wiki", "Space", "User");
        when(this.xcontext.getDoc()).thenReturn(doc);
        when(doc.getAuthorReference()).thenReturn(authorReference);
        DocumentReference currentDocReference = new DocumentReference("wiki", "Space", "Page");
        when(doc.getDocumentReference()).thenReturn(currentDocReference);

        when(this.authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(true);

        ColorThemeReference colorThemeReference = mock(ColorThemeReference.class);
        when(this.colorThemeReferenceFactory.createReference("colorTheme")).thenReturn(colorThemeReference);

        // Tests
        assertTrue(this.lessCompilerScriptService.clearCacheFromColorTheme("colorTheme"));

        // Verify
        verify(this.lessCache).clearFromColorTheme(colorThemeReference);
        verify(this.colorThemeCache).clearFromColorTheme(colorThemeReference);
    }

    @Test
    void clearCacheFromColorThemeWithoutRights()
    {
        // Mocks
        XWikiDocument doc = mock(XWikiDocument.class);
        DocumentReference authorReference = new DocumentReference("wiki", "Space", "User");
        when(this.xcontext.getDoc()).thenReturn(doc);
        when(doc.getAuthorReference()).thenReturn(authorReference);
        DocumentReference currentDocReference = new DocumentReference("wiki", "Space", "Page");
        when(doc.getDocumentReference()).thenReturn(currentDocReference);

        when(this.authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(false);

        // Tests
        assertFalse(this.lessCompilerScriptService.clearCacheFromColorTheme("colorTheme"));

        // Verify
        verifyNoInteractions(this.lessCache);
        verifyNoInteractions(this.colorThemeCache);
    }

    @Test
    void clearCacheFromColorThemeWithException() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(true);

        LESSCompilerException lessCompilerException = new LESSCompilerException("Test Exception");
        when(this.colorThemeReferenceFactory.createReference("colorTheme")).thenThrow(lessCompilerException);

        assertFalse(this.lessCompilerScriptService.clearCacheFromColorTheme("colorTheme"));

        verifyNoInteractions(this.lessCache);
        verifyNoInteractions(this.colorThemeCache);
    }

    @Test
    void clearCacheFromSkinWithRights() throws Exception
    {
        // Mocks
        XWikiDocument doc = mock(XWikiDocument.class);
        DocumentReference authorReference = new DocumentReference("wiki", "Space", "User");
        when(this.xcontext.getDoc()).thenReturn(doc);
        when(doc.getAuthorReference()).thenReturn(authorReference);
        DocumentReference currentDocReference = new DocumentReference("wiki", "Space", "Page");
        when(doc.getDocumentReference()).thenReturn(currentDocReference);

        when(this.authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(true);

        SkinReference skinReference = mock(SkinReference.class);
        when(this.skinReferenceFactory.createReference("skin")).thenReturn(skinReference);

        // Tests
        assertTrue(this.lessCompilerScriptService.clearCacheFromSkin("skin"));

        // Verify
        verify(this.lessCache).clearFromSkin(skinReference);
        verify(this.colorThemeCache).clearFromSkin(skinReference);
    }

    @Test
    void clearCacheFromSkinWithoutRights()
    {
        // Mocks
        XWikiDocument doc = mock(XWikiDocument.class);
        DocumentReference authorReference = new DocumentReference("wiki", "Space", "User");
        when(this.xcontext.getDoc()).thenReturn(doc);
        when(doc.getAuthorReference()).thenReturn(authorReference);
        DocumentReference currentDocReference = new DocumentReference("wiki", "Space", "Page");
        when(doc.getDocumentReference()).thenReturn(currentDocReference);

        when(this.authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(false);

        // Tests
        assertFalse(this.lessCompilerScriptService.clearCacheFromSkin("skin"));

        // Verify
        verifyNoInteractions(this.lessCache);
        verifyNoInteractions(this.colorThemeCache);
    }

    @Test
    void clearCacheFromSkinWithException() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(true);

        LESSCompilerException exception = new LESSCompilerException("test");
        when(this.skinReferenceFactory.createReference("skin")).thenThrow(exception);

        assertFalse(this.lessCompilerScriptService.clearCacheFromSkin("skin"));

        verifyNoInteractions(this.lessCache);
        verifyNoInteractions(this.colorThemeCache);
    }
}
