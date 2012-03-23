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
 * Tests for the {@link ActionExecutedEvent} event type.
 * 
 * @version $Id$
 */
public class ActionExecutedEventTest
{
    // Tests for constructors

    @Test
    public void testDefaultConstructor()
    {
        ActionExecutedEvent event = new ActionExecutedEvent();
        Assert.assertNull("A default action was used!", event.getActionName());
    }

    @Test
    public void testConstructorWithActionName()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        Assert.assertEquals("Action name was lost!", "something", event.getActionName());
    }

    // Tests for matches(Object)

    @Test
    public void testMatchesSameObject()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        Assert.assertTrue("Same object wasn't matched!", event.matches(event));
    }

    @Test
    public void testMatchesSameAction()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        Assert.assertTrue("Same action wasn't matched!", event.matches(new ActionExecutedEvent("something")));
    }

    @Test
    public void testDoesntMatchNull()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        Assert.assertFalse("null was matched!", event.matches(null));
    }

    @Test
    public void testDoesntMatchWildcardAction()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        Assert.assertFalse("Wildcard action was matched!", event.matches(new ActionExecutedEvent()));
    }

    @Test
    public void testDoesntMatchDifferentAction()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        Assert.assertFalse("A different action was matched!", event.matches(new ActionExecutedEvent("else")));
    }

    @Test
    public void testDoesntMatchDifferentCaseAction()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        Assert.assertFalse("Action matching was case insensitive!",
            event.matches(new ActionExecutedEvent("SomeThing")));
    }

    @Test
    public void testDoesntMatchDifferentTypeOfAction()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        Assert.assertFalse("A different type of action was matched!",
            event.matches(new ActionExecutingEvent("something")));
    }

    @Test
    public void testWildcardActionMatchesAll()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        Assert.assertTrue("Wildcard action didn't match!", new ActionExecutedEvent().matches(event));
        Assert.assertTrue("Wildcard action didn't match another wildcard action!",
            new ActionExecutedEvent().matches(new ActionExecutedEvent()));
    }

    @Test
    public void testWildcardActionDoesnMatchNull()
    {
        Assert.assertFalse("Wildcard action matched null!", new ActionExecutedEvent().matches(null));
    }

    @Test
    public void testEmptyActionDoesntMatch()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        Assert.assertFalse("Empty action behaves as wildcard!", new ActionExecutedEvent("").matches(event));
    }

    // Tests for equals(Object)

    @Test
    public void testEqualsSameObject()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        Assert.assertTrue("Same object wasn't equal!", event.equals(event));
    }

    @Test
    public void testEqualsSameAction()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        Assert.assertTrue("Same action wasn't equal!", event.equals(new ActionExecutedEvent("something")));
    }

    @Test
    public void testEqualsWithNull()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        Assert.assertFalse("null was equal!", event.equals(null));
    }

    @Test
    public void testDoesntEqualWildcardAction()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        Assert.assertFalse("Wildcard action was equal!", event.equals(new ActionExecutedEvent()));
    }

    @Test
    public void testDoesntEqualDifferentAction()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        Assert.assertFalse("A different action was equal!", event.equals(new ActionExecutedEvent("else")));
    }

    @Test
    public void testDoesntEqualDifferentCaseAction()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        Assert.assertFalse("Action equals comparison was case insensitive!",
            event.equals(new ActionExecutedEvent("SomeThing")));
    }

    @Test
    public void testDoesntEqualDifferentTypeOfAction()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        Assert.assertFalse("Same object isn't matched!", event.equals(new ActionExecutingEvent("something")));
    }

    @Test
    public void testWildcardActionDoesntEqualOtherActions()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        Assert.assertFalse("Wildcard action equals another action!", new ActionExecutedEvent().equals(event));
    }

    @Test
    public void testWildcardActionDoesntEqualEmptyAction()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("");
        Assert.assertFalse("Wildcard action equals another action!", new ActionExecutedEvent().equals(event));
    }

    @Test
    public void testWildcardActionEqualsWildcardAction()
    {
        Assert.assertTrue("Wildcard action isn't equal to another wildcard action",
            new ActionExecutedEvent().equals(new ActionExecutedEvent()));
    }

    @Test
    public void testWildcardActionDoesntEqualNull()
    {
        Assert.assertFalse("Wildcard action equals null!", new ActionExecutedEvent().equals(null));
    }

    // Tests for hashCode()

    @Test
    public void testHashCode()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("something");
        Assert.assertTrue("Hashcode was zero!", event.hashCode() != 0);
    }

    @Test
    public void testHashCodeWithEmptyAction()
    {
        ActionExecutedEvent event = new ActionExecutedEvent("");
        Assert.assertTrue("Hashcode for empty string action wasn't zero!", event.hashCode() == 0);
    }

    @Test
    public void testHashCodeForWildcardAction()
    {
        ActionExecutedEvent event = new ActionExecutedEvent();
        Assert.assertTrue("Hashcode for wildcard action wasn't zero!", event.hashCode() == 0);
    }
}
