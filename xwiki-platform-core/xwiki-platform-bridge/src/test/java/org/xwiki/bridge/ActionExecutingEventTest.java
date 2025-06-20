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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@link ActionExecutingEvent} event type.
 * 
 * @version $Id$
 */
class ActionExecutingEventTest
{
    // Tests for constructors

    @Test
    void defaultConstructor()
    {
        ActionExecutingEvent event = new ActionExecutingEvent();
        assertNull(event.getActionName(), "A default action was used!");
        assertFalse(event.isCanceled(), "Event was created canceled!");
        assertNull(event.getReason(), "A cancel reason was initially set!");
    }

    @Test
    void constructorWithActionName()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        assertEquals("something", event.getActionName(), "Action name was lost!");
        assertFalse(event.isCanceled(), "Event was created canceled!");
        assertNull(event.getReason(), "A cancel reason was initially set!");
    }

    // Tests for cancel()

    @Test
    void cancel()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        assertFalse(event.isCanceled(), "Event was created canceled!");
        event.cancel();
        assertTrue(event.isCanceled(), "Event wasn't canceled when requested!");
    }

    @Test
    void cancelWithReason()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        event.cancel("Testing reason");
        assertEquals("Testing reason", event.getReason(), "Cancelling reason was lost!");
    }

    // Tests for matches(Object)

    @Test
    void matchesSameObject()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        assertTrue(event.matches(event), "Same object wasn't matched!");
    }

    @Test
    void matchesSameAction()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        assertTrue(event.matches(new ActionExecutingEvent("something")), "Same action wasn't matched!");
    }

    @Test
    void doesntMatchNull()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        assertFalse(event.matches(null), "null was matched!");
    }

    @Test
    void doesntMatchWildcardAction()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        assertFalse(event.matches(new ActionExecutingEvent()), "Wildcard action was matched!");
    }

    @Test
    void doesntMatchDifferentAction()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        assertFalse(event.matches(new ActionExecutingEvent("else")), "A different action was matched!");
    }

    @Test
    void doesntMatchDifferentCaseAction()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        assertFalse(event.matches(new ActionExecutingEvent("SomeThing")), "Action matching was case insensitive!");
    }

    @Test
    void doesntMatchDifferentTypeOfAction()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        assertFalse(event.matches(new ActionExecutedEvent("something")), "A different type of action was matched!");
    }

    @Test
    void wildcardActionMatchesAll()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        assertTrue(new ActionExecutingEvent().matches(event), "Wildcard action didn't match!");
        assertTrue(new ActionExecutingEvent().matches(new ActionExecutingEvent()),
            "Wildcard action didn't match another wildcard action!");
    }

    @Test
    void wildcardActionDoesnMatchNull()
    {
        assertFalse(new ActionExecutingEvent().matches(null), "Wildcard action matched null!");
    }

    @Test
    void emptyActionDoesntMatch()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        assertFalse(new ActionExecutingEvent("").matches(event), "Empty action behaves as wildcard!");
    }

    // Tests for equals(Object)

    @Test
    void equalsSameObject()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        assertTrue(event.equals(event), "Same object wasn't equal!");
    }

    @Test
    void equalsSameAction()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        assertTrue(event.equals(new ActionExecutingEvent("something")), "Same action wasn't equal!");
    }

    @Test
    void equalsWithNull()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        assertFalse(event.equals(null), "null was equal!");
    }

    @Test
    void doesntEqualWildcardAction()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        assertFalse(event.equals(new ActionExecutingEvent()), "Wildcard action was equal!");
    }

    @Test
    void doesntEqualDifferentAction()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        assertFalse(event.equals(new ActionExecutingEvent("else")), "A different action was equal!");
    }

    @Test
    void doesntEqualDifferentCaseAction()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        assertFalse(event.equals(new ActionExecutingEvent("SomeThing")),
            "Action equals comparison was case insensitive!");
    }

    @Test
    void doesntEqualDifferentTypeOfAction()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        assertFalse(event.equals(new ActionExecutedEvent("something")), "Same object isn't matched!");
    }

    @Test
    void wildcardActionDoesntEqualOtherActions()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        assertFalse(new ActionExecutingEvent().equals(event), "Wildcard action equals another action!");
    }

    @Test
    void wildcardActionDoesntEqualEmptyAction()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("");
        assertFalse(new ActionExecutingEvent().equals(event), "Wildcard action equals another action!");
    }

    @Test
    void wildcardActionEqualsWildcardAction()
    {
        assertTrue(new ActionExecutingEvent().equals(new ActionExecutingEvent()),
            "Wildcard action isn't equal to another wildcard action");
    }

    @Test
    void wildcardActionDoesntEqualNull()
    {
        assertFalse(new ActionExecutingEvent().equals(null), "Wildcard action equals null!");
    }

    // Tests for hashCode()

    @Test
    void verifyHashCode()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        assertTrue(event.hashCode() != 0, "Hashcode was zero!");
    }

    @Test
    void hashCodeWithEmptyAction()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("");
        assertTrue(event.hashCode() == 0, "Hashcode for empty string action wasn't zero!");
    }

    @Test
    void hashCodeForWildcardAction()
    {
        ActionExecutingEvent event = new ActionExecutingEvent();
        assertTrue(event.hashCode() == 0, "Hashcode for wildcard action wasn't zero!");
    }
}
