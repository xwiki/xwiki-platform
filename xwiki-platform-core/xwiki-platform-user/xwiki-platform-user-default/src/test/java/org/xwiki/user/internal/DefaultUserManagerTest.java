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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserManager;
import org.xwiki.user.UserReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultUserManager}.
 *
 * @version $Id$
 */
@ComponentTest
public class DefaultUserManagerTest
{
    @InjectMockComponents
    private DefaultUserManager userManager;

    @MockComponent
    @Named("context")
    private ComponentManager contextComponentManager;

    private class TestUserReference implements UserReference
    {
    }

    @Test
    void exists() throws Exception
    {
        UserManager customUserManager = mock(UserManager.class);
        when(customUserManager.exists(any(TestUserReference.class))).thenReturn(true);

        when(this.contextComponentManager.getInstance(UserManager.class, TestUserReference.class.getName()))
            .thenReturn(customUserManager);

        assertTrue(this.userManager.exists(new TestUserReference()));
    }

    @Test
    void existsWhenSuperAdmin()
    {
        assertFalse(this.userManager.exists(UserReference.SUPERADMIN_REFERENCE));
    }

    @Test
    void existsWhenGuest()
    {
        assertFalse(this.userManager.exists(UserReference.GUEST_REFERENCE));
    }

    @Test
    void existsWhenNull() throws Exception
    {
        UserManager currentUserManager = mock(UserManager.class);
        when(this.contextComponentManager.getInstance(UserManager.class, CurrentUserReference.class.getName()))
            .thenReturn(currentUserManager);

        this.userManager.exists(null);

        verify(currentUserManager).exists(UserReference.CURRENT_USER_REFERENCE);
    }

    @Test
    void existsWhenCurrentUser() throws Exception
    {
        UserManager currentUserManager = mock(UserManager.class);
        when(this.contextComponentManager.getInstance(UserManager.class, CurrentUserReference.class.getName()))
            .thenReturn(currentUserManager);

        this.userManager.exists(UserReference.CURRENT_USER_REFERENCE);

        verify(currentUserManager).exists(UserReference.CURRENT_USER_REFERENCE);
    }

    @Test
    void existsWhenNoUserManager() throws Exception
    {
        when(this.contextComponentManager.getInstance(UserManager.class, TestUserReference.class.getName()))
            .thenThrow(new ComponentLookupException("error"));

        Throwable exception = assertThrows(RuntimeException.class, () -> {
            this.userManager.exists(new TestUserReference());
        });
        assertEquals("Failed to find user manager for role [org.xwiki.user.UserManager] and hint "
            + "[org.xwiki.user.internal.DefaultUserManagerTest$TestUserReference]", exception.getMessage());
        assertEquals("ComponentLookupException: error", ExceptionUtils.getRootCauseMessage(exception));
    }
}
