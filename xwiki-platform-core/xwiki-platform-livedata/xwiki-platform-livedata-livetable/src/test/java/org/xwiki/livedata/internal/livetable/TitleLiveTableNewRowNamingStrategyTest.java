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
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.model.validation.EntityNameValidation;
import org.xwiki.model.validation.EntityNameValidationConfiguration;
import org.xwiki.model.validation.EntityNameValidationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TitleLiveTableNewRowNamingStrategy}.
 *
 * @version $Id$
 */
@ComponentTest
class TitleLiveTableNewRowNamingStrategyTest
{
    private static final String LOCATION = "NewRows";

    private static final Map<String, Object> PARAMETERS = Map.of("newRowLocation", LOCATION);

    private static final String TITLE_PROPERTY = "doc.title";

    private static final String TITLE = "My first entry";

    private final SpaceReference space = new SpaceReference("wiki", LOCATION);

    @InjectMockComponents
    private TitleLiveTableNewRowNamingStrategy strategy;

    @MockComponent
    @Named("current")
    private SpaceReferenceResolver<String> currentSpaceReferenceResolver;

    @MockComponent
    private ModelBridge modelBridge;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    private EntityNameValidationManager entityNameValidationManager;

    @MockComponent
    private EntityNameValidationConfiguration entityNameValidationConfiguration;

    @Test
    void generate() throws Exception
    {
        when(this.currentSpaceReferenceResolver.resolve(LOCATION)).thenReturn(this.space);

        assertEquals(new DocumentReference(TITLE, this.space),
            this.strategy.generate(PARAMETERS, Map.of(TITLE_PROPERTY, TITLE)));
    }

    @Test
    void generateWithSurroundingWhitespace() throws Exception
    {
        when(this.currentSpaceReferenceResolver.resolve(LOCATION)).thenReturn(this.space);

        assertEquals(new DocumentReference(TITLE, this.space),
            this.strategy.generate(PARAMETERS, Map.of(TITLE_PROPERTY, "  " + TITLE + "\n")));
    }

    @Test
    void generateAppliesTheNameStrategy() throws Exception
    {
        when(this.currentSpaceReferenceResolver.resolve(LOCATION)).thenReturn(this.space);
        EntityNameValidation nameStrategy = mock(EntityNameValidation.class);
        when(this.entityNameValidationManager.getEntityReferenceNameStrategy()).thenReturn(nameStrategy);
        when(this.entityNameValidationConfiguration.useTransformation()).thenReturn(true);
        when(nameStrategy.transform(TITLE)).thenReturn("My-first-entry");

        assertEquals(new DocumentReference("My-first-entry", this.space),
            this.strategy.generate(PARAMETERS, Map.of(TITLE_PROPERTY, TITLE)));
    }

    @Test
    void generateWhenTheTransformationIsDisabled() throws Exception
    {
        when(this.currentSpaceReferenceResolver.resolve(LOCATION)).thenReturn(this.space);
        EntityNameValidation nameStrategy = mock(EntityNameValidation.class);
        when(this.entityNameValidationManager.getEntityReferenceNameStrategy()).thenReturn(nameStrategy);
        when(nameStrategy.transform(TITLE)).thenReturn("My-first-entry");

        assertEquals(new DocumentReference(TITLE, this.space),
            this.strategy.generate(PARAMETERS, Map.of(TITLE_PROPERTY, TITLE)));
    }

    @Test
    void generateWhenTheNameStrategyStripsTheTitle()
    {
        EntityNameValidation nameStrategy = mock(EntityNameValidation.class);
        when(this.entityNameValidationManager.getEntityReferenceNameStrategy()).thenReturn(nameStrategy);
        when(this.entityNameValidationConfiguration.useTransformation()).thenReturn(true);
        when(nameStrategy.transform("///")).thenReturn("");
        when(this.currentSpaceReferenceResolver.resolve(LOCATION)).thenReturn(this.space);

        LiveDataException exception = assertThrows(LiveDataException.class,
            () -> this.strategy.generate(PARAMETERS, Map.of(TITLE_PROPERTY, "///")));
        assertEquals("No page name can be built from the title [///].", exception.getMessage());
    }

    @Test
    void generateSuffixesTheNameOfAnExistingPage() throws Exception
    {
        when(this.currentSpaceReferenceResolver.resolve(LOCATION)).thenReturn(this.space);
        when(this.modelBridge.exists(new DocumentReference(TITLE, this.space))).thenReturn(true);
        when(this.modelBridge.exists(new DocumentReference(TITLE + "-1", this.space))).thenReturn(true);

        assertEquals(new DocumentReference(TITLE + "-2", this.space),
            this.strategy.generate(PARAMETERS, Map.of(TITLE_PROPERTY, TITLE)));
    }

    @Test
    void generateWithMissingTitle()
    {
        when(this.currentSpaceReferenceResolver.resolve(LOCATION)).thenReturn(this.space);

        LiveDataException exception =
            assertThrows(LiveDataException.class, () -> this.strategy.generate(PARAMETERS, Map.of()));
        assertEquals("Missing [doc.title] value, which the new entry is named after.", exception.getMessage());
    }

    @Test
    void generateWithBlankTitle()
    {
        when(this.currentSpaceReferenceResolver.resolve(LOCATION)).thenReturn(this.space);

        LiveDataException exception = assertThrows(LiveDataException.class,
            () -> this.strategy.generate(PARAMETERS, Map.of(TITLE_PROPERTY, " ")));
        assertEquals("Missing [doc.title] value, which the new entry is named after.", exception.getMessage());
    }

    @Test
    void generateWithMissingLocation()
    {
        LiveDataException exception = assertThrows(LiveDataException.class,
            () -> this.strategy.generate(Map.of(), Map.of(TITLE_PROPERTY, TITLE)));
        assertEquals("Missing location for row creation.", exception.getMessage());
    }

    @Test
    void getMandatoryProperties()
    {
        assertEquals(Set.of(TITLE_PROPERTY), this.strategy.getMandatoryProperties(PARAMETERS));
    }

    @Test
    void isCreationAllowed()
    {
        when(this.currentSpaceReferenceResolver.resolve(LOCATION)).thenReturn(this.space);
        when(this.authorization.hasAccess(Right.EDIT, this.space)).thenReturn(true);

        assertTrue(this.strategy.isCreationAllowed(PARAMETERS));
    }

    @Test
    void isCreationAllowedWithoutEditRight()
    {
        when(this.currentSpaceReferenceResolver.resolve(LOCATION)).thenReturn(this.space);

        assertFalse(this.strategy.isCreationAllowed(PARAMETERS));
    }

    @Test
    void getNewEntryLocation()
    {
        when(this.currentSpaceReferenceResolver.resolve(LOCATION)).thenReturn(this.space);

        assertEquals(Optional.of(this.space), this.strategy.getNewEntryLocation(PARAMETERS));
    }
}
