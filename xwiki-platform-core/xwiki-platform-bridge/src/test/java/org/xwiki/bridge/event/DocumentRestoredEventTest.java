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
package org.xwiki.bridge.event;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link DocumentRestoredEvent}.
 * 
 * @version $Id$
 */
class DocumentRestoredEventTest
{
    @Test
    void match()
    {
        DocumentReference alice = new DocumentReference("wiki", "Users", "Alice");
        DocumentReference bob = new DocumentReference("wiki", "Users", "Bob");

        assertFalse(new DocumentRestoredEvent().matches(new DocumentRestoringEvent()));
        assertTrue(new DocumentRestoredEvent().matches(new DocumentRestoredEvent()));
        assertTrue(new DocumentRestoredEvent().matches(new DocumentRestoredEvent(alice, 1)));

        assertFalse(new DocumentRestoredEvent(bob).matches(new DocumentRestoredEvent(alice, 1)));
        assertTrue(new DocumentRestoredEvent(bob).matches(new DocumentRestoredEvent(bob, 2)));

        assertFalse(new DocumentRestoredEvent(bob, 1).matches(new DocumentRestoredEvent(bob)));
        assertFalse(new DocumentRestoredEvent(bob, 1).matches(new DocumentRestoredEvent(bob, 2)));

        assertFalse(new DocumentRestoredEvent(bob, 1).matches(new DocumentRestoredEvent(alice, 1)));
    }
}
