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
 * Unit tests for {@link DocumentRenamedEvent}.
 * 
 * @version $Id$
 */
public class DocumentRenamedEventTest
{
    private DocumentReference alice = new DocumentReference("wiki", "Users", "Alice");

    private DocumentReference bob = new DocumentReference("wiki", "Users", "Bob");

    private DocumentReference carol = new DocumentReference("wiki", "Users", "Carol");

    @Test
    public void equalsAndHashCode()
    {
        assertEquals(new DocumentRenamedEvent(), new DocumentRenamedEvent());
        assertEquals(new DocumentRenamedEvent().hashCode(), new DocumentRenamedEvent().hashCode());

        assertEquals(new DocumentRenamedEvent(alice, bob), new DocumentRenamedEvent(alice, bob));
        assertEquals(new DocumentRenamedEvent(bob, carol).hashCode(), new DocumentRenamedEvent(bob, carol).hashCode());

        assertNotEquals(new DocumentRenamedEvent(), null);
        assertNotEquals(new DocumentRenamedEvent(), new DocumentRenamingEvent());
        assertNotEquals(new DocumentRenamedEvent(alice, bob), new DocumentRenamedEvent(alice, carol));

        assertNotEquals(new DocumentRenamedEvent(alice, bob).hashCode(),
            new DocumentRenamedEvent(alice, carol).hashCode());
    }
}
