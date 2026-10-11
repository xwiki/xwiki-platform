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
package org.xwiki.lesscss.internal.colortheme;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.lesscss.internal.colortheme.DefaultColorThemeReferenceFactory}.
 *
 * @version $Id$
 * @since 6.4M2
 */
@ComponentTest
class DefaultColorThemeReferenceFactoryTest
{
    @InjectMockComponents
    private DefaultColorThemeReferenceFactory defaultColorThemeReferenceFactory;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    private XWikiContext xcontext;

    private XWiki xwiki;

    @BeforeEach
    void setUp()
    {
        this.xcontext = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        this.xwiki = mock(XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);

        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("wikiId");
    }

    @Test
    void createReferenceWhenItIsAColorTheme() throws Exception
    {
        // Mocks
        DocumentReference colorThemeDocRef = new DocumentReference("otherWiki", "ColorThemes", "colorTheme");
        when(this.documentReferenceResolver.resolve("colorTheme", new WikiReference("wikiId")))
            .thenReturn(colorThemeDocRef);
        XWikiDocument colorThemeDoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(colorThemeDocRef, this.xcontext)).thenReturn(colorThemeDoc);
        when(colorThemeDoc.getXObjectSize(new DocumentReference("otherWiki", "ColorThemes", "ColorThemeClass")))
            .thenReturn(1);

        // Test
        assertEquals(new DocumentColorThemeReference(new DocumentReference("otherWiki", "ColorThemes", "colorTheme"),
            null), this.defaultColorThemeReferenceFactory.createReference("colorTheme"));
    }

    @Test
    void createReferenceWhenItIsAFlamingoTheme() throws Exception
    {
        // Mocks
        DocumentReference colorThemeDocRef = new DocumentReference("otherWiki", "ColorThemes", "colorTheme");
        when(this.documentReferenceResolver.resolve("colorTheme", new WikiReference("wikiId")))
            .thenReturn(colorThemeDocRef);
        XWikiDocument colorThemeDoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(colorThemeDocRef, this.xcontext)).thenReturn(colorThemeDoc);
        when(colorThemeDoc.getXObjectSize(new DocumentReference("otherWiki", "ColorThemes", "ColorThemeClass")))
            .thenReturn(0);
        when(colorThemeDoc.getXObjectSize(new DocumentReference("otherWiki", "FlamingoThemesCode", "ThemeClass")))
            .thenReturn(1);

        // Test
        assertEquals(new DocumentColorThemeReference(new DocumentReference("otherWiki", "ColorThemes", "colorTheme"),
            null), this.defaultColorThemeReferenceFactory.createReference("colorTheme"));
    }

    @Test
    void createReferenceWhenItIsDefault() throws Exception
    {
        // Mocks
        DocumentReference colorThemeDocRef = new DocumentReference("wikiId", "Main", "default");
        when(this.documentReferenceResolver.resolve("default", new WikiReference("wikiId"))).thenReturn(
            colorThemeDocRef);
        XWikiDocument colorThemeDoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(colorThemeDocRef, this.xcontext)).thenReturn(colorThemeDoc);
        when(colorThemeDoc.isNew()).thenReturn(true);

        // Test
        assertEquals(new NamedColorThemeReference("default"),
            this.defaultColorThemeReferenceFactory.createReference("default"));
    }
}
