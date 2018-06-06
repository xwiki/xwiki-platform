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

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.RecordableEventDescriptorManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 * @since
 */
public class RecordableEventDescriptorHelperTest
{
    @Rule
    public final MockitoComponentMockingRule<RecordableEventDescriptorHelper> mocker =
            new MockitoComponentMockingRule<>(RecordableEventDescriptorHelper.class);

    private WikiDescriptorManager wikiDescriptorManager;
    private RecordableEventDescriptorManager recordableEventDescriptorManager;
    private Execution execution;

    @Before
    public void setUp() throws Exception
    {
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        recordableEventDescriptorManager = mocker.getInstance(RecordableEventDescriptorManager.class);
        execution = mocker.getInstance(Execution.class);
    }

    @Test
    public void hasDescriptorTest() throws Exception
    {
        DocumentReference user = new DocumentReference("xwiki", "XWiki", "UserA");
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("xwiki");
        ExecutionContext executionContext = new ExecutionContext();
        when(execution.getContext()).thenReturn(executionContext);

        RecordableEventDescriptor descriptor1 = mock(RecordableEventDescriptor.class);
        RecordableEventDescriptor descriptor2 = mock(RecordableEventDescriptor.class);
        when(descriptor1.getEventType()).thenReturn("eventType1");
        when(descriptor2.getEventType()).thenReturn("eventType2");

        when(recordableEventDescriptorManager.getRecordableEventDescriptors(true)).thenReturn(
                Arrays.asList(descriptor1, descriptor2));

        assertTrue(mocker.getComponentUnderTest().hasDescriptor("eventType1", user));
        assertFalse(mocker.getComponentUnderTest().hasDescriptor("eventType3", user));

        List<RecordableEventDescriptor> descriptorList = (List<RecordableEventDescriptor>) executionContext
                .getProperty("RecordableEventDescriptorHelperCache_xwiki:XWiki.UserA");
        assertNotNull(descriptorList);
        assertTrue(descriptorList.contains(descriptor1));
        assertTrue(descriptorList.contains(descriptor2));
        assertEquals(2, descriptorList.size());
    }

    @Test
    public void hasDescriptorWhenInCacheTest() throws Exception
    {
        DocumentReference user = new DocumentReference("xwiki", "XWiki", "UserA");
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("xwiki");
        ExecutionContext executionContext = new ExecutionContext();
        when(execution.getContext()).thenReturn(executionContext);

        RecordableEventDescriptor descriptor1 = mock(RecordableEventDescriptor.class);
        RecordableEventDescriptor descriptor2 = mock(RecordableEventDescriptor.class);
        when(descriptor2.getEventType()).thenReturn("eventType");

        executionContext.setProperty("RecordableEventDescriptorHelperCache_xwiki:XWiki.UserA",
                Arrays.asList(descriptor1, descriptor2));

        assertTrue(mocker.getComponentUnderTest().hasDescriptor("eventType", user));
        verifyZeroInteractions(recordableEventDescriptorManager);
    }
}
