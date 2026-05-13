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
package org.xwiki.eventstream.internal;

import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.component.internal.multi.ComponentManagerManager;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.UntypedRecordableEventDescriptor;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 * @since 10.5RC1
 * @since 9.11.6
 */
@ComponentTest
class DefaultRecordableEventDescriptorManagerTest
{
    @InjectMockComponents
    private DefaultRecordableEventDescriptorManager manager;

    @MockComponent
    @Named("context")
    private ComponentManager contextComponentManager;

    @MockComponent
    private ComponentManagerManager componentManagerManager;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @Test
    void getRecordableEventDescriptors() throws Exception
    {
        // Mocks
        RecordableEventDescriptor descriptorWiki11 = mock(RecordableEventDescriptor.class);
        RecordableEventDescriptor descriptorWiki12 = mock(RecordableEventDescriptor.class);
        UntypedRecordableEventDescriptor descriptorWiki13 = mock(UntypedRecordableEventDescriptor.class);
        UntypedRecordableEventDescriptor descriptorWiki14 = mock(UntypedRecordableEventDescriptor.class);

        RecordableEventDescriptor descriptorWiki21 = mock(RecordableEventDescriptor.class);
        RecordableEventDescriptor descriptorWiki22 = mock(RecordableEventDescriptor.class);
        UntypedRecordableEventDescriptor descriptorwiki23 = mock(UntypedRecordableEventDescriptor.class);
        UntypedRecordableEventDescriptor descriptorWiki24 = mock(UntypedRecordableEventDescriptor.class);

        when(this.contextComponentManager.getInstanceList(RecordableEventDescriptor.class))
            .thenReturn(List.of(descriptorWiki11, descriptorWiki12));
        when(this.contextComponentManager.getInstanceList(UntypedRecordableEventDescriptor.class))
            .thenReturn(List.of(descriptorWiki13, descriptorWiki14));

        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("wiki1");

        when(descriptorWiki11.isEnabled("wiki1")).thenReturn(true);
        when(descriptorWiki12.isEnabled("wiki1")).thenReturn(false);
        when(descriptorWiki13.isEnabled("wiki1")).thenReturn(false);
        when(descriptorWiki14.isEnabled("wiki1")).thenReturn(true);

        when(this.wikiDescriptorManager.getAllIds()).thenReturn(List.of("wiki1", "wiki2", "wiki3"));

        ComponentManager wiki1ComponentManager = mock(ComponentManager.class);
        when(this.componentManagerManager.getComponentManager("wiki:wiki1", false)).thenReturn(wiki1ComponentManager);
        when(wiki1ComponentManager.getInstanceList(RecordableEventDescriptor.class))
            .thenReturn(List.of(descriptorWiki11, descriptorWiki12));
        when(wiki1ComponentManager.getInstanceList(UntypedRecordableEventDescriptor.class))
            .thenReturn(List.of(descriptorWiki13, descriptorWiki14));

        ComponentManager wiki2ComponentManager = mock(ComponentManager.class);
        when(this.componentManagerManager.getComponentManager("wiki:wiki2", false)).thenReturn(wiki2ComponentManager);
        when(wiki2ComponentManager.getInstanceList(RecordableEventDescriptor.class))
            .thenReturn(List.of(descriptorWiki21, descriptorWiki22));
        when(wiki2ComponentManager.getInstanceList(UntypedRecordableEventDescriptor.class))
            .thenReturn(List.of(descriptorwiki23, descriptorWiki24));

        when(descriptorWiki21.isEnabled("wiki2")).thenReturn(false);
        when(descriptorWiki22.isEnabled("wiki2")).thenReturn(true);
        when(descriptorwiki23.isEnabled("wiki2")).thenReturn(true);
        when(descriptorWiki24.isEnabled("wiki2")).thenReturn(false);

        // Test 1
        List<RecordableEventDescriptor> result = this.manager.getRecordableEventDescriptors(true);

        // Checks
        assertNotNull(result);
        assertEquals(4, result.size());
        assertTrue(result.contains(descriptorWiki11));
        assertTrue(result.contains(descriptorWiki14));
        assertTrue(result.contains(descriptorWiki22));
        assertTrue(result.contains(descriptorwiki23));

        // Test 2
        List<RecordableEventDescriptor> result2 = this.manager.getRecordableEventDescriptors(false);
        assertNotNull(result2);
        assertEquals(2, result2.size());
        assertTrue(result2.contains(descriptorWiki11));
        assertTrue(result2.contains(descriptorWiki14));
    }

    @Test
    void getRecordableEventDescriptorsForEventType() throws Exception
    {
        // Mocks
        RecordableEventDescriptor descriptorWiki11 = mock(RecordableEventDescriptor.class);
        RecordableEventDescriptor descriptorWiki12 = mock(RecordableEventDescriptor.class);

        when(this.contextComponentManager.getInstanceList(RecordableEventDescriptor.class))
            .thenReturn(List.of(descriptorWiki11, descriptorWiki12));

        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("wiki1");

        when(descriptorWiki11.isEnabled("wiki1")).thenReturn(true);
        when(descriptorWiki12.isEnabled("wiki1")).thenReturn(true);
        when(descriptorWiki11.getEventType()).thenReturn("type1");
        when(descriptorWiki12.getEventType()).thenReturn("someType");

        // Test
        RecordableEventDescriptor result = this.manager.getDescriptorForEventType("someType", false);

        // Checks
        assertNotNull(result);
        assertEquals(descriptorWiki12, result);

        // Test 2
        assertNull(this.manager.getDescriptorForEventType("unknowType", false));
    }
}
