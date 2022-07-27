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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate {@link DefaultEvent}.
 * 
 * @version $Id$
 */
class DefaultEventTest
{
    @Test
    void equals()
    {
        DefaultEvent event1 = new DefaultEvent();
        DefaultEvent event2 = new DefaultEvent();

        assertEquals(event1, event1);
        assertEquals(event1, event2);

        assertNotEquals(event1, null);
        assertNotEquals(null, event1);

        assertNotEquals(event1, new Object());
        assertNotEquals(new Object(), event1);

        event1.setId("id1");

        assertNotEquals(event1, event2);

        event2.setId("id2");

        assertNotEquals(event1, event2);

        event2.setId("id1");

        assertEquals(event1, event2);
    }

    @Test
    void getParameters()
    {
        DefaultEvent event = new DefaultEvent();

        assertTrue(event.getParameters().isEmpty());

        Map<String, Object> custom = new HashMap<>();
        custom.put("key1", "value");
        custom.put("key2", null);
        custom.put("key3", 42);

        event.setCustom(custom);

        Map<String, String> parameters = event.getParameters();

        assertEquals(3, parameters.size());
        assertEquals("value", parameters.get("key1"));
        assertTrue(parameters.containsKey("key2"));
        assertEquals(null, parameters.get("key2"));
        assertEquals("42", parameters.get("key3"));
    }
}
