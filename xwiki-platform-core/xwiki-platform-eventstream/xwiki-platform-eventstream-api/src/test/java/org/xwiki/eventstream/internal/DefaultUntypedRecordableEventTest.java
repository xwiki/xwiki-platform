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

import java.util.Collections;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.eventstream.RecordableEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link DefaultUntypedRecordableEvent}.
 *
 * @version $Id$
 * @since 9.6RC1
 */
public class DefaultUntypedRecordableEventTest
{
    private DefaultUntypedRecordableEvent defaultUntypedRecordableEvent;

    private final class RandomRecordableEvent implements RecordableEvent
    {
        @Override
        public boolean matches(Object o)
        {
            return false;
        }
    }

    @Before
    public void setUp() throws Exception
    {
        defaultUntypedRecordableEvent = new DefaultUntypedRecordableEvent("test");
    }

    @Test
    public void eventType() throws Exception
    {
        DefaultUntypedRecordableEvent event = new DefaultUntypedRecordableEvent("unitTestEvent");
        assertEquals("unitTestEvent", event.getEventType());
    }

    @Test
    public void matchesWithCorrectEvent() throws Exception
    {
        assertTrue(this.defaultUntypedRecordableEvent.matches(
                new DefaultUntypedRecordableEvent("otherEvent")));
    }

    @Test
    public void matchesWithNullObject() throws Exception
    {
        assertFalse(this.defaultUntypedRecordableEvent.matches(null));
    }

    @Test
    public void matchesWithIncorrectObject() throws Exception
    {
        assertFalse(this.defaultUntypedRecordableEvent.matches(new RandomRecordableEvent()));
    }

    @Test
    public void target() throws Exception
    {
        HashSet<String> target = new HashSet<>();
        target.add("a");
        target.add("b");
        DefaultUntypedRecordableEvent event = new DefaultUntypedRecordableEvent("unitTestEvent",
                target);
        assertEquals(target, event.getTarget());

        DefaultUntypedRecordableEvent event2 = new DefaultUntypedRecordableEvent("unitTestEvent");
        assertEquals(Collections.emptySet(), event2.getTarget());

        DefaultUntypedRecordableEvent event3 = new DefaultUntypedRecordableEvent();
        assertEquals(Collections.emptySet(), event3.getTarget());
    }
}
