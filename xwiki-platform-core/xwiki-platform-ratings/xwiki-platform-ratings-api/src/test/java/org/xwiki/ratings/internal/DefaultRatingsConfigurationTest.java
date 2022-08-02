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
package org.xwiki.ratings.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultRatingsConfiguration}.
 *
 * @version $Id$
 * @since 12.9RC1
 */
@ComponentTest
class DefaultRatingsConfigurationTest
{
    @InjectMockComponents
    private DefaultRatingsConfiguration configuration;

    @MockComponent
    @Named("ratings")
    private ConfigurationSource configurationSource;

    @MockComponent
    private EntityReferenceResolver<String> entityReferenceResolver;

    @Test
    void defaultValues()
    {
        when(this.configurationSource.getProperty(any(String.class), any(Object.class)))
            .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(1));
        assertEquals("solr", configuration.getRatingsStorageHint());
        assertEquals("xobject", configuration.getAverageRatingStorageHint());
        assertTrue(configuration.isZeroStored());
        assertFalse(configuration.hasDedicatedCore());
        assertEquals(5, configuration.getScaleUpperBound());
        assertTrue(configuration.isAverageStored());
        assertTrue(configuration.isEnabled());
        assertEquals(Collections.emptySet(), configuration.getExcludedReferencesFromRatings());
    }

    @Test
    void getExcludedReferencesFromRatings()
    {
        HashSet<String> references = new HashSet<>();
        references.add("foo");
        references.add("bar");
        when(this.configurationSource.getProperty("excludedReferences", new HashSet<String>())).thenReturn(references);

        EntityReference fooReference = new DocumentReference("xwiki", "Foo", "WebHome");
        EntityReference barReference = new DocumentReference("xwiki", "Bar", "WebHome");
        Set<EntityReference> result = new HashSet<>();
        result.add(fooReference);
        result.add(barReference);

        when(this.entityReferenceResolver.resolve("foo", EntityType.PAGE)).thenReturn(fooReference);
        when(this.entityReferenceResolver.resolve("bar", EntityType.PAGE)).thenReturn(barReference);

        assertEquals(result, this.configuration.getExcludedReferencesFromRatings());
    }
}
