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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventFactory;
import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.eventstream.UntypedRecordableEvent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultUntypedRecordableEventConverter}.
 *
 * @version $Id$
 * @since 9.6RC1
 */
@ComponentTest
class DefaultUntypedRecordableEventConverterTest
{
    @InjectMockComponents
    private DefaultUntypedRecordableEventConverter converter;

    @MockComponent
    private EventFactory eventFactory;

    @BeforeEach
    void beforeEach()
    {
        when(this.eventFactory.createRawEvent()).thenReturn(new DefaultEvent());
    }

    @Test
    void getSupportedEvents() throws Exception
    {
        List<RecordableEvent> supportedEvents = this.converter.getSupportedEvents();

        assertTrue(supportedEvents.get(0).matches(new DefaultUntypedRecordableEvent()));
    }

    @Test
    void convert() throws Exception
    {
        UntypedRecordableEvent event = mock(UntypedRecordableEvent.class);
        when(event.getEventType()).thenReturn("some type");
        Event result = this.converter.convert(event, "source", "data");
        assertEquals("some type", result.getType());
    }
}
