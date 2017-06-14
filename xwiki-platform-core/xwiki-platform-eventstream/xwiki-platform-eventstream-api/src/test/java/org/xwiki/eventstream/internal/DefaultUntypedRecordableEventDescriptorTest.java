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
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link DefaultUntypedRecordableEventDescriptor}.
 *
 * @version $Id$
 * @since 9.5RC1
 */
public class DefaultUntypedRecordableEventDescriptorTest
{
    @Test
    public void argumentsAssignment() throws Exception
    {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(ModelBridge.UNTYPED_EVENT_DESCRIPTOR_VALIDATION_EXPRESSION, "validationExpression");
        parameters.put(ModelBridge.UNTYPED_EVENT_DESCRIPTOR_OBJECT_TYPE, Arrays.asList("o1", "o2"));
        parameters.put(ModelBridge.UNTYPED_EVENT_DESCRIPTOR_DESCRIPTION, "descriptorDescription");
        parameters.put(ModelBridge.UNTYPED_EVENT_DESCRIPTOR_EVENT_TRIGGERS, Arrays.asList("t1", "t2"));
        parameters.put(ModelBridge.UNTYPED_EVENT_DESCRIPTOR_APPLICATION_NAME, "applicationName");
        parameters.put(ModelBridge.UNTYPED_EVENT_DESCRIPTOR_APPLICATION_ICON, "applicationIcon");
        parameters.put(ModelBridge.UNTYPED_EVENT_EVENT_TYPE, "eventType");

        DocumentReference documentReference = mock(DocumentReference.class);
        DocumentReference authorReference = mock(DocumentReference.class);

        DefaultUntypedRecordableEventDescriptor eventDescriptor =
                new DefaultUntypedRecordableEventDescriptor(
                        documentReference, authorReference, parameters);

        assertEquals("eventType", eventDescriptor.getEventType());
        assertEquals("validationExpression", eventDescriptor.getValidationExpression());
        assertEquals("descriptorDescription", eventDescriptor.getDescription());
        assertEquals("applicationName", eventDescriptor.getApplicationName());
        assertEquals("applicationIcon", eventDescriptor.getApplicationIcon());
        assertEquals(2, eventDescriptor.getEventTriggers().size());
        assertEquals(2, eventDescriptor.getObjectTypes().size());
    }
}
