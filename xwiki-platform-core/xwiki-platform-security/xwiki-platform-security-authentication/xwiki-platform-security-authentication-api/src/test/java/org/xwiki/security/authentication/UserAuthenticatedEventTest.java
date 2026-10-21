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
package org.xwiki.security.authentication;

import org.junit.jupiter.api.Test;
import org.xwiki.observation.event.AllEvent;
import org.xwiki.user.UserReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test of {@link UserAuthenticatedEvent}.
 *
 * @version $Id$
 */
class UserAuthenticatedEventTest
{
    /**
     * A {@link UserReference} with value-based equality, to make sure {@link UserAuthenticatedEvent#matches(Object)}
     * relies on {@link Object#equals(Object)} and not on identity.
     */
    private record TestUserReference(String name) implements UserReference
    {
        @Override
        public boolean isGlobal()
        {
            return true;
        }
    }

    private static final UserReference ALICE = new TestUserReference("Alice");

    private static final UserReference BOB = new TestUserReference("Bob");

    @Test
    void getUserReference()
    {
        assertSame(ALICE, new UserAuthenticatedEvent(ALICE).getUserReference());
        assertNull(new UserAuthenticatedEvent(null).getUserReference());
        assertNull(new UserAuthenticatedEvent().getUserReference());
    }

    @Test
    void matchesWithoutUserReference()
    {
        UserAuthenticatedEvent event = new UserAuthenticatedEvent();

        assertTrue(event.matches(new UserAuthenticatedEvent()));
        assertTrue(event.matches(new UserAuthenticatedEvent(ALICE)));
        assertTrue(event.matches(new UserAuthenticatedEvent(null)));
    }

    @Test
    void matchesWithUserReference()
    {
        UserAuthenticatedEvent event = new UserAuthenticatedEvent(ALICE);

        assertTrue(event.matches(new UserAuthenticatedEvent(ALICE)));
        assertTrue(event.matches(new UserAuthenticatedEvent(new TestUserReference("Alice"))));

        assertFalse(event.matches(new UserAuthenticatedEvent(BOB)));
        assertFalse(event.matches(new UserAuthenticatedEvent()));
        assertFalse(event.matches(new UserAuthenticatedEvent(null)));
    }

    @Test
    void matchesOtherTypes()
    {
        assertFalse(new UserAuthenticatedEvent().matches(null));
        assertFalse(new UserAuthenticatedEvent().matches(AllEvent.ALLEVENT));
        assertFalse(new UserAuthenticatedEvent().matches(new AuthenticationFailureEvent()));
        assertFalse(new UserAuthenticatedEvent().matches("not an event"));

        assertFalse(new UserAuthenticatedEvent(ALICE).matches(null));
        assertFalse(new UserAuthenticatedEvent(ALICE).matches(new AuthenticationFailureEvent()));
    }
}
