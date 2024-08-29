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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.lesscss.internal.colortheme.DefaultColorThemeReferenceFactory}.
 *
 * @since 6.4M2
 * @version $Id$
 */
public class DefaultColorThemeReferenceFactoryTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultColorThemeReferenceFactory> mocker =
            new MockitoComponentMockingRule<>(DefaultColorThemeReferenceFactory.class);

    private Provider<XWikiContext> xcontextProvider;

    private DocumentReferenceResolver<String> documentReferenceResolver;

    private WikiDescriptorManager wikiDescriptorManager;

    private XWikiContext xcontext;

    private XWiki xwiki;

    @Before
    public void setUp() throws Exception
    {
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        documentReferenceResolver = mocker.getInstance(new DefaultParameterizedType(null, DocumentReferenceResolver.class,
                String.class));
        xcontextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        xwiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);

        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("wikiId");
    }

    @Test
    public void createReferenceWhenItIsAColorTheme() throws Exception
    {
        // Mocks
        DocumentReference colorThemeDocRef = new DocumentReference("otherWiki", "ColorThemes", "colorTheme");
        when(documentReferenceResolver.resolve(eq("colorTheme"), eq(new WikiReference("wikiId")))).thenReturn(
                colorThemeDocRef);
        XWikiDocument colorThemeDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(colorThemeDocRef, xcontext)).thenReturn(colorThemeDoc);
        when(colorThemeDoc.getXObjectSize(eq(new DocumentReference("otherWiki", "ColorThemes", "ColorThemeClass")))).
                thenReturn(1);

        // Test
        assertEquals(new DocumentColorThemeReference(new DocumentReference("otherWiki", "ColorThemes", "colorTheme"),
                        null), mocker.getComponentUnderTest().createReference("colorTheme"));
    }

    @Test
    public void createReferenceWhenItIsAFlamingoTheme() throws Exception
    {
        // Mocks
        DocumentReference colorThemeDocRef = new DocumentReference("otherWiki", "ColorThemes", "colorTheme");
        when(documentReferenceResolver.resolve(eq("colorTheme"), eq(new WikiReference("wikiId")))).thenReturn(
                colorThemeDocRef);
        XWikiDocument colorThemeDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(colorThemeDocRef, xcontext)).thenReturn(colorThemeDoc);
        when(colorThemeDoc.getXObjectSize(eq(new DocumentReference("otherWiki", "ColorThemes", "ColorThemeClass")))).
                thenReturn(0);
        when(colorThemeDoc.getXObjectSize(eq(new DocumentReference("otherWiki", "FlamingoThemesCode", "ThemeClass")))).
                thenReturn(1);

        // Test
        assertEquals(new DocumentColorThemeReference(new DocumentReference("otherWiki", "ColorThemes", "colorTheme"),
                        null), mocker.getComponentUnderTest().createReference("colorTheme"));
    }

    @Test
    public void createReferenceWhenItIsDefault() throws Exception
    {
        // Mocks
        DocumentReference colorThemeDocRef = new DocumentReference("wikiId", "Main", "default");
        when(documentReferenceResolver.resolve("default", new WikiReference("wikiId"))).thenReturn(colorThemeDocRef);
        XWikiDocument colorThemeDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(colorThemeDocRef, xcontext)).thenReturn(colorThemeDoc);
        when(colorThemeDoc.isNew()).thenReturn(true);

        // Test
        assertEquals(new NamedColorThemeReference("default"),
                mocker.getComponentUnderTest().createReference("default"));
    }
}
