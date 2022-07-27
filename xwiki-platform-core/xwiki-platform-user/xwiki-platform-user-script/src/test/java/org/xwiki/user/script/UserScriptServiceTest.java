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
package org.xwiki.user.script;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserPropertiesResolver;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserScriptService}
 *
 * @version $Id$
 */
@ComponentTest
public class UserScriptServiceTest
{
    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @InjectMockComponents
    private UserScriptService scriptService;

    @MockComponent
    private UserPropertiesResolver userPropertiesResolver;

    @MockComponent
    private UserReferenceResolver<String> userReferenceResolver;

    @MockComponent
    private UserReferenceSerializer<String> userReferenceSerializer;

    /**
     * Verify that the correct signature is called when a string parameter is passed to specify the user reference.
     */
    @Test
    void getPropertiesStringCorrectSignature()
    {
        this.scriptService.getProperties("xwiki:XWiki.Admin");
        verify(this.userPropertiesResolver, never()).resolve(eq(CurrentUserReference.INSTANCE),
            ArgumentMatchers.<String>any());
        verify(this.userReferenceResolver).resolve("xwiki:XWiki.Admin");
    }

    @Test
    void serialize()
    {
        UserReference userReference = mock(UserReference.class);
        when(this.userReferenceSerializer.serialize(userReference)).thenReturn("XWiki.Admin");
        assertEquals("XWiki.Admin", this.scriptService.serialize(userReference));
    }
}
