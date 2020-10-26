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
package org.xwiki.livedata.internal;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.internal.multi.ComponentManagerManager;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.livedata.LiveDataQuery.Source;
import org.xwiki.livedata.LiveDataSource;
import org.xwiki.livedata.WithParameters;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultLiveDataSourceManager}.
 * 
 * @version $Id$
 * @since 12.10RC1
 */
@ComponentTest
class DefaultLiveDataSourceManagerTest
{
    @InjectMockComponents
    private DefaultLiveDataSourceManager sourceManager;

    @MockComponent
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    @Mock
    @Named("context")
    private ComponentManager contextComponentManager;

    @MockComponent
    private ComponentManagerManager componentManagerManager;

    @Mock
    @Named("wiki")
    private ComponentManager wikiComponentManager;

    @Mock
    @Named("alice")
    private ComponentDescriptor<LiveDataSource> alice;

    @Mock
    @Named("bob")
    private ComponentDescriptor<LiveDataSource> bob;

    @Mock(extraInterfaces = {LiveDataSource.class})
    private WithParameters source;

    @BeforeEach
    void before()
    {
        when(this.contextComponentManagerProvider.get()).thenReturn(this.contextComponentManager);
        when(this.alice.getRoleHint()).thenReturn("alice");
        when(this.bob.getRoleHint()).thenReturn("bob");
    }

    @Test
    void getAvailableSourcesFromCurrentNamespace()
    {
        when(this.contextComponentManager.<LiveDataSource>getComponentDescriptorList((Type) LiveDataSource.class))
            .thenReturn(Arrays.asList(this.alice, this.bob));

        assertEquals(new HashSet<>(Arrays.asList("alice", "bob")), this.sourceManager.getAvailableSources());
    }

    @Test
    void getAvailableSourcesFromUnknownNamespace()
    {
        assertFalse(this.sourceManager.getAvailableSources("unknown").isPresent());
    }

    @Test
    void getAvailableSourcesFromKnownNamespace()
    {
        when(this.componentManagerManager.getComponentManager("wiki:dev", false)).thenReturn(this.wikiComponentManager);

        assertTrue(this.sourceManager.getAvailableSources("wiki:dev").get().isEmpty());
    }

    @Test
    void getSourceFromCurrentNamespace() throws Exception
    {
        Source sourceInfo = new Source();
        sourceInfo.setId("test");
        sourceInfo.setParameter("key", "value");

        when(this.contextComponentManager.hasComponent(LiveDataSource.class, "test")).thenReturn(true);
        when(this.contextComponentManager.getInstance(LiveDataSource.class, "test")).thenReturn(this.source);

        Map<String, Object> sourceParams = new HashMap<>();
        when(((WithParameters) this.source).getParameters()).thenReturn(sourceParams);

        assertSame(this.source, this.sourceManager.get(sourceInfo).get());
        assertEquals("value", sourceParams.get("key"));
    }

    @Test
    void getSourceFromUnknownNamespace()
    {
        assertFalse(this.sourceManager.get("test", "unknown").isPresent());
    }

    @Test
    void getSourceFromKnownNamespace() throws Exception
    {
        when(this.componentManagerManager.getComponentManager("wiki:dev", false)).thenReturn(this.wikiComponentManager);
        when(this.wikiComponentManager.hasComponent(LiveDataSource.class, "test")).thenReturn(true);
        when(this.wikiComponentManager.getInstance(LiveDataSource.class, "test")).thenReturn(this.source);

        assertSame(this.source, this.sourceManager.get("test", "wiki:dev").get());
    }
}
