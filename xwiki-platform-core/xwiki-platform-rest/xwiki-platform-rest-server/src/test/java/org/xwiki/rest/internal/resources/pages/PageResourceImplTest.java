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
package org.xwiki.rest.internal.resources.pages;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.inject.Provider;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;

import jakarta.inject.Named;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link PageResourceImpl}.
 *
 * @version $Id$
 * @since 18.1.0RC1
 */
@ComponentTest
class PageResourceImplTest
{
    @InjectMockComponents
    private PageResourceImpl pageResource;

    @InjectMockComponents
    private ModelFactory modelFactory;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    @Named("context")
    private ComponentManager contextComponentManager;

    @Mock
    private UriInfo uriInfo;

    @Mock
    private XWiki xwiki;

    private XWikiContext context;

    @BeforeEach
    void setUp(MockitoComponentManager componentManager)
        throws Exception
    {
        Utils.setComponentManager(componentManager);

        // Because XWikiResource injects the context component manager, it exists as a mock, and we thus need to mock
        // its behavior - otherwise it would just be ignored.
        when(this.contextComponentManager.getInstance(any()))
            .thenAnswer(invocation -> componentManager.getInstance(invocation.getArgument(0)));
        when(this.contextComponentManager.getInstance(any(), any()))
            .thenAnswer(
                invocation -> componentManager.getInstance(invocation.getArgument(0), invocation.getArgument(1)));

        when(this.uriInfo.getBaseUri()).thenReturn(new URI("https://test/"));
        FieldUtils.writeField(this.pageResource, "uriInfo", this.uriInfo, true);
        FieldUtils.writeField(this.pageResource, "factory", this.modelFactory, true);

        Provider<XWikiContext> contextProvider = componentManager.getInstance(XWikiContext.TYPE_PROVIDER);
        this.context = contextProvider.get();
        when(this.context.getWiki()).thenReturn(this.xwiki);
    }

    @Test
    void testCheckRights() throws XWikiRestException, XWikiException
    {
        String wikiName = "testWiki";
        String spaceName = "TestSpace";
        String pageName = "TestPage";

        XWikiDocument testPageMock = mock(XWikiDocument.class);
        DocumentReference testPageRef = new DocumentReference(wikiName, spaceName, pageName);
        Document testPageDoc = mock(Document.class);
        when(this.xwiki.getDocument(testPageRef, this.context)).thenReturn(testPageMock);
        when(testPageMock.getDocumentReference()).thenReturn(testPageRef);
        when(testPageMock.newDocument(this.context)).thenReturn(testPageDoc);
        when(testPageDoc.getDocumentReference()).thenReturn(testPageRef);
        when(testPageDoc.getLocale()).thenReturn(Locale.ROOT);
        when(testPageDoc.getRealLocale()).thenReturn(Locale.ROOT);
        when(testPageDoc.getSyntax()).thenReturn(Syntax.XWIKI_2_1);
        when(testPageDoc.getWiki()).thenReturn(wikiName);
        when(testPageDoc.getComments()).thenReturn(new Vector<>());
        when(testPageDoc.getAttachmentList()).thenReturn(new Vector<>());
        when(testPageDoc.getxWikiObjects()).thenReturn(Map.of());
        when(testPageDoc.getRCSVersion()).thenReturn(new Version(1, 0));
        when(testPageDoc.getCreationDate()).thenReturn(new Date(0));
        when(testPageDoc.getContentUpdateDate()).thenReturn(new Date(0));
        when(testPageDoc.getAuthors()).thenReturn(mock(DocumentAuthors.class));
        when(this.contextualAuthorizationManager.hasAccess(Right.VIEW, testPageRef)).thenReturn(true);
        when(this.contextualAuthorizationManager.hasAccess(Right.EDIT, testPageRef)).thenReturn(false);
        when(this.contextualAuthorizationManager.hasAccess(Right.COMMENT, testPageRef)).thenReturn(true);
        when(this.contextualAuthorizationManager.hasAccess(Right.DELETE, testPageRef)).thenReturn(false);

        Page page = this.pageResource.getPage(wikiName, spaceName, pageName, false, false, false, false, List.of());
        assertEquals(List.of(), page.getRights());

        Page pageWithRights = this.pageResource.getPage(wikiName, spaceName, pageName, false, false, false, false,
            List.of("edit", "comment", "delete"));
        List<org.xwiki.rest.model.jaxb.Right> rights = pageWithRights.getRights();
        assertEquals("edit", rights.get(0).getName());
        assertFalse(rights.get(0).isValue());
        assertEquals("comment", rights.get(1).getName());
        assertTrue(rights.get(1).isValue());
        assertEquals("delete", rights.get(2).getName());
        assertFalse(rights.get(2).isValue());

        assertThrows(BadRequestException.class, () -> this.pageResource.getPage(wikiName, spaceName, pageName, false,
            false, false, false, List.of("unknownRight")));
    }
}
