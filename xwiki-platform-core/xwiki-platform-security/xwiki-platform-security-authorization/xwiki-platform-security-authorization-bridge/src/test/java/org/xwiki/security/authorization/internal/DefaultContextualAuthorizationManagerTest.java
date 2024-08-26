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
package org.xwiki.security.authorization.internal;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.DocumentAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link DefaultContextualAuthorizationManager}.
 * 
 * @version $Id$
 */
@ReferenceComponentList
@OldcoreTest
class DefaultContextualAuthorizationManagerTest
{
    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private RenderingContext renderingContext;

    @MockComponent
    private DocumentAuthorizationManager documentAuthorizationManager;

    @InjectMockComponents
    private DefaultContextualAuthorizationManager contextualAuthorizationManager;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    private WikiReference currentWikiReference;

    @BeforeEach
    public void before() throws Exception
    {
        this.currentWikiReference = new WikiReference("wiki");
        this.oldcore.getXWikiContext().setWikiId(this.currentWikiReference.getName());
        when(this.documentAuthorizationManager.hasRequiredRight(any(), any(), any())).thenReturn(true);
    }

    // Tests

    @Test
    void checkAccess() throws Exception
    {
        LocalDocumentReference localReference = new LocalDocumentReference("space", "page");

        this.contextualAuthorizationManager.checkAccess(Right.VIEW, localReference);

        verify(this.authorizationManager).checkAccess(same(Right.VIEW), isNull(),
            eq(new DocumentReference(localReference, this.currentWikiReference)));
    }

    @Test
    void hasAccess()
    {
        LocalDocumentReference localReference = new LocalDocumentReference("space", "page");

        this.contextualAuthorizationManager.hasAccess(Right.VIEW, localReference);

        verify(this.authorizationManager).hasAccess(same(Right.VIEW), isNull(),
            eq(new DocumentReference(localReference, this.currentWikiReference)));
    }

    @ParameterizedTest
    @MethodSource("contentRightsSource")
    void contentAuthorRightPreAccess(Right right)
    {
        when(this.authorizationManager.hasAccess(eq(right), any(), any())).thenReturn(true);

        assertTrue(this.contextualAuthorizationManager.hasAccess(right));

        // Check restricted rendering context (once).
        when(this.renderingContext.isRestricted()).thenReturn(true).thenReturn(false);
        assertFalse(this.contextualAuthorizationManager.hasAccess(right));

        XWikiDocument contextDocument = mock(XWikiDocument.class);
        this.oldcore.getXWikiContext().setDoc(contextDocument);
        assertTrue(this.contextualAuthorizationManager.hasAccess(right));
        verify(contextDocument).isRestricted();
        // Check restricted document denies script right.
        when(contextDocument.isRestricted()).thenReturn(true).thenReturn(false);
        assertFalse(this.contextualAuthorizationManager.hasAccess(right));

        // Check dropping permissions keeps script but not programming right
        this.oldcore.getXWikiContext().dropPermissions();
        if (right == Right.PROGRAM) {
            assertFalse(this.contextualAuthorizationManager.hasAccess(right));
        } else {
            assertTrue(this.contextualAuthorizationManager.hasAccess(right));
        }
    }

    @ParameterizedTest
    @MethodSource("contentRightsSource")
    void requiredRightsPreAccess(Right right) throws AuthorizationException
    {
        when(this.authorizationManager.hasAccess(eq(right), any(), any())).thenReturn(true);

        XWikiDocument contextDocument = mock();
        DocumentReference documentReference = mock();
        when(contextDocument.getDocumentReference()).thenReturn(documentReference);
        this.oldcore.getXWikiContext().setDoc(contextDocument);

        when(this.documentAuthorizationManager.hasRequiredRight(right, EntityType.DOCUMENT, documentReference))
            .thenReturn(false);

        assertFalse(this.contextualAuthorizationManager.hasAccess(right));
    }

    static Stream<Right> contentRightsSource()
    {
        return Stream.of(Right.SCRIPT, Right.PROGRAM);
    }
}
