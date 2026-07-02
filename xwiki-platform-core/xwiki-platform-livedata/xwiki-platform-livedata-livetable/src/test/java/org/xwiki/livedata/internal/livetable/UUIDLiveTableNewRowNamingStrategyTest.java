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
package org.xwiki.livedata.internal.livetable;

import java.util.Map;
import java.util.UUID;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UUIDLiveTableNewRowNamingStrategy}.
 *
 * @version $Id$
 */
@ComponentTest
class UUIDLiveTableNewRowNamingStrategyTest
{
    private static final String UUID_VALUE_STRING = "d3856e8e-1d8a-4f9e-9c3a-2b1f0a6d7e54";
    private static final UUID UUID_VALUE = UUID.fromString(UUID_VALUE_STRING);

    @InjectMockComponents
    private UUIDLiveTableNewRowNamingStrategy strategy;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @MockComponent
    @Named("current")
    private SpaceReferenceResolver<String> currentSpaceReferenceResolver;

    @MockComponent
    private ModelBridge modelBridge;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @BeforeEach
    void before()
    {
    }

    @Test
    void generate() throws Exception
    {
        Map<String, Object> parameters = Map.of("newRowLocation", "NewRows");
        DocumentReference candidate = new DocumentReference("wiki", "NewRows", UUID_VALUE_STRING);
        when(this.currentDocumentReferenceResolver.resolve("NewRows." + UUID_VALUE_STRING)).thenReturn(candidate);
        when(this.modelBridge.exists(candidate)).thenReturn(false);

        try (MockedStatic<UUID> uuidStatic = mockStatic(UUID.class)) {
            uuidStatic.when(UUID::randomUUID).thenReturn(UUID_VALUE);
            assertEquals(candidate, this.strategy.generate(parameters));
        }
    }

    @Test
    void generateWithMissingLocation()
    {
        LiveDataException exception =
            assertThrows(LiveDataException.class, () -> this.strategy.generate(Map.of()));
        assertEquals("Missing location for row creation.", exception.getMessage());
    }

    @Test
    void generateWhenPageAlreadyExists() throws XWikiException
    {
        Map<String, Object> parameters = Map.of("newRowLocation", "NewRows");
        DocumentReference candidate = new DocumentReference("wiki", "NewRows", UUID_VALUE_STRING);
        when(this.currentDocumentReferenceResolver.resolve("NewRows." + UUID_VALUE_STRING)).thenReturn(candidate);
        when(this.modelBridge.exists(candidate)).thenReturn(true);

        try (MockedStatic<UUID> uuidStatic = mockStatic(UUID.class)) {
            uuidStatic.when(UUID::randomUUID).thenReturn(UUID_VALUE);
            LiveDataException exception =
                assertThrows(LiveDataException.class, () -> this.strategy.generate(parameters));
            assertEquals(String.format("The page [%s] already exists.", candidate), exception.getMessage());
        }
    }

    @Test
    void isCreationAllowed()
    {
        Map<String, Object> parameters = Map.of("newRowLocation", "NewRows");
        SpaceReference space = new SpaceReference("wiki", "NewRows");
        when(this.currentSpaceReferenceResolver.resolve("NewRows")).thenReturn(space);
        when(this.authorization.hasAccess(Right.EDIT, space)).thenReturn(true);

        assertTrue(this.strategy.isCreationAllowed(parameters));
    }

    @Test
    void isCreationAllowedWithoutEditRight()
    {
        Map<String, Object> parameters = Map.of("newRowLocation", "NewRows");
        SpaceReference space = new SpaceReference("wiki", "NewRows");
        when(this.currentSpaceReferenceResolver.resolve("NewRows")).thenReturn(space);
        when(this.authorization.hasAccess(Right.EDIT, space)).thenReturn(false);

        assertFalse(this.strategy.isCreationAllowed(parameters));
    }

    @Test
    void isCreationAllowedWithMissingLocation()
    {
        assertFalse(this.strategy.isCreationAllowed(Map.of()));
    }
}
