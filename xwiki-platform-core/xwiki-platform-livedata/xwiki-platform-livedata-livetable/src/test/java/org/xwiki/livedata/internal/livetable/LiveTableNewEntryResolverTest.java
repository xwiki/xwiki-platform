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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.internal.livetable.LiveTableNewEntryResolver.NewEntry;
import org.xwiki.livedata.livetable.LiveTableNewRowNamingStrategy;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LiveTableNewEntryResolver}.
 *
 * @version $Id$
 */
@ComponentTest
class LiveTableNewEntryResolverTest
{
    private static final Map<String, Object> PARAMETERS = Map.of("newRowNamingStrategy", "uuid");

    private static final DocumentReference NEW_REFERENCE = new DocumentReference("xwiki", "NewEntries", "uuid");

    private static final DocumentReference TEMPLATE_REFERENCE = new DocumentReference("xwiki", "Code", "Template");

    @InjectMockComponents
    private LiveTableNewEntryResolver resolver;

    @MockComponent
    @Named("uuid")
    private LiveTableNewRowNamingStrategy namingStrategy;

    @MockComponent
    private NewRowTemplateProviderResolver templateProviderResolver;

    @Test
    void resolve() throws Exception
    {
        Map<String, Object> entry = new HashMap<>();
        when(this.namingStrategy.generate(PARAMETERS, entry)).thenReturn(NEW_REFERENCE);

        assertEquals(new NewEntry(NEW_REFERENCE, null), this.resolver.resolve(PARAMETERS, entry));
        // The naming strategy can name the entry after the values it is created with.
        verify(this.namingStrategy).generate(PARAMETERS, entry);
    }

    @Test
    void resolveWithTemplate() throws Exception
    {
        when(this.namingStrategy.generate(any(), any())).thenReturn(NEW_REFERENCE);
        when(this.templateProviderResolver.getTemplate(PARAMETERS)).thenReturn(Optional.of(TEMPLATE_REFERENCE));
        when(this.templateProviderResolver.hasRequiredRights(PARAMETERS, NEW_REFERENCE)).thenReturn(true);

        assertEquals(new NewEntry(NEW_REFERENCE, TEMPLATE_REFERENCE), this.resolver.resolve(PARAMETERS, Map.of()));
    }

    @Test
    void resolveWithoutTheRequiredRightsOfTheTemplate() throws Exception
    {
        when(this.namingStrategy.generate(any(), any())).thenReturn(NEW_REFERENCE);
        when(this.templateProviderResolver.getTemplate(PARAMETERS)).thenReturn(Optional.of(TEMPLATE_REFERENCE));

        LiveDataException exception =
            assertThrows(LiveDataException.class, () -> this.resolver.resolve(PARAMETERS, Map.of()));
        assertEquals("Cannot create [xwiki:NewEntries.uuid]: the current user doesn't have the required rights of "
            + "the template [xwiki:Code.Template].", exception.getMessage());
    }

    @Test
    void resolveWithMandatoryProperties() throws Exception
    {
        Map<String, Object> entry = Map.of("doc.title", "My entry");
        when(this.namingStrategy.getMandatoryProperties(PARAMETERS)).thenReturn(Set.of("doc.title"));
        when(this.namingStrategy.generate(PARAMETERS, entry)).thenReturn(NEW_REFERENCE);

        assertEquals(new NewEntry(NEW_REFERENCE, null), this.resolver.resolve(PARAMETERS, entry));
    }

    @Test
    void resolveWithBlankMandatoryProperty() throws Exception
    {
        when(this.namingStrategy.getMandatoryProperties(PARAMETERS)).thenReturn(Set.of("doc.title", "status"));

        LiveDataException exception = assertThrows(LiveDataException.class,
            () -> this.resolver.resolve(PARAMETERS, Map.of("doc.title", " ")));
        assertEquals("Cannot create an entry without a value for [doc.title, status].", exception.getMessage());
        verify(this.namingStrategy, never()).generate(any(), any());
    }

    @Test
    void resolveWithoutNamingStrategy()
    {
        LiveDataException exception =
            assertThrows(LiveDataException.class, () -> this.resolver.resolve(Map.of(), Map.of()));
        assertEquals("Unsupported row naming strategy [null].", exception.getMessage());
    }
}
