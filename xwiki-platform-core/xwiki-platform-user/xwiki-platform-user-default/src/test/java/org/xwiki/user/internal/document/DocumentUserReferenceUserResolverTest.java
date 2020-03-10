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

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.User;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DocumentUserReferenceUserResolver}.
 *
 * @version $Id$
 */
@ComponentTest
public class DocumentUserReferenceUserResolverTest
{
    @InjectMockComponents
    private DocumentUserReferenceUserResolver resolver;

    @MockComponent
    @Named("org.xwiki.user.CurrentUserReference")
    private UserResolver<UserReference> currentUserResolver;

    @MockComponent
    protected EntityReferenceProvider entityReferenceProvider;

    @Test
    void resolve()
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        User user = this.resolver.resolve(new DocumentUserReference(documentReference,this.entityReferenceProvider));
        assertNotNull(user);
        assertEquals("wiki:space.page", ((DocumentUser) user).getUserReference().getReference().toString());
    }

    @Test
    void resolveCurrentUser()
    {
        User currentUser = mock(User.class);
        when(this.currentUserResolver.resolve(any())).thenReturn(currentUser);

        User user = this.resolver.resolve(UserReference.CURRENT_USER_REFERENCE);
        assertNotNull(user);
        assertSame(currentUser, user);
    }

    @Test
    void resolveWhenNullReference()
    {
        User currentUser = mock(User.class);
        when(this.currentUserResolver.resolve(any())).thenReturn(currentUser);

        User user = this.resolver.resolve(null);
        assertNotNull(user);
        assertSame(currentUser, user);
    }

    @Test
    void resolveGuest()
    {
        User user = this.resolver.resolve(UserReference.GUEST_REFERENCE);
        assertSame(UserReference.GUEST_REFERENCE, user.getUserReference());
    }

    @Test
    void resolveGuestWhenSpecifiedAsString()
    {
        User user = this.resolver.resolve(new DocumentUserReference(
            new DocumentReference("wiki", "space", "XWikiGuest"), this.entityReferenceProvider));
        assertSame(UserReference.GUEST_REFERENCE, user.getUserReference());
    }

    @Test
    void resolveSuperAdmin()
    {
        User user = this.resolver.resolve(UserReference.SUPERADMIN_REFERENCE);
        assertSame(UserReference.SUPERADMIN_REFERENCE, user.getUserReference());
    }

    @Test
    void resolveSuperAdminWhenSpecifiedAsString()
    {
        User user = this.resolver.resolve(new DocumentUserReference(
            new DocumentReference("wiki", "space", "superadmin"), this.entityReferenceProvider));
        assertSame(UserReference.SUPERADMIN_REFERENCE, user.getUserReference());
    }
}
