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
package org.xwiki.refactoring.event;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link DocumentRenamingEvent}.
 * 
 * @version $Id$
 */
public class DocumentRenamingEventTest
{
    private DocumentReference alice = new DocumentReference("wiki", "Users", "Alice");

    private DocumentReference bob = new DocumentReference("wiki", "Users", "Bob");

    private DocumentReference carol = new DocumentReference("wiki", "Users", "Carol");

    @Test
    public void equalsAndHashCode()
    {
        assertEquals(new DocumentRenamingEvent(), new DocumentRenamingEvent());
        assertEquals(new DocumentRenamingEvent().hashCode(), new DocumentRenamingEvent().hashCode());

        assertEquals(new DocumentRenamingEvent(alice, bob), new DocumentRenamingEvent(alice, bob));
        assertEquals(new DocumentRenamingEvent(bob, carol).hashCode(),
            new DocumentRenamingEvent(bob, carol).hashCode());

        assertNotEquals(new DocumentRenamingEvent(), null);
        assertNotEquals(new DocumentRenamingEvent(), new DocumentRenamedEvent());
        assertNotEquals(new DocumentRenamingEvent(alice, bob), new DocumentRenamingEvent(alice, carol));

        assertNotEquals(new DocumentRenamingEvent(alice, bob).hashCode(),
            new DocumentRenamingEvent(alice, carol).hashCode());
    }

    @Test
    public void cancel()
    {
        DocumentRenamingEvent event = new DocumentRenamingEvent();

        assertFalse(event.isCanceled());
        assertNull(event.getReason());

        event.cancel("Something");

        assertTrue(event.isCanceled());
        assertEquals("Something", event.getReason());
    }

    @Test
    public void matches()
    {
        assertFalse(new DocumentRenamingEvent().matches(null));
        assertFalse(new DocumentRenamingEvent().matches(new DocumentRenamedEvent()));

        assertTrue(new DocumentRenamingEvent().matches(new DocumentRenamingEvent()));
        assertTrue(new DocumentRenamingEvent().matches(new DocumentRenamingEvent(alice, bob)));

        assertFalse(new DocumentRenamingEvent(alice, null).matches(new DocumentRenamingEvent()));
        assertFalse(new DocumentRenamingEvent(alice, null).matches(new DocumentRenamingEvent(bob, null)));
        assertFalse(new DocumentRenamingEvent(alice, null).matches(new DocumentRenamingEvent(null, alice)));

        assertTrue(new DocumentRenamingEvent(alice, null).matches(new DocumentRenamingEvent(alice, null)));
        assertTrue(new DocumentRenamingEvent(alice, null).matches(new DocumentRenamingEvent(alice, carol)));

        assertFalse(new DocumentRenamingEvent(null, alice).matches(new DocumentRenamingEvent()));
        assertFalse(new DocumentRenamingEvent(null, alice).matches(new DocumentRenamingEvent(null, bob)));
        assertFalse(new DocumentRenamingEvent(null, alice).matches(new DocumentRenamingEvent(alice, null)));

        assertTrue(new DocumentRenamingEvent(null, alice).matches(new DocumentRenamingEvent(null, alice)));
        assertTrue(new DocumentRenamingEvent(null, alice).matches(new DocumentRenamingEvent(carol, alice)));

        assertFalse(new DocumentRenamingEvent(alice, bob).matches(new DocumentRenamingEvent()));
        assertFalse(new DocumentRenamingEvent(alice, bob).matches(new DocumentRenamingEvent(bob, alice)));
        assertFalse(new DocumentRenamingEvent(alice, bob).matches(new DocumentRenamingEvent(alice, carol)));
        assertFalse(new DocumentRenamingEvent(alice, bob).matches(new DocumentRenamingEvent(carol, bob)));

        assertTrue(new DocumentRenamingEvent(alice, bob).matches(new DocumentRenamingEvent(alice, bob)));
    }
}
