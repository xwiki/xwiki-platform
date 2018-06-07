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

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.internal.multi.ComponentManagerManager;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.UntypedRecordableEventDescriptor;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 * @since 10.5RC1
 * @since 9.11.6
 */
public class DefaultRecordableEventDescriptorManagerTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultRecordableEventDescriptorManager> mocker =
            new MockitoComponentMockingRule<>(DefaultRecordableEventDescriptorManager.class);

    private ComponentManager contextComponentManager;
    private ComponentManagerManager componentManagerManager;
    private WikiDescriptorManager wikiDescriptorManager;

    @Before
    public void setUp() throws Exception
    {
        contextComponentManager = mocker.getInstance(ComponentManager.class, "context");
        componentManagerManager = mocker.getInstance(ComponentManagerManager.class);
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
    }

    @Test
    public void getRecordableEventDescriptors() throws Exception
    {
        // Mocks
        RecordableEventDescriptor descriptorWiki1_1 = mock(RecordableEventDescriptor.class);
        RecordableEventDescriptor descriptorWiki1_2 = mock(RecordableEventDescriptor.class);
        UntypedRecordableEventDescriptor descriptorWiki1_3 = mock(UntypedRecordableEventDescriptor.class);
        UntypedRecordableEventDescriptor descriptorWiki1_4 = mock(UntypedRecordableEventDescriptor.class);

        RecordableEventDescriptor descriptorWiki2_1 = mock(RecordableEventDescriptor.class);
        RecordableEventDescriptor descriptorWiki2_2 = mock(RecordableEventDescriptor.class);
        UntypedRecordableEventDescriptor descriptorWiki2_3 = mock(UntypedRecordableEventDescriptor.class);
        UntypedRecordableEventDescriptor descriptorWiki2_4 = mock(UntypedRecordableEventDescriptor.class);


        when(contextComponentManager.getInstanceList(RecordableEventDescriptor.class)).thenReturn(
                Arrays.asList(descriptorWiki1_1, descriptorWiki1_2));
        when(contextComponentManager.getInstanceList(UntypedRecordableEventDescriptor.class)).thenReturn(
                Arrays.asList(descriptorWiki1_3, descriptorWiki1_4));

        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("wiki1");

        when(descriptorWiki1_1.isEnabled("wiki1")).thenReturn(true);
        when(descriptorWiki1_2.isEnabled("wiki1")).thenReturn(false);
        when(descriptorWiki1_3.isEnabled("wiki1")).thenReturn(false);
        when(descriptorWiki1_4.isEnabled("wiki1")).thenReturn(true);

        when(wikiDescriptorManager.getAllIds()).thenReturn(Arrays.asList("wiki1", "wiki2", "wiki3"));

        ComponentManager wiki1ComponentManager = mock(ComponentManager.class);
        when(componentManagerManager.getComponentManager("wiki:wiki1", false)).thenReturn(wiki1ComponentManager);
        when(wiki1ComponentManager.getInstanceList(RecordableEventDescriptor.class)).thenReturn(
                Arrays.asList(descriptorWiki1_1, descriptorWiki1_2));
        when(wiki1ComponentManager.getInstanceList(UntypedRecordableEventDescriptor.class)).thenReturn(
                Arrays.asList(descriptorWiki1_3, descriptorWiki1_4));

        ComponentManager wiki2ComponentManager = mock(ComponentManager.class);
        when(componentManagerManager.getComponentManager("wiki:wiki2", false)).thenReturn(wiki2ComponentManager);
        when(wiki2ComponentManager.getInstanceList(RecordableEventDescriptor.class)).thenReturn(
                Arrays.asList(descriptorWiki2_1, descriptorWiki2_2));
        when(wiki2ComponentManager.getInstanceList(UntypedRecordableEventDescriptor.class)).thenReturn(
                Arrays.asList(descriptorWiki2_3, descriptorWiki2_4));

        when(descriptorWiki2_1.isEnabled("wiki2")).thenReturn(false);
        when(descriptorWiki2_2.isEnabled("wiki2")).thenReturn(true);
        when(descriptorWiki2_3.isEnabled("wiki2")).thenReturn(true);
        when(descriptorWiki2_4.isEnabled("wiki2")).thenReturn(false);

        // Test 1
        List<RecordableEventDescriptor> result = mocker.getComponentUnderTest().getRecordableEventDescriptors(true);

        // Checks
        assertNotNull(result);
        assertEquals(4, result.size());
        assertTrue(result.contains(descriptorWiki1_1));
        assertTrue(result.contains(descriptorWiki1_4));
        assertTrue(result.contains(descriptorWiki2_2));
        assertTrue(result.contains(descriptorWiki2_3));

        // Test 2
        List<RecordableEventDescriptor> result2 = mocker.getComponentUnderTest().getRecordableEventDescriptors(false);
        assertNotNull(result2);
        assertEquals(2, result2.size());
        assertTrue(result2.contains(descriptorWiki1_1));
        assertTrue(result2.contains(descriptorWiki1_4));
    }

    @Test
    public void getRecordableEventDescriptorsForEventType() throws Exception
    {
        // Mocks
        RecordableEventDescriptor descriptorWiki1_1 = mock(RecordableEventDescriptor.class);
        RecordableEventDescriptor descriptorWiki1_2 = mock(RecordableEventDescriptor.class);

        when(contextComponentManager.getInstanceList(RecordableEventDescriptor.class)).thenReturn(
                Arrays.asList(descriptorWiki1_1, descriptorWiki1_2));

        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("wiki1");

        when(descriptorWiki1_1.isEnabled("wiki1")).thenReturn(true);
        when(descriptorWiki1_2.isEnabled("wiki1")).thenReturn(true);
        when(descriptorWiki1_1.getEventType()).thenReturn("type1");
        when(descriptorWiki1_2.getEventType()).thenReturn("someType");

        // Test
        RecordableEventDescriptor result = mocker.getComponentUnderTest().getDescriptorForEventType("someType", false);

        // Checks
        assertNotNull(result);
        assertEquals(descriptorWiki1_2, result);

        // Test 2
        assertNull(mocker.getComponentUnderTest().getDescriptorForEventType("unknowType", false));
    }

}
