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
 * Unit tests for {@link DocumentRestoringEvent}.
 * 
 * @version $Id$
 */
class DocumentRestoringEventTest
{
    @Test
    void match()
    {
        DocumentReference alice = new DocumentReference("wiki", "Users", "Alice");
        DocumentReference bob = new DocumentReference("wiki", "Users", "Bob");

        assertFalse(new DocumentRestoringEvent().matches(new DocumentRestoredEvent()));
        assertTrue(new DocumentRestoringEvent().matches(new DocumentRestoringEvent()));
        assertTrue(new DocumentRestoringEvent().matches(new DocumentRestoringEvent(alice, 1)));

        assertFalse(new DocumentRestoringEvent(bob).matches(new DocumentRestoringEvent(alice, 1)));
        assertTrue(new DocumentRestoringEvent(bob).matches(new DocumentRestoringEvent(bob, 2)));

        assertFalse(new DocumentRestoringEvent(bob, 1).matches(new DocumentRestoringEvent(bob)));
        assertFalse(new DocumentRestoringEvent(bob, 1).matches(new DocumentRestoringEvent(bob, 2)));

        assertFalse(new DocumentRestoringEvent(bob, 1).matches(new DocumentRestoringEvent(alice, 1)));
    }
}
