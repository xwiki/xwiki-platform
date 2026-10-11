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

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.RecordableEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link DefaultUntypedRecordableEvent}.
 *
 * @version $Id$
 * @since 9.6RC1
 */
class DefaultUntypedRecordableEventTest
{
    private DefaultUntypedRecordableEvent defaultUntypedRecordableEvent;

    private static final class RandomRecordableEvent implements RecordableEvent
    {
        @Override
        public boolean matches(Object o)
        {
            return false;
        }
    }

    @BeforeEach
    void setUp()
    {
        this.defaultUntypedRecordableEvent = new DefaultUntypedRecordableEvent("test");
    }

    @Test
    void eventType()
    {
        DefaultUntypedRecordableEvent event = new DefaultUntypedRecordableEvent("unitTestEvent");
        assertEquals("unitTestEvent", event.getEventType());
    }

    @Test
    void matchesWithCorrectEvent()
    {
        assertTrue(this.defaultUntypedRecordableEvent.matches(new DefaultUntypedRecordableEvent("otherEvent")));
    }

    @Test
    void matchesWithNullObject()
    {
        assertFalse(this.defaultUntypedRecordableEvent.matches(null));
    }

    @Test
    void matchesWithIncorrectObject()
    {
        assertFalse(this.defaultUntypedRecordableEvent.matches(new RandomRecordableEvent()));
    }

    @Test
    void target()
    {
        Set<String> target = Set.of("a", "b");
        DefaultUntypedRecordableEvent event = new DefaultUntypedRecordableEvent("unitTestEvent", target);
        assertEquals(target, event.getTarget());

        DefaultUntypedRecordableEvent event2 = new DefaultUntypedRecordableEvent("unitTestEvent");
        assertEquals(Set.of(), event2.getTarget());

        DefaultUntypedRecordableEvent event3 = new DefaultUntypedRecordableEvent();
        assertEquals(Set.of(), event3.getTarget());
    }
}
