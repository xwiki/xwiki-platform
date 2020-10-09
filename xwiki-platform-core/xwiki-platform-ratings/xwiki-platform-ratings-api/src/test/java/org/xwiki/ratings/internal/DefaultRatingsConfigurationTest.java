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

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for {@link DefaultRatingsConfiguration}.
 *
 * @version $Id$
 * @since 12.9RC1
 */
@ComponentTest
public class DefaultRatingsConfigurationTest
{
    @InjectMockComponents
    private DefaultRatingsConfiguration configuration;

    @MockComponent
    @Named("ratings")
    private ConfigurationSource configurationSource;

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
}
