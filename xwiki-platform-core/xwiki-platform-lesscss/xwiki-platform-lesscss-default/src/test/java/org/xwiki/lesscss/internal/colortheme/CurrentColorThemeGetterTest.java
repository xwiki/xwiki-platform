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
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.lesscss.internal.colortheme.CurrentColorThemeGetter}.
 *
 * @version $Id$
 * @since 6.4M2
 */
@ComponentTest
class CurrentColorThemeGetterTest
{
    @InjectMockComponents
    private CurrentColorThemeGetter currentColorThemeGetter;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private AuthorizationManager authorizationManager;

    private XWikiContext xcontext;

    private XWiki xwiki;

    private XWikiRequest request;

    @BeforeEach
    void setUp() throws Exception
    {
        this.xcontext = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        this.xwiki = mock(XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);

        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("wikiId");
        this.request = mock(XWikiRequest.class);
        when(this.xcontext.getRequest()).thenReturn(this.request);
        DocumentReference colorThemeReference = new DocumentReference("wikiId", "XWiki", "MyColorTheme");
        WikiReference mainWikiReference = new WikiReference("wikiId");
        when(this.documentReferenceResolver.resolve("myColorTheme", mainWikiReference))
            .thenReturn(colorThemeReference);
        when(this.entityReferenceSerializer.serialize(colorThemeReference))
            .thenReturn("wikiId:ColorTheme.MyColorTheme");
        when(this.xwiki.exists(colorThemeReference, this.xcontext)).thenReturn(true);
        DocumentReference currentUser = new DocumentReference("xwiki", "XWiki", "CurrentUser");
        when(this.xcontext.getUserReference()).thenReturn(currentUser);
        when(this.authorizationManager.hasAccess(eq(Right.VIEW), any(DocumentReference.class),
            any(DocumentReference.class))).thenReturn(true);
    }

    @Test
    void getCurrentColorThemeWhenRequestParameter()
    {
        when(this.request.getParameter("colorTheme")).thenReturn("myColorTheme");
        assertEquals("wikiId:ColorTheme.MyColorTheme", this.currentColorThemeGetter.getCurrentColorTheme("default"));
    }

    @Test
    void getCurrentColorThemeWhenNoRequestParameter()
    {
        when(this.xwiki.getUserPreference(eq("colorTheme"), any(XWikiContext.class))).thenReturn("myColorTheme");
        assertEquals("wikiId:ColorTheme.MyColorTheme", this.currentColorThemeGetter.getCurrentColorTheme("default"));
    }

    @Test
    void getCurrentColorThemeFallback() throws Exception
    {
        when(this.request.getParameter("colorTheme")).thenReturn("myColorTheme");
        when(this.xwiki.exists(any(DocumentReference.class), eq(this.xcontext))).thenReturn(false);
        assertEquals("fallback", this.currentColorThemeGetter.getCurrentColorTheme("fallback"));
        assertEquals("error", this.currentColorThemeGetter.getCurrentColorTheme("error"));
    }

    @Test
    void getCurrentColorWithAndWithoutRight()
    {
        when(this.request.getParameter("colorTheme")).thenReturn("myColorTheme");
        when(this.authorizationManager.hasAccess(eq(Right.VIEW), any(DocumentReference.class),
            any(DocumentReference.class)))
            .thenReturn(false);
        assertEquals("fallback", this.currentColorThemeGetter.getCurrentColorTheme(true, "fallback"));
        assertEquals("wikiId:ColorTheme.MyColorTheme",
            this.currentColorThemeGetter.getCurrentColorTheme(false, "fallback"));
    }
}
