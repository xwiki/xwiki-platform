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
package org.xwiki.records.internal.macro;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.livedata.livetable.LiveTableNewRowNamingStrategy;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RecordsCreationStrategies}.
 *
 * @version $Id$
 */
@ComponentTest
class RecordsCreationStrategiesTest
{
    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private RecordsCreationStrategies creationStrategies;

    @MockComponent
    @Named("context")
    private ComponentManager contextComponentManager;

    @Test
    void getMandatoryPropertiesAsksTheStrategyWithTheSourceParameters() throws Exception
    {
        Map<String, Object> sourceParameters = Map.of("className", "Clients.ProjectClass");
        LiveTableNewRowNamingStrategy title = mock();
        when(title.getMandatoryProperties(sourceParameters)).thenReturn(Set.of("doc.title"));
        when(this.contextComponentManager.hasComponent(LiveTableNewRowNamingStrategy.class, "title")).thenReturn(true);
        when(this.contextComponentManager.<LiveTableNewRowNamingStrategy>getInstance(
            LiveTableNewRowNamingStrategy.class, "title")).thenReturn(title);

        assertEquals(Optional.of(Set.of("doc.title")),
            this.creationStrategies.getMandatoryProperties("title", sourceParameters));
    }

    @Test
    void getMandatoryPropertiesGivesNothingForAStrategyThatIsNotInstalled()
    {
        assertEquals(Optional.empty(), this.creationStrategies.getMandatoryProperties("missing", Map.of()));
    }

    @Test
    void getMandatoryPropertiesGivesNothingForAStrategyThatCannotBeLoaded() throws Exception
    {
        when(this.contextComponentManager.hasComponent(LiveTableNewRowNamingStrategy.class, "title")).thenReturn(true);
        when(this.contextComponentManager.getInstance(LiveTableNewRowNamingStrategy.class, "title"))
            .thenThrow(new ComponentLookupException("broken"));

        assertEquals(Optional.empty(), this.creationStrategies.getMandatoryProperties("title", Map.of()));
        assertEquals(
            "Failed to load the new row naming strategy [title]. Root cause is [ComponentLookupException: broken].",
            this.logCapture.getMessage(0));
    }
}
