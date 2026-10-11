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
package org.xwiki.notifications.sources.internal;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.RecordableEventDescriptorManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
@ComponentTest
class RecordableEventDescriptorHelperTest
{
    @InjectMockComponents
    private RecordableEventDescriptorHelper helper;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private RecordableEventDescriptorManager recordableEventDescriptorManager;

    @MockComponent
    private Execution execution;

    @Test
    void hasDescriptorTest() throws Exception
    {
        DocumentReference user = new DocumentReference("xwiki", "XWiki", "UserA");
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn("xwiki");
        ExecutionContext executionContext = new ExecutionContext();
        when(this.execution.getContext()).thenReturn(executionContext);

        RecordableEventDescriptor descriptor1 = mock(RecordableEventDescriptor.class);
        RecordableEventDescriptor descriptor2 = mock(RecordableEventDescriptor.class);
        when(descriptor1.getEventType()).thenReturn("eventType1");
        when(descriptor2.getEventType()).thenReturn("eventType2");

        when(this.recordableEventDescriptorManager.getRecordableEventDescriptors(true)).thenReturn(
            List.of(descriptor1, descriptor2));

        assertTrue(this.helper.hasDescriptor("eventType1", user));
        assertFalse(this.helper.hasDescriptor("eventType3", user));

        List<RecordableEventDescriptor> descriptorList = (List<RecordableEventDescriptor>) executionContext
            .getProperty("RecordableEventDescriptorHelperCache_xwiki:XWiki.UserA");
        assertNotNull(descriptorList);
        assertTrue(descriptorList.contains(descriptor1));
        assertTrue(descriptorList.contains(descriptor2));
        assertEquals(2, descriptorList.size());
    }

    @Test
    void hasDescriptorWhenInCacheTest() throws Exception
    {
        DocumentReference user = new DocumentReference("xwiki", "XWiki", "UserA");
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn("xwiki");
        ExecutionContext executionContext = new ExecutionContext();
        when(this.execution.getContext()).thenReturn(executionContext);

        RecordableEventDescriptor descriptor1 = mock(RecordableEventDescriptor.class);
        RecordableEventDescriptor descriptor2 = mock(RecordableEventDescriptor.class);
        when(descriptor2.getEventType()).thenReturn("eventType");

        executionContext.setProperty("RecordableEventDescriptorHelperCache_xwiki:XWiki.UserA",
            List.of(descriptor1, descriptor2));

        assertTrue(this.helper.hasDescriptor("eventType", user));
        verifyNoInteractions(this.recordableEventDescriptorManager);
    }
}
