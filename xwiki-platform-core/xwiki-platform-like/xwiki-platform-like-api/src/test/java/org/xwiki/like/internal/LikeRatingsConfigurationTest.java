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
package org.xwiki.like.internal;

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

/**
 * Tests for {@link LikeRatingsConfiguration}.
 *
 * @version $Id$
 */
@ComponentTest
class LikeRatingsConfigurationTest
{
    @InjectMockComponents
    private LikeRatingsConfiguration likeRatingsConfiguration;

    @MockComponent
    @Named("xwikiproperties")
    private ConfigurationSource configurationSource;

    @Test
    void isZeroStored()
    {
        assertFalse(this.likeRatingsConfiguration.isZeroStored());
    }

    @Test
    void getScaleUpperBound()
    {
        assertEquals(1, this.likeRatingsConfiguration.getScaleUpperBound());
    }

    @Test
    void hasDedicatedCore()
    {
        assertFalse(this.likeRatingsConfiguration.hasDedicatedCore());
    }

    @Test
    void isAverageStored()
    {
        when(this.configurationSource.getProperty("like.averagerating.isStored", false)).thenReturn(true);
        assertTrue(this.likeRatingsConfiguration.isAverageStored());
    }

    @Test
    void getRatingsStorageHint()
    {
        assertEquals("solr", this.likeRatingsConfiguration.getRatingsStorageHint());
    }

    @Test
    void getAverageRatingStorageHint()
    {
        when(this.configurationSource.getProperty("like.averagerating.hint", "xobject")).thenReturn("something");
        assertEquals("something", this.likeRatingsConfiguration.getAverageRatingStorageHint());
    }

    @Test
    void getExcludedReferencesFromRatings()
    {
        assertEquals(Collections.emptySet(), this.likeRatingsConfiguration.getExcludedReferencesFromRatings());
    }

    @Test
    void isEnabled()
    {
        assertTrue(this.likeRatingsConfiguration.isEnabled());
    }
}
