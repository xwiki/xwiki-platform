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

import junit.framework.Assert;

import org.junit.Test;
import org.xwiki.bridge.event.ActionExecutedEvent;
import org.xwiki.bridge.event.ActionExecutingEvent;

/**
 * Tests for the {@link ActionExecutingEvent} event type.
 * 
 * @version $Id$
 */
public class ActionExecutingEventTest
{
    // Tests for constructors

    @Test
    public void testDefaultConstructor()
    {
        ActionExecutingEvent event = new ActionExecutingEvent();
        Assert.assertNull("A default action was used!", event.getActionName());
        Assert.assertFalse("Event was created canceled!", event.isCanceled());
        Assert.assertNull("A cancel reason was initially set!", event.getReason());
    }

    @Test
    public void testConstructorWithActionName()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        Assert.assertEquals("Action name was lost!", "something", event.getActionName());
        Assert.assertFalse("Event was created canceled!", event.isCanceled());
        Assert.assertNull("A cancel reason was initially set!", event.getReason());
    }

    // Tests for cancel()

    @Test
    public void testCancel()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        Assert.assertFalse("Event was created canceled!", event.isCanceled());
        event.cancel();
        Assert.assertTrue("Event wasn't canceled when requested!", event.isCanceled());
    }

    @Test
    public void testCancelWithReason()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        event.cancel("Testing reason");
        Assert.assertEquals("Cancelling reason was lost!", "Testing reason", event.getReason());
    }

    // Tests for matches(Object)

    @Test
    public void testMatchesSameObject()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        Assert.assertTrue("Same object wasn't matched!", event.matches(event));
    }

    @Test
    public void testMatchesSameAction()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        Assert.assertTrue("Same action wasn't matched!", event.matches(new ActionExecutingEvent("something")));
    }

    @Test
    public void testDoesntMatchNull()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        Assert.assertFalse("null was matched!", event.matches(null));
    }

    @Test
    public void testDoesntMatchWildcardAction()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        Assert.assertFalse("Wildcard action was matched!", event.matches(new ActionExecutingEvent()));
    }

    @Test
    public void testDoesntMatchDifferentAction()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        Assert.assertFalse("A different action was matched!", event.matches(new ActionExecutingEvent("else")));
    }

    @Test
    public void testDoesntMatchDifferentCaseAction()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        Assert.assertFalse("Action matching was case insensitive!",
            event.matches(new ActionExecutingEvent("SomeThing")));
    }

    @Test
    public void testDoesntMatchDifferentTypeOfAction()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        Assert.assertFalse("A different type of action was matched!",
            event.matches(new ActionExecutedEvent("something")));
    }

    @Test
    public void testWildcardActionMatchesAll()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        Assert.assertTrue("Wildcard action didn't match!", new ActionExecutingEvent().matches(event));
        Assert.assertTrue("Wildcard action didn't match another wildcard action!",
            new ActionExecutingEvent().matches(new ActionExecutingEvent()));
    }

    @Test
    public void testWildcardActionDoesnMatchNull()
    {
        Assert.assertFalse("Wildcard action matched null!", new ActionExecutingEvent().matches(null));
    }

    @Test
    public void testEmptyActionDoesntMatch()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        Assert.assertFalse("Empty action behaves as wildcard!", new ActionExecutingEvent("").matches(event));
    }

    // Tests for equals(Object)

    @Test
    public void testEqualsSameObject()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        Assert.assertTrue("Same object wasn't equal!", event.equals(event));
    }

    @Test
    public void testEqualsSameAction()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        Assert.assertTrue("Same action wasn't equal!", event.equals(new ActionExecutingEvent("something")));
    }

    @Test
    public void testEqualsWithNull()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        Assert.assertFalse("null was equal!", event.equals(null));
    }

    @Test
    public void testDoesntEqualWildcardAction()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        Assert.assertFalse("Wildcard action was equal!", event.equals(new ActionExecutingEvent()));
    }

    @Test
    public void testDoesntEqualDifferentAction()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        Assert.assertFalse("A different action was equal!", event.equals(new ActionExecutingEvent("else")));
    }

    @Test
    public void testDoesntEqualDifferentCaseAction()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        Assert.assertFalse("Action equals comparison was case insensitive!",
            event.equals(new ActionExecutingEvent("SomeThing")));
    }

    @Test
    public void testDoesntEqualDifferentTypeOfAction()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        Assert.assertFalse("Same object isn't matched!", event.equals(new ActionExecutedEvent("something")));
    }

    @Test
    public void testWildcardActionDoesntEqualOtherActions()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        Assert.assertFalse("Wildcard action equals another action!", new ActionExecutingEvent().equals(event));
    }

    @Test
    public void testWildcardActionDoesntEqualEmptyAction()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("");
        Assert.assertFalse("Wildcard action equals another action!", new ActionExecutingEvent().equals(event));
    }

    @Test
    public void testWildcardActionEqualsWildcardAction()
    {
        Assert.assertTrue("Wildcard action isn't equal to another wildcard action",
            new ActionExecutingEvent().equals(new ActionExecutingEvent()));
    }

    @Test
    public void testWildcardActionDoesntEqualNull()
    {
        Assert.assertFalse("Wildcard action equals null!", new ActionExecutingEvent().equals(null));
    }

    // Tests for hashCode()

    @Test
    public void testHashCode()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("something");
        Assert.assertTrue("Hashcode was zero!", event.hashCode() != 0);
    }

    @Test
    public void testHashCodeWithEmptyAction()
    {
        ActionExecutingEvent event = new ActionExecutingEvent("");
        Assert.assertTrue("Hashcode for empty string action wasn't zero!", event.hashCode() == 0);
    }

    @Test
    public void testHashCodeForWildcardAction()
    {
        ActionExecutingEvent event = new ActionExecutingEvent();
        Assert.assertTrue("Hashcode for wildcard action wasn't zero!", event.hashCode() == 0);
    }
}
