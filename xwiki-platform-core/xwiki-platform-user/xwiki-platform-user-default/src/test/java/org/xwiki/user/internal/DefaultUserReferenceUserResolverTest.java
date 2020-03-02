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
package org.xwiki.user.internal;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.User;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultUserReferenceUserResolver}.
 *
 * @version $Id$
 */
@ComponentTest
public class DefaultUserReferenceUserResolverTest
{
    @InjectMockComponents
    private DefaultUserReferenceUserResolver resolver;

    @MockComponent
    @Named("context")
    private ComponentManager contextComponentManager;

    private class TestUserReference implements UserReference
    {
    }

    @Test
    void resolve() throws Exception
    {
        UserResolver<TestUserReference> customUserResolver = mock(UserResolver.class);
        when(customUserResolver.resolve(any(TestUserReference.class))).thenReturn(mock(User.class));

        when(this.contextComponentManager.getInstance(UserResolver.TYPE_USER_REFERENCE,
            TestUserReference.class.getName())).thenReturn(customUserResolver);

        assertNotNull(this.resolver.resolve(new TestUserReference()));
    }

    @Test
    void resolveForCurrentUser() throws Exception
    {
        UserResolver<CurrentUserReference> customUserResolver = mock(UserResolver.class);
        when(customUserResolver.resolve(any(CurrentUserReference.class))).thenReturn(mock(User.class));

        when(this.contextComponentManager.getInstance(UserResolver.TYPE_USER_REFERENCE,
            CurrentUserReference.class.getName())).thenReturn(customUserResolver);

        assertNotNull(this.resolver.resolve(UserReference.CURRENT_USER_REFERENCE));
    }

    @Test
    void resolveForCurrentUserWhenNull() throws Exception
    {
        UserResolver<CurrentUserReference> customUserResolver = mock(UserResolver.class);
        when(customUserResolver.resolve(any(CurrentUserReference.class))).thenReturn(mock(User.class));

        when(this.contextComponentManager.getInstance(UserResolver.TYPE_USER_REFERENCE,
            CurrentUserReference.class.getName())).thenReturn(customUserResolver);

        assertNotNull(this.resolver.resolve(null));
    }

    @Test
    void resolveWhenNoUserResolver() throws Exception
    {
        when(this.contextComponentManager.getInstance(UserResolver.TYPE_USER_REFERENCE,
            TestUserReference.class.getName())).thenThrow(new ComponentLookupException("error"));

        Throwable exception = assertThrows(RuntimeException.class, () -> {
            this.resolver.resolve(new TestUserReference());
        });
        assertEquals("Failed to find component implementation for role "
            + "[org.xwiki.user.UserResolver<org.xwiki.user.UserReference>] and hint "
            + "[org.xwiki.user.internal.DefaultUserReferenceUserResolverTest$TestUserReference]",
            exception.getMessage());
    }

    @Test
    void resolveForSuperAdminReference()
    {
        assertSame(User.SUPERADMIN, this.resolver.resolve(UserReference.SUPERADMIN_REFERENCE));
    }

    @Test
    void resolveForGuestReference()
    {
        assertSame(User.GUEST, this.resolver.resolve(UserReference.GUEST_REFERENCE));
    }
}
