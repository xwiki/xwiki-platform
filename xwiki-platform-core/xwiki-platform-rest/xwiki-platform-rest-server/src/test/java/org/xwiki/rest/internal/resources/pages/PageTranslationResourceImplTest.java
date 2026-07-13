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

import java.util.Locale;

import javax.inject.Provider;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import jakarta.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.refactoring.RefactoringConfiguration;
import org.xwiki.rest.XWikiRestException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link PageTranslationResourceImpl}.
 *
 * @version $Id$
 */
@ComponentTest
class PageTranslationResourceImplTest
{
    private static final String WIKI = "testWiki";

    private static final String SPACE = "TestSpace";

    private static final String PAGE = "TestPage";

    private static final String LANGUAGE = "fr";

    @InjectMockComponents
    private PageTranslationResourceImpl pageTranslationResource;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    private RefactoringConfiguration refactoringConfiguration;

    @MockComponent
    @Named("context")
    private ComponentManager contextComponentManager;

    @Mock
    private XWiki xwiki;

    private XWikiContext context;

    private XWikiDocument translationXWikiDocument;

    private Document translationDocument;

    @BeforeEach
    void setUp(MockitoComponentManager componentManager) throws Exception
    {
        Utils.setComponentManager(componentManager);

        // Because XWikiResource injects the context component manager, it exists as a mock, and we thus need to mock
        // its behavior - otherwise it would just be ignored.
        when(this.contextComponentManager.getInstance(any()))
            .thenAnswer(invocation -> componentManager.getInstance(invocation.getArgument(0)));
        when(this.contextComponentManager.getInstance(any(), any()))
            .thenAnswer(
                invocation -> componentManager.getInstance(invocation.getArgument(0), invocation.getArgument(1)));

        Provider<XWikiContext> contextProvider = componentManager.getInstance(XWikiContext.TYPE_PROVIDER);
        this.context = contextProvider.get();
        when(this.context.getWiki()).thenReturn(this.xwiki);
    }

    @Test
    void deletePageTranslationRequiresDeleteRight() throws XWikiException
    {
        DocumentReference reference = initTranslation();
        when(this.contextualAuthorizationManager.hasAccess(Right.DELETE, reference)).thenReturn(false);

        WebApplicationException exception = assertThrows(WebApplicationException.class,
            () -> this.pageTranslationResource.deletePageTranslation(WIKI, SPACE, PAGE, LANGUAGE, false));
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), exception.getResponse().getStatus());

        verify(this.translationDocument, never()).delete();
        verify(this.xwiki, never()).deleteDocument(any(), anyBoolean(), any());
    }

    @Test
    void deletePageTranslationSendsToRecycleBinByDefault() throws XWikiRestException, XWikiException
    {
        DocumentReference reference = initTranslation();
        when(this.contextualAuthorizationManager.hasAccess(Right.DELETE, reference)).thenReturn(true);

        this.pageTranslationResource.deletePageTranslation(WIKI, SPACE, PAGE, LANGUAGE, false);

        verify(this.translationDocument).delete();
        verify(this.xwiki, never()).deleteDocument(any(), anyBoolean(), any());
    }

    @Test
    void deletePageTranslationSkipsRecycleBinWhenActivated() throws XWikiRestException, XWikiException
    {
        DocumentReference reference = initTranslation();
        when(this.contextualAuthorizationManager.hasAccess(Right.DELETE, reference)).thenReturn(true);
        when(this.refactoringConfiguration.isRecycleBinSkippingActivated()).thenReturn(true);

        this.pageTranslationResource.deletePageTranslation(WIKI, SPACE, PAGE, LANGUAGE, true);

        verify(this.xwiki).deleteDocument(this.translationXWikiDocument, false, this.context);
        verify(this.translationDocument, never()).delete();
    }

    @Test
    void deletePageTranslationSkipRecycleBinFallsBackWhenNotActivated() throws XWikiRestException, XWikiException
    {
        DocumentReference reference = initTranslation();
        when(this.contextualAuthorizationManager.hasAccess(Right.DELETE, reference)).thenReturn(true);
        when(this.refactoringConfiguration.isRecycleBinSkippingActivated()).thenReturn(false);

        this.pageTranslationResource.deletePageTranslation(WIKI, SPACE, PAGE, LANGUAGE, true);

        verify(this.translationDocument).delete();
        verify(this.xwiki, never()).deleteDocument(any(), anyBoolean(), any());
    }

    private DocumentReference initTranslation() throws XWikiException
    {
        DocumentReference reference = new DocumentReference(WIKI, SPACE, PAGE, Locale.FRENCH);
        this.translationXWikiDocument = mock(XWikiDocument.class);
        this.translationDocument = mock(Document.class);

        when(this.xwiki.getDocument(reference, this.context)).thenReturn(this.translationXWikiDocument);
        when(this.translationXWikiDocument.newDocument(this.context)).thenReturn(this.translationDocument);
        when(this.translationDocument.getDocumentReference()).thenReturn(reference);

        // The translation must be resolved (VIEW right + existing document) before the DELETE right is checked.
        when(this.contextualAuthorizationManager.hasAccess(Right.VIEW, reference)).thenReturn(true);

        return reference;
    }
}
