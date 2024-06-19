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
 * Unit tests for {@link DocumentRollingBackEvent}.
 * 
 * @version $Id$
 */
class DocumentRollingBackEventTest
{
    @Test
    void match()
    {
        DocumentReference alice = new DocumentReference("wiki", "Users", "Alice");
        DocumentReference bob = new DocumentReference("wiki", "Users", "Bob");

        assertFalse(new DocumentRollingBackEvent().matches(new DocumentRolledBackEvent()));
        assertTrue(new DocumentRollingBackEvent().matches(new DocumentRollingBackEvent()));
        assertTrue(new DocumentRollingBackEvent().matches(new DocumentRollingBackEvent(alice, "2.3")));

        assertFalse(new DocumentRollingBackEvent(bob).matches(new DocumentRollingBackEvent(alice, "1.5")));
        assertTrue(new DocumentRollingBackEvent(bob).matches(new DocumentRollingBackEvent(bob, "4.2")));

        assertFalse(new DocumentRollingBackEvent(bob, "3.1").matches(new DocumentRollingBackEvent(bob)));
        assertFalse(new DocumentRollingBackEvent(bob, "7.6").matches(new DocumentRollingBackEvent(bob, "7.5")));
        assertTrue(new DocumentRollingBackEvent(bob, "5.8").matches(new DocumentRollingBackEvent(bob, "5.8")));

        assertFalse(new DocumentRollingBackEvent(bob, "1.0").matches(new DocumentRollingBackEvent(alice, "1.0")));
    }
}
