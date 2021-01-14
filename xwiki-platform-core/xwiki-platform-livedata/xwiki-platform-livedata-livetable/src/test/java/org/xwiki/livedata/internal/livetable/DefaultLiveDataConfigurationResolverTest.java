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

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataQuery.SortEntry;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultLiveDataConfigurationResolver}.
 * 
 * @version $Id$
 */
@ComponentTest
class DefaultLiveDataConfigurationResolverTest
{
    @InjectMockComponents
    private DefaultLiveDataConfigurationResolver resolver;

    @MockComponent
    private ContextualLocalizationManager localization;

    private LiveDataConfiguration config = new LiveDataConfiguration();

    @BeforeEach
    void configure()
    {
        config.initialize();
    }

    @Test
    void setDefaultSort() throws Exception
    {
        // Verify when no sort is set.
        this.config.getQuery().setProperties(Arrays.asList("_one", "two", "three"));
        this.config.getQuery().setSort(null);
        this.resolver.resolve(this.config);

        SortEntry sortEntry = this.config.getQuery().getSort().get(0);
        assertEquals("two", sortEntry.getProperty());
        assertFalse(sortEntry.isDescending());

        // Verify when only the sort order is set.
        sortEntry.setProperty(null);
        sortEntry.setDescending(true);
        this.resolver.resolve(this.config);

        sortEntry = this.config.getQuery().getSort().get(0);
        assertEquals("two", sortEntry.getProperty());
        assertTrue(sortEntry.isDescending());

        // Verify when no properties are specified.
        this.config.getQuery().setProperties(Collections.emptyList());
        sortEntry.setProperty(null);
        this.resolver.resolve(this.config);
        assertTrue(this.config.getQuery().getSort().isEmpty());
    }

    @Test
    void addMissingPropertyDescriptors() throws Exception
    {
        this.config.getQuery().setProperties(Arrays.asList("alice", "bob"));

        LiveDataPropertyDescriptor aliceDescriptor = new LiveDataPropertyDescriptor();
        aliceDescriptor.setId("alice");

        LiveDataPropertyDescriptor carolDescriptor = new LiveDataPropertyDescriptor();
        carolDescriptor.setId("carol");

        this.config.getMeta().getPropertyDescriptors().add(aliceDescriptor);
        this.config.getMeta().getPropertyDescriptors().add(carolDescriptor);

        this.resolver.resolve(this.config);

        assertEquals(3, this.config.getMeta().getPropertyDescriptors().size());
        LiveDataPropertyDescriptor bobDescriptor = getPropertyDescritor("bob");
        assertEquals("String", bobDescriptor.getType());
        assertTrue(bobDescriptor.isVisible());
    }

    @Test
    void setDefaultPropertyNames() throws Exception
    {
        this.config.getQuery().getSource().setParameter("translationPrefix", "test.liveData.");

        LiveDataPropertyDescriptor aliceDescriptor = new LiveDataPropertyDescriptor();
        aliceDescriptor.setId("alice");
        this.config.getMeta().getPropertyDescriptors().add(aliceDescriptor);

        LiveDataPropertyDescriptor carolDescriptor = new LiveDataPropertyDescriptor();
        carolDescriptor.setId("carol");
        carolDescriptor.setName("*Carol*");
        this.config.getMeta().getPropertyDescriptors().add(carolDescriptor);

        when(this.localization.getTranslationPlain("test.liveData.alice")).thenReturn("Bob");

        this.resolver.resolve(this.config);

        assertEquals("Bob", getPropertyDescritor("alice").getName());
        assertEquals("*Carol*", getPropertyDescritor("carol").getName());
    }

    private LiveDataPropertyDescriptor getPropertyDescritor(String property)
    {
        return this.config.getMeta().getPropertyDescriptors().stream()
            .filter(descriptor -> property.equals(descriptor.getId())).findFirst().get();
    }
}
