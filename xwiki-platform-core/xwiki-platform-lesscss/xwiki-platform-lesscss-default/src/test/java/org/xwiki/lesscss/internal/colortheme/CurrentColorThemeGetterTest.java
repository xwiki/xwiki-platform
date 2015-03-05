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
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.lesscss.internal.colortheme.CurrentColorThemeGetter}.
 *
 * @since 6.4M2
 * @version $Id$
 */
public class CurrentColorThemeGetterTest
{
    @Rule
    public MockitoComponentMockingRule<CurrentColorThemeGetter> mocker =
            new MockitoComponentMockingRule<>(CurrentColorThemeGetter.class);

    private DocumentReferenceResolver<String> documentReferenceResolver;

    private EntityReferenceSerializer<String> entityReferenceSerializer;

    private Provider<XWikiContext> xcontextProvider;

    private WikiDescriptorManager wikiDescriptorManager;

    private AuthorizationManager authorizationManager;

    private XWikiContext xcontext;

    private XWiki xwiki;

    private XWikiRequest request;

    @Before
    public void setUp() throws Exception
    {
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        authorizationManager = mocker.getInstance(AuthorizationManager.class);
        documentReferenceResolver = mocker.getInstance(new DefaultParameterizedType(null, DocumentReferenceResolver.class,
                String.class));
        entityReferenceSerializer = mocker.getInstance(new DefaultParameterizedType(null, EntityReferenceSerializer.class,
                String.class));
        xcontextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        xwiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);

        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("wikiId");
        request = mock(XWikiRequest.class);
        when(xcontext.getRequest()).thenReturn(request);
        DocumentReference colorThemeReference = new DocumentReference("wikiId", "XWiki", "MyColorTheme");
        WikiReference mainWikiReference = new WikiReference("wikiId");
        when(documentReferenceResolver.resolve(eq("myColorTheme"), eq(mainWikiReference))).thenReturn(colorThemeReference);
        when(entityReferenceSerializer.serialize(colorThemeReference)).thenReturn("wikiId:ColorTheme.MyColorTheme");
        when(xwiki.exists(colorThemeReference, xcontext)).thenReturn(true);
        DocumentReference currentUser = new DocumentReference("xwiki", "XWiki", "CurrentUser");
        when(xcontext.getUserReference()).thenReturn(currentUser);
        when(authorizationManager.hasAccess(eq(Right.VIEW), any(DocumentReference.class),
                any(DocumentReference.class))).thenReturn(true);
    }

    @Test
    public void getCurrentColorThemeTestWhenRequestParameter() throws Exception
    {
         when(request.getParameter("colorTheme")).thenReturn("myColorTheme");
         assertEquals("wikiId:ColorTheme.MyColorTheme", mocker.getComponentUnderTest().getCurrentColorTheme("default"));
    }

    @Test
    public void getCurrentColorThemeTestWhenNoRequestParameter() throws Exception
    {
        when(xwiki.getUserPreference(eq("colorTheme"), any(XWikiContext.class))).thenReturn("myColorTheme");
        assertEquals("wikiId:ColorTheme.MyColorTheme", mocker.getComponentUnderTest().getCurrentColorTheme("default"));
    }

    @Test
    public void getCurrentColorThemeFallbackTest() throws Exception
    {
        when(request.getParameter("colorTheme")).thenReturn("myColorTheme");
        when(xwiki.exists(any(DocumentReference.class), eq(xcontext))).thenReturn(false);
        assertEquals("fallback", mocker.getComponentUnderTest().getCurrentColorTheme("fallback"));
        assertEquals("error", mocker.getComponentUnderTest().getCurrentColorTheme("error"));
    }

    @Test
    public void getCurrentColorWithAndWithoutRight() throws Exception
    {
        when(request.getParameter("colorTheme")).thenReturn("myColorTheme");
        when(authorizationManager.hasAccess(eq(Right.VIEW), any(DocumentReference.class), any(DocumentReference.class)))
                .thenReturn(false);
        assertEquals("fallback", mocker.getComponentUnderTest().getCurrentColorTheme(true, "fallback"));
        assertEquals("wikiId:ColorTheme.MyColorTheme",
            mocker.getComponentUnderTest().getCurrentColorTheme(false, "fallback"));
    }
}
