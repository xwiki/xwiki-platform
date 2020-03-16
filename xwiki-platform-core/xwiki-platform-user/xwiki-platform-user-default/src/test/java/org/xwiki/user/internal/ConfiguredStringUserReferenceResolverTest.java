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
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserConfiguration;
import org.xwiki.user.UserReferenceResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ConfiguredStringUserReferenceResolver}.
 *
 * @version $Id$
 */
@ComponentTest
public class ConfiguredStringUserReferenceResolverTest
{
    @InjectMockComponents
    private ConfiguredStringUserReferenceResolver resolver;

    @MockComponent
    @Named("context")
    private ComponentManager componentManager;

    @MockComponent
    private UserConfiguration userConfiguration;

    @Test
    void resolve() throws Exception
    {
        when(this.userConfiguration.getStoreHint()).thenReturn("storehint");
        UserReferenceResolver customUserReferenceResolver = mock(UserReferenceResolver.class);
        when(this.componentManager.getInstance(new DefaultParameterizedType(null, UserReferenceResolver.class,
            String.class), "storehint")).thenReturn(customUserReferenceResolver);

        this.resolver.resolve("userreference");

        verify(customUserReferenceResolver).resolve("userreference");
    }

    @Test
    void resolveWhenNoResolver() throws Exception
    {
        when(this.userConfiguration.getStoreHint()).thenReturn("storehint");
        when(this.componentManager.getInstance(new DefaultParameterizedType(null, UserReferenceResolver.class,
            String.class), "storehint")).thenThrow(new ComponentLookupException("error"));

        Throwable exception = assertThrows(RuntimeException.class,
            () -> this.resolver.resolve("userreference"));
        assertEquals("Failed to find user reference resolver for role "
            + "[org.xwiki.user.UserReferenceResolver<java.lang.String>] and hint [storehint]", exception.getMessage());
        assertEquals("ComponentLookupException: error", ExceptionUtils.getRootCauseMessage(exception));
    }
}
