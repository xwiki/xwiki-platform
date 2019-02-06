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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Unit tests for {@link DocumentCopyingEvent}.
 * 
 * @version $Id$
 */
public class DocumentCopyingEventTest
{
    private DocumentReference alice = new DocumentReference("wiki", "Users", "Alice");

    private DocumentReference bob = new DocumentReference("wiki", "Users", "Bob");

    private DocumentReference carol = new DocumentReference("wiki", "Users", "Carol");

    @Test
    public void equalsAndHashCode()
    {
        assertEquals(new DocumentCopyingEvent(), new DocumentCopyingEvent());
        assertEquals(new DocumentCopyingEvent().hashCode(), new DocumentCopyingEvent().hashCode());

        assertEquals(new DocumentCopyingEvent(alice, bob), new DocumentCopyingEvent(alice, bob));
        assertEquals(new DocumentCopyingEvent(bob, carol).hashCode(), new DocumentCopyingEvent(bob, carol).hashCode());

        assertNotEquals(new DocumentCopyingEvent(), null);
        assertNotEquals(new DocumentCopyingEvent(), new DocumentCopiedEvent());
        assertNotEquals(new DocumentCopyingEvent(alice, bob), new DocumentCopyingEvent(alice, carol));

        assertNotEquals(new DocumentCopyingEvent(alice, bob).hashCode(),
            new DocumentCopyingEvent(alice, carol).hashCode());
    }
}
