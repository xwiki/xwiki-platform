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
package org.xwiki.notifications.internal.page;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.eventstream.RecordableEventDescriptorContainer;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.page.PageNotificationEventDescriptor;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class DefaultPageNotificationEventDescriptorContainerTest
{
    @Rule
    public final MockitoComponentMockingRule<DefaultPageNotificationEventDescriptorContainer> mocker =
            new MockitoComponentMockingRule<>(DefaultPageNotificationEventDescriptorContainer.class);

    private RecordableEventDescriptorContainer recordableEventDescriptorContainer;

    private PageNotificationEventDescriptor eventDescriptor1;
    private PageNotificationEventDescriptor eventDescriptor2;

    @Before
    public void setUp() throws Exception
    {
        recordableEventDescriptorContainer = mocker.registerMockComponent(RecordableEventDescriptorContainer.class);

        // «Reset» the component by giving him 0 descriptors
        mocker.getComponentUnderTest().updateDescriptorList(Collections.emptyList());

        eventDescriptor1 = mock(PageNotificationEventDescriptor.class);
        when(eventDescriptor1.getEventType()).thenReturn("eventType1");
        eventDescriptor2 = mock(PageNotificationEventDescriptor.class);
        when(eventDescriptor2.getEventType()).thenReturn("eventType2");
    }

    @Test
    public void descriptorListWithoutElements() throws Exception
    {
        assertEquals(0, mocker.getComponentUnderTest().getDescriptorList().size());
    }

    @Test
    public void updateDescriptorList() throws Exception
    {
        List<PageNotificationEventDescriptor> eventDescriptorList = Arrays.asList(eventDescriptor1, eventDescriptor2);

        mocker.getComponentUnderTest().updateDescriptorList(eventDescriptorList);

        List<PageNotificationEventDescriptor> result = mocker.getComponentUnderTest().getDescriptorList();
        assertEquals(2, result.size());
        assertTrue(result.containsAll(eventDescriptorList));
    }

    @Test
    public void getDescriptorByType() throws Exception
    {
        mocker.getComponentUnderTest().updateDescriptorList(Arrays.asList(eventDescriptor1, eventDescriptor2));

        assertEquals(eventDescriptor1, mocker.getComponentUnderTest().getDescriptorByType("eventType1"));
        assertEquals(eventDescriptor2, mocker.getComponentUnderTest().getDescriptorByType("eventType2"));
    }

    @Test(expected = NotificationException.class)
    public void getDescriptorByTypeWithNullType() throws Exception
    {
        mocker.getComponentUnderTest().getDescriptorByType(null);
    }

    @Test(expected = NotificationException.class)
    public void getDescriptorByTypeWithWrongType() throws Exception
    {
        mocker.getComponentUnderTest().getDescriptorByType("eventType3");
    }
}
