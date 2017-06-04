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
package org.xwiki.notifications.page;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.eventstream.RecordableEventDescriptorContainer;
import org.xwiki.model.reference.DocumentReference;

import com.google.common.collect.ImmutableMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @version $Id$
 */
public class PageNotificationEventDescriptorTest
{
    private List<String> eventTriggersList;
    private DocumentReference authorReferenceMock;

    @Before
    public void setUp() throws Exception
    {
        this.eventTriggersList = Arrays.asList("element1", "element2");
        this.authorReferenceMock = mock(DocumentReference.class);
    }

    private PageNotificationEventDescriptor instanciateDescriptor() throws Exception
    {
        return new PageNotificationEventDescriptor(ImmutableMap.<String, String>builder()
                .put("applicationName", "applicationNameValue")
                .put("eventType", "eventTypeValue")
                .put("eventPrettyName", "eventPrettyNameValue")
                .put("eventIcon", "eventIconValue")
                .put("objectType", "objectTypeValue")
                .put("validationExpression", "validationExpressionValue")
                .put("notificationTemplate", "notificationTemplateValue")
                .build(),
                this.eventTriggersList,
                this.authorReferenceMock);
    }

    @Test
    public void constructorParameters() throws Exception
    {
        // Check that every parameter given to the constructor are correctly stored

        PageNotificationEventDescriptor mockedElement = instanciateDescriptor();

        assertEquals("applicationNameValue", mockedElement.getApplicationName());
        assertEquals("eventTypeValue", mockedElement.getEventType());
        assertEquals("eventTypeValue", mockedElement.getEventName());
        assertEquals("eventPrettyNameValue", mockedElement.getDescription());
        assertEquals("eventIconValue", mockedElement.getApplicationIcon());
        assertEquals("objectTypeValue", mockedElement.getObjectType());
        assertEquals("validationExpressionValue", mockedElement.getValidationExpression());
        assertEquals("notificationTemplateValue", mockedElement.getNotificationTemplate());

        assertEquals(this.eventTriggersList, mockedElement.getEventTriggers());
        assertEquals(this.authorReferenceMock, mockedElement.getAuthorReference());
    }

    @Test
    public void testDescriptorRegistrationAndUnRegistration() throws Exception
    {
        PageNotificationEventDescriptor mockedElement = instanciateDescriptor();

        RecordableEventDescriptorContainer container = mock(RecordableEventDescriptorContainer.class);

        mockedElement.register(container);
        verify(container, times(1)).addRecordableEventDescriptor(mockedElement);

        mockedElement.unRegister();
        verify(container, times(1)).deleteRecordableEventDescriptor(mockedElement);
    }
}
