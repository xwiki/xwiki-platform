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
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.SuperAdminUserReference;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserPropertiesResolver;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultUserPropertiesResolver}.
 *
 * @version $Id$
 */
@ComponentTest
public class DefaultUserPropertiesResolverTest
{
    @InjectMockComponents
    private DefaultUserPropertiesResolver resolver;

    @MockComponent
    @Named("context")
    private ComponentManager contextComponentManager;

    @MockComponent
    @Named("superadminuser")
    private ConfigurationSource superAdminConfigurationSource;

    @MockComponent
    @Named("guestuser")
    private ConfigurationSource guestConfigurationSource;

    @MockComponent
    private UserReferenceResolver<CurrentUserReference> currentUserReferenceUserReferenceResolver;

    private final class TestUserReference implements UserReference
    {
        @Override
        public boolean isGlobal()
        {
            return false;
        }
    }

    @Test
    void resolve() throws Exception
    {
        UserPropertiesResolver customPropertiesResolver = mock(UserPropertiesResolver.class);
        when(customPropertiesResolver.resolve(any(TestUserReference.class))).thenReturn(mock(UserProperties.class));

        when(this.contextComponentManager.getInstance(UserPropertiesResolver.class,
            "user/" + TestUserReference.class.getName())).thenReturn(customPropertiesResolver);

        assertNotNull(this.resolver.resolve(new TestUserReference()));
    }

    @Test
    void resolveForCurrentUser() throws Exception
    {
        TestUserReference testUserReference = new TestUserReference();
        when(this.currentUserReferenceUserReferenceResolver.resolve(null)).thenReturn(testUserReference);

        UserPropertiesResolver customPropertiesResolver = mock(UserPropertiesResolver.class);
        when(customPropertiesResolver.resolve(testUserReference)).thenReturn(mock(UserProperties.class));

        when(this.contextComponentManager.getInstance(UserPropertiesResolver.class,
            "user/" + TestUserReference.class.getName())).thenReturn(customPropertiesResolver);

        assertNotNull(this.resolver.resolve(CurrentUserReference.INSTANCE));
    }

    @Test
    void resolveForCurrentUserWhenNull() throws Exception
    {
        TestUserReference testUserReference = new TestUserReference();
        when(this.currentUserReferenceUserReferenceResolver.resolve(null)).thenReturn(testUserReference);

        UserPropertiesResolver customPropertiesResolver = mock(UserPropertiesResolver.class);
        when(customPropertiesResolver.resolve(testUserReference)).thenReturn(mock(UserProperties.class));

        when(this.contextComponentManager.getInstance(UserPropertiesResolver.class,
            "user/" + TestUserReference.class.getName())).thenReturn(customPropertiesResolver);

        assertNotNull(this.resolver.resolve(null));
    }

    @Test
    void resolveWhenNoUserPropertiesResolver() throws Exception
    {
        when(this.contextComponentManager.getInstance(UserPropertiesResolver.class,
            "user/" + TestUserReference.class.getName())).thenThrow(new ComponentLookupException("error"));

        Throwable exception = assertThrows(RuntimeException.class, () -> {
            this.resolver.resolve(new TestUserReference());
        });
        assertEquals("Failed to find user properties resolver for role "
            + "[org.xwiki.user.UserPropertiesResolver] and hint "
            + "[user/org.xwiki.user.internal.DefaultUserPropertiesResolverTest$TestUserReference]",
            exception.getMessage());
        assertEquals("ComponentLookupException: error", ExceptionUtils.getRootCauseMessage(exception));
    }

    @Test
    void resolveForSuperAdminReference()
    {
        UserProperties userProperties = this.resolver.resolve(SuperAdminUserReference.INSTANCE);
        assertTrue(userProperties instanceof DefaultUserProperties);
        assertSame(this.superAdminConfigurationSource,
            ((DefaultUserProperties) userProperties).getConfigurationSource());
    }

    @Test
    void resolveForGuestReference()
    {
        UserProperties userProperties = this.resolver.resolve(GuestUserReference.INSTANCE);
        assertTrue(userProperties instanceof DefaultUserProperties);
        assertSame(this.guestConfigurationSource,
            ((DefaultUserProperties) userProperties).getConfigurationSource());
    }
}
