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
package org.xwiki.bridge;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.ActionExecutedEvent;
import org.xwiki.bridge.event.ActionExecutingEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@link ActionExecutedEvent} event type.
 * 
 * @version $Id$
 */
class ActionExecutedEventTest
{
    // Tests for constructors

    @Test
    void defaultConstructor()
    {
        ActionExecutedEvent event = new ActionExecutedEvent();
        assertNull(event.getActionName(), "A default action was used!");
    }

    @Test
    void constructorWithActionName()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        assertEquals("something", event.getActionName(), "Action name was lost!");
    }

    // Tests for matches(Object)

    @Test
    void matchesSameObject()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        assertTrue(event.matches(event), "Same object wasn't matched!");
    }

    @Test
    void matchesSameAction()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        assertTrue(event.matches(new ActionExecutedEvent("something")), "Same action wasn't matched!");
    }

    @Test
    void doesntMatchNull()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        assertFalse(event.matches(null), "null was matched!");
    }

    @Test
    void doesntMatchWildcardAction()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        assertFalse(event.matches(new ActionExecutedEvent()), "Wildcard action was matched!");
    }

    @Test
    void doesntMatchDifferentAction()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        assertFalse(event.matches(new ActionExecutedEvent("else")), "A different action was matched!");
    }

    @Test
    void doesntMatchDifferentCaseAction()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        assertFalse(event.matches(new ActionExecutedEvent("SomeThing")), "Action matching was case insensitive!");
    }

    @Test
    void doesntMatchDifferentTypeOfAction()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        assertFalse(event.matches(new ActionExecutingEvent("something")), "A different type of action was matched!");
    }

    @Test
    void wildcardActionMatchesAll()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        assertTrue(new ActionExecutedEvent().matches(event), "Wildcard action didn't match!");
        assertTrue(new ActionExecutedEvent().matches(new ActionExecutedEvent()),
            "Wildcard action didn't match another wildcard action!");
    }

    @Test
    void wildcardActionDoesnMatchNull()
    {
        assertFalse(new ActionExecutedEvent().matches(null), "Wildcard action matched null!");
    }

    @Test
    void emptyActionDoesntMatch()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        assertFalse(new ActionExecutedEvent("").matches(event), "Empty action behaves as wildcard!");
    }

    // Tests for equals(Object)

    @Test
    void equalsSameObject()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        assertEquals(event, event, "Same object wasn't equal!");
    }

    @Test
    void equalsSameAction()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        assertEquals(new ActionExecutedEvent("something"), event, "Same action wasn't equal!");
    }

    @Test
    void equalsWithNull()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        assertNotEquals(null, event, "null was equal!");
    }

    @Test
    void doesntEqualWildcardAction()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        assertNotEquals(new ActionExecutedEvent(), event, "Wildcard action was equal!");
    }

    @Test
    void doesntEqualDifferentAction()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        assertNotEquals(new ActionExecutedEvent("else"), event, "A different action was equal!");
    }

    @Test
    void doesntEqualDifferentCaseAction()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        assertNotEquals(new ActionExecutedEvent("SomeThing"), event, "Action equals comparison was case insensitive!");
    }

    @Test
    void doesntEqualDifferentTypeOfAction()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        assertNotEquals(new ActionExecutingEvent("something"), event, "Same object isn't matched!");
    }

    @Test
    void wildcardActionDoesntEqualOtherActions()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        assertNotEquals(new ActionExecutedEvent(), event, "Wildcard action equals another action!");
    }

    @Test
    void wildcardActionDoesntEqualEmptyAction()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("");
        assertNotEquals(new ActionExecutedEvent(), event, "Wildcard action equals another action!");
    }

    @Test
    void wildcardActionEqualsWildcardAction()
    {
        assertEquals(new ActionExecutedEvent(), new ActionExecutedEvent(),
            "Wildcard action isn't equal to another wildcard action");
    }

    @Test
    void wildcardActionDoesntEqualNull()
    {
        assertNotEquals(null, new ActionExecutedEvent(), "Wildcard action equals null!");
    }

    // Tests for hashCode()

    @Test
    void hashCodeNotZero()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        assertNotEquals(0, event.hashCode(), "Hashcode was zero!");
    }

    @Test
    void hashCodeWithEmptyAction()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("");
        assertEquals(0, event.hashCode(), "Hashcode for empty string action wasn't zero!");
    }

    @Test
    void hashCodeForWildcardAction()
    {
        ActionExecutedEvent event = new ActionExecutedEvent();
        assertEquals(0, event.hashCode(), "Hashcode for wildcard action wasn't zero!");
    }
}
