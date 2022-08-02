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

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.SuperAdminUserReference;
import org.xwiki.user.UserManager;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CurrentUserManager}.
 *
 * @version $Id$
 */
@ComponentTest
class CurrentUserManagerTest
{
    @InjectMockComponents
    private CurrentUserManager manager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @MockComponent
    @Named("org.xwiki.user.internal.document.DocumentUserReference")
    private UserManager documentUserManager;

    @Test
    void existsWhenNoCurrentUser() throws Exception
    {
        // No current user in the context
        XWikiContext xcontext = mock(XWikiContext.class);
        when(xcontext.getUserReference()).thenReturn(null);
        when(this.contextProvider.get()).thenReturn(xcontext);

        when(this.userReferenceResolver.resolve(null)).thenReturn(CurrentUserReference.INSTANCE);

        assertFalse(this.manager.exists(CurrentUserReference.INSTANCE));
    }

    @Test
    void existsWhenCurrentUser() throws Exception
    {
        XWikiContext xcontext = mock(XWikiContext.class);
        DocumentReference documentReference = new DocumentReference("wiki", "XWiki", "User");
        when(xcontext.getUserReference()).thenReturn(documentReference);
        when(this.contextProvider.get()).thenReturn(xcontext);

        DocumentUserReference documentUserReference = new DocumentUserReference(documentReference, true);
        when(this.userReferenceResolver.resolve(documentReference)).thenReturn(documentUserReference);

        this.manager.exists(CurrentUserReference.INSTANCE);

        verify(this.documentUserManager).exists(documentUserReference);
    }

    @Test
    void existsWhenCurrentUserIsSuperAdmin() throws Exception
    {
        XWikiContext xcontext = mock(XWikiContext.class);
        DocumentReference documentReference = new DocumentReference("wiki", "XWiki", "superadmin");
        when(xcontext.getUserReference()).thenReturn(documentReference);
        when(this.contextProvider.get()).thenReturn(xcontext);

        when(this.userReferenceResolver.resolve(documentReference)).thenReturn(SuperAdminUserReference.INSTANCE);

        assertFalse(this.manager.exists(CurrentUserReference.INSTANCE));
    }

    @Test
    void existsWhenCurrentUserIsGuest() throws Exception
    {
        XWikiContext xcontext = mock(XWikiContext.class);
        DocumentReference documentReference = new DocumentReference("wiki", "XWiki", "xwikiguest");
        when(xcontext.getUserReference()).thenReturn(documentReference);
        when(this.contextProvider.get()).thenReturn(xcontext);

        when(this.userReferenceResolver.resolve(documentReference)).thenReturn(GuestUserReference.INSTANCE);

        assertFalse(this.manager.exists(CurrentUserReference.INSTANCE));
    }
}
