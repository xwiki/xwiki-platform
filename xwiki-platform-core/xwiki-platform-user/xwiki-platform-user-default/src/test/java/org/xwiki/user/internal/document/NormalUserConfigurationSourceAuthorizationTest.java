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
package org.xwiki.user.internal.document;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.configuration.ConfigurationRight;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link NormalUserConfigurationSourceAuthorization}.
 *
 * @version $Id$
 * @since 12.4RC1
 */
@ComponentTest
class NormalUserConfigurationSourceAuthorizationTest
{
    private static final String IS_IN_RENDERING_ENGINE = "isInRenderingEngine";

    @InjectMockComponents
    private NormalUserConfigurationSourceAuthorization authorization;

    @MockComponent
    private EntityReferenceProvider entityReferenceProvider;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> documentResolver;

    @MockComponent
    @Named("document")
    private UserReferenceSerializer<DocumentReference> bridgeSerializer;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWikiDocument currentDocument;

    private DocumentReference lastAuthorDocumentReference;

    private DocumentReference userDocumentReference;

    private UserReference userReference;

    private DocumentReference previousCurrentUserDocumentReference;

    @BeforeEach
    void setup()
    {
        when(this.contextProvider.get()).thenReturn(this.xcontext);

        // Original context:
        // - a page ("wiki.space.currentdoc") in which we're using the user SS API to retrieve a user's
        //   property
        // - the current page is that page: "wiki.space.currentdoc"
        // - the user for which we're retrieving its configuration, "wiki.space.user"
        // - the last author of that page= "wiki.space.lastauthor"
        // - the currently logged in user before the test: originally "wiki.space.previouscurrentuser".
        //   We change it in the code.

        // The user for which we're retrieving its configuration.
        this.userDocumentReference = new DocumentReference("wiki", "space", "user");
        this.userReference = new DocumentUserReference(this.userDocumentReference, true);

        // Current document and last author.
        this.lastAuthorDocumentReference = new DocumentReference("wiki", "space", "lastauthor");
        when(this.xcontext.getAuthorReference()).thenReturn(this.lastAuthorDocumentReference);
        when(this.xcontext.getDoc()).thenReturn(this.currentDocument);
        DocumentReference currentDocumentReference = new DocumentReference("wiki", "space", "currentdoc");
        when(this.currentDocument.getDocumentReference()).thenReturn(currentDocumentReference);
        DocumentUserReference lastAuthorReference =
            new DocumentUserReference(this.lastAuthorDocumentReference, true);
        when(this.documentResolver.resolve(this.lastAuthorDocumentReference)).thenReturn(lastAuthorReference);
        when(this.bridgeSerializer.serialize(lastAuthorReference)).thenReturn(this.lastAuthorDocumentReference);

        // Note: The current user is first "previouscurrentuser" and we later change it to be the "user" user since
        // we're verifying that the last author has view permission on it, and then put it back to be "previoususer".
        // We test that below with a verify().
        this.previousCurrentUserDocumentReference = new DocumentReference("wiki", "space", "previouscurrentuser");
        when(this.documentAccessBridge.getCurrentUserReference()).thenReturn(this.previousCurrentUserDocumentReference,
            this.userDocumentReference, this.previousCurrentUserDocumentReference);
    }

    @Test
    void hasAccessREADWhenLastAuthorHasViewPermissionOnUserDoc()
    {
        when(this.authorizationManager.hasAccess(Right.VIEW, this.lastAuthorDocumentReference,
            this.userDocumentReference)).thenReturn(true);

        assertTrue(this.authorization.hasAccess("key", this.userReference, ConfigurationRight.READ));

        InOrder inOrder = inOrder(this.xcontext);
        inOrder.verify(this.xcontext).setUserReference(this.userDocumentReference);
        inOrder.verify(this.xcontext).setUserReference(this.previousCurrentUserDocumentReference);
    }

    @Test
    void hasAccessREADWhenNoLastAuthorHasNoViewPermissionOnUserDoc()
    {
        when(this.authorizationManager.hasAccess(Right.VIEW, this.lastAuthorDocumentReference,
            this.userDocumentReference)).thenReturn(false);

        assertFalse(this.authorization.hasAccess("key", userReference, ConfigurationRight.READ));

        verify(this.xcontext).setUserReference(this.userDocumentReference);
        verify(this.xcontext).setUserReference(this.previousCurrentUserDocumentReference);
    }

    @Test
    void hasAccessWRITEWhenLastAuthorHasEditPermissionOnUserDoc()
    {
        when(this.authorizationManager.hasAccess(Right.EDIT, this.lastAuthorDocumentReference,
            this.userDocumentReference)).thenReturn(true);

        assertTrue(this.authorization.hasAccess("key", this.userReference, ConfigurationRight.WRITE));

        InOrder inOrder = inOrder(this.xcontext);
        inOrder.verify(this.xcontext).setUserReference(this.userDocumentReference);
        inOrder.verify(this.xcontext).setUserReference(this.previousCurrentUserDocumentReference);
    }

    @Test
    void hasAccessWRITEWhenLastAuthorHasNoEditPermissionOnUserDoc()
    {
        when(this.authorizationManager.hasAccess(Right.EDIT, this.lastAuthorDocumentReference,
            this.userDocumentReference)).thenReturn(false);

        assertFalse(this.authorization.hasAccess("key", this.userReference, ConfigurationRight.WRITE));

        InOrder inOrder = inOrder(this.xcontext);
        inOrder.verify(this.xcontext).setUserReference(this.userDocumentReference);
        inOrder.verify(this.xcontext).setUserReference(this.previousCurrentUserDocumentReference);
    }
}
