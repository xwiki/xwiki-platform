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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.livetable.LiveTableNewRowNamingStrategy;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultLiveTableNewRowNamingStrategyManager}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultLiveTableNewRowNamingStrategyManagerTest
{
    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private DefaultLiveTableNewRowNamingStrategyManager manager;

    @MockComponent
    @Named("context")
    private ComponentManager contextComponentManager;

    @Test
    void getGivesTheInstalledStrategy() throws Exception
    {
        LiveTableNewRowNamingStrategy title = mock();
        when(this.contextComponentManager.hasComponent(LiveTableNewRowNamingStrategy.class, "title")).thenReturn(true);
        when(this.contextComponentManager.<LiveTableNewRowNamingStrategy>getInstance(
            LiveTableNewRowNamingStrategy.class, "title")).thenReturn(title);

        assertSame(title, this.manager.get("title").orElseThrow());
    }

    @Test
    void getGivesNothingForAStrategyThatIsNotInstalled() throws Exception
    {
        assertEquals(Optional.empty(), this.manager.get("missing"));
    }

    @Test
    void getGivesNothingWithoutAHint() throws Exception
    {
        // No hint gives no strategy, even when one is installed under the default hint.
        when(this.contextComponentManager.hasComponent(LiveTableNewRowNamingStrategy.class, null)).thenReturn(true);

        assertEquals(Optional.empty(), this.manager.get(null));
        assertEquals(Optional.empty(), this.manager.get(" "));
    }

    @Test
    void getFailsForAStrategyThatCannotBeLoaded() throws Exception
    {
        when(this.contextComponentManager.hasComponent(LiveTableNewRowNamingStrategy.class, "title")).thenReturn(true);
        when(this.contextComponentManager.getInstance(LiveTableNewRowNamingStrategy.class, "title"))
            .thenThrow(new ComponentLookupException("broken"));

        LiveDataException exception = assertThrows(LiveDataException.class, () -> this.manager.get("title"));
        assertEquals("Failed to load the row naming strategy [title].", exception.getMessage());
    }

    @Test
    void getAllLeavesOutTheStrategiesThatCannotBeLoaded() throws Exception
    {
        LiveTableNewRowNamingStrategy title = mock();
        LiveTableNewRowNamingStrategy uuid = mock();
        givenStrategies("uuid", "broken", "title");
        when(this.contextComponentManager.getInstance(LiveTableNewRowNamingStrategy.class, "uuid")).thenReturn(uuid);
        when(this.contextComponentManager.getInstance(LiveTableNewRowNamingStrategy.class, "broken"))
            .thenThrow(new ComponentLookupException("broken"));
        when(this.contextComponentManager.getInstance(LiveTableNewRowNamingStrategy.class, "title")).thenReturn(title);

        Map<String, LiveTableNewRowNamingStrategy> strategies = this.manager.getAll();

        assertEquals(List.of("title", "uuid"), List.copyOf(strategies.keySet()));
        assertSame(title, strategies.get("title"));
        assertSame(uuid, strategies.get("uuid"));
        assertEquals("Failed to load the row naming strategy [broken]. Root cause is "
            + "[ComponentLookupException: broken].", this.logCapture.getMessage(0));
    }

    private void givenStrategies(String... hints)
    {
        List<ComponentDescriptor<LiveTableNewRowNamingStrategy>> descriptors = Stream.of(hints).map(hint -> {
            DefaultComponentDescriptor<LiveTableNewRowNamingStrategy> descriptor = new DefaultComponentDescriptor<>();
            descriptor.setRoleType(LiveTableNewRowNamingStrategy.class);
            descriptor.setRoleHint(hint);
            return (ComponentDescriptor<LiveTableNewRowNamingStrategy>) descriptor;
        }).toList();
        when(this.contextComponentManager.getComponentDescriptorList(LiveTableNewRowNamingStrategy.class))
            .thenReturn(descriptors);
    }
}
