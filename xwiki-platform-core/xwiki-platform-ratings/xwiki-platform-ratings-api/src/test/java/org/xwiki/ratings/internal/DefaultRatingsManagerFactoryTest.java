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

import java.util.Arrays;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.ratings.RatingsConfiguration;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultRatingsManagerFactory}.
 *
 * @version $Id$
 * @since 12.9RC1
 */
@ComponentTest
class DefaultRatingsManagerFactoryTest
{
    @InjectMockComponents
    private DefaultRatingsManagerFactory factory;

    @MockComponent
    @Named("context")
    private ComponentManager contextComponentManager;

    @MockComponent
    private ComponentManager currentComponentManager;

    @MockComponent
    private RatingsManager ratingsManager;

    @MockComponent
    private RatingsConfiguration ratingsConfiguration;

    @Test
    void getExistingInstance() throws Exception
    {
        String hint = "existingInstance";
        when(this.contextComponentManager.hasComponent(RatingsManager.class, hint)).thenReturn(true);
        when(this.contextComponentManager.getInstance(RatingsManager.class, hint)).thenReturn(ratingsManager);

        assertSame(ratingsManager, this.factory.getRatingsManager(hint));
        verify(this.currentComponentManager, never()).registerComponent(any(), any());
    }

    @Test
    void getNewInstanceCustomConfiguration() throws Exception
    {
        String hint = "newInstance";
        when(this.contextComponentManager.hasComponent(RatingsManager.class, hint)).thenReturn(false);
        when(this.contextComponentManager.hasComponent(RatingsConfiguration.class, hint)).thenReturn(true);
        when(this.contextComponentManager.getInstance(RatingsConfiguration.class, hint))
            .thenReturn(this.ratingsConfiguration);
        when(this.ratingsConfiguration.getRatingsStorageHint()).thenReturn("someStorage");
        when(this.contextComponentManager.getInstance(RatingsManager.class, "someStorage"))
            .thenReturn(this.ratingsManager);
        ComponentDescriptor componentDescriptor = mock(ComponentDescriptor.class);
        when(componentDescriptor.getImplementation()).thenReturn(SolrRatingsManager.class);
        when(this.contextComponentManager.getComponentDescriptor(RatingsManager.class, "someStorage"))
            .thenReturn(componentDescriptor);
        DefaultComponentDescriptor<RatingsManager> expectedComponentDescriptor = new DefaultComponentDescriptor<>();
        expectedComponentDescriptor.setImplementation(SolrRatingsManager.class);
        expectedComponentDescriptor.setRoleHint(hint);
        expectedComponentDescriptor.setInstantiationStrategy(ComponentInstantiationStrategy.SINGLETON);
        expectedComponentDescriptor.setRoleHintPriority(0);
        expectedComponentDescriptor.setRoleTypePriority(0);

        assertSame(this.ratingsManager, this.factory.getRatingsManager(hint));
        verify(this.currentComponentManager).registerComponent(expectedComponentDescriptor, this.ratingsManager);
    }

    @Test
    void getNewInstanceDefaultConfiguration() throws Exception
    {
        String hint = "newInstance";
        when(this.contextComponentManager.hasComponent(RatingsManager.class, hint)).thenReturn(false);
        when(this.contextComponentManager.hasComponent(RatingsConfiguration.class, hint)).thenReturn(false);
        when(this.contextComponentManager.getInstance(RatingsConfiguration.class))
            .thenReturn(this.ratingsConfiguration);
        when(this.ratingsConfiguration.getRatingsStorageHint()).thenReturn("someStorage");
        when(this.contextComponentManager.getInstance(RatingsManager.class, "someStorage"))
            .thenReturn(this.ratingsManager);
        ComponentDescriptor componentDescriptor = mock(ComponentDescriptor.class);
        when(componentDescriptor.getImplementation()).thenReturn(SolrRatingsManager.class);
        when(this.contextComponentManager.getComponentDescriptor(RatingsManager.class, "someStorage"))
            .thenReturn(componentDescriptor);
        DefaultComponentDescriptor<RatingsManager> expectedComponentDescriptor = new DefaultComponentDescriptor<>();
        expectedComponentDescriptor.setImplementation(SolrRatingsManager.class);
        expectedComponentDescriptor.setRoleHint(hint);
        expectedComponentDescriptor.setInstantiationStrategy(ComponentInstantiationStrategy.SINGLETON);
        expectedComponentDescriptor.setRoleHintPriority(0);
        expectedComponentDescriptor.setRoleTypePriority(0);

        assertSame(this.ratingsManager, this.factory.getRatingsManager(hint));
        verify(this.currentComponentManager).registerComponent(expectedComponentDescriptor, this.ratingsManager);
    }

    @Test
    void getInstantiatedManagers() throws Exception
    {
        RatingsManager manager1 = mock(RatingsManager.class);
        RatingsManager manager2 = mock(RatingsManager.class);
        RatingsManager manager3 = mock(RatingsManager.class);
        when(this.contextComponentManager.getInstanceList(RatingsManager.class)).thenReturn(Arrays.asList(
            manager1, manager2, manager3
        ));
        when(manager1.getIdentifier()).thenReturn("foo");
        when(manager3.getIdentifier()).thenReturn("bar");
        assertEquals(Arrays.asList(manager1, manager3), this.factory.getInstantiatedManagers());
    }
}
