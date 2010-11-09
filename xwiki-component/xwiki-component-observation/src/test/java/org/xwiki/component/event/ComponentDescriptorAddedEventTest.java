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
 *
 */
package org.xwiki.component.event;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.component.event.ComponentDescriptorAddedEvent;
import org.xwiki.observation.event.Event;

/**
 * Unit tests for {@link ComponentDescriptorAddedEvent}.
 * 
 * @version $Id$
 * @since 2.6RC2
 */
public class ComponentDescriptorAddedEventTest
{
    /**
     * Used in tests below.
     */
    private static class Dummy
    {
    }

    @Test
    public void testMatchesAllComponentRoles()
    {
        ComponentDescriptorAddedEvent event = new ComponentDescriptorAddedEvent();
        // Note: We use any class for the test but it's supposed to be a component role class.
        Assert.assertTrue(event.matches(new ComponentDescriptorAddedEvent(Dummy.class, "rolehint")));
    }

    @Test
    public void testMatchesWhenDifferentEvent()
    {
        ComponentDescriptorAddedEvent event = new ComponentDescriptorAddedEvent();
        Assert.assertFalse(event.matches(new Event()
        {
            public boolean matches(Object otherEvent)
            {
                return false;
            }
        }));
    }

    @Test
    public void testMatchesWhenSpecificRoleSpecified()
    {
        // Note: We use any class for the test but it's supposed to be a component role class.
        ComponentDescriptorAddedEvent event = new ComponentDescriptorAddedEvent(Dummy.class);
        Assert.assertTrue(event.matches(new ComponentDescriptorAddedEvent(Dummy.class, "rolehint")));
        // Use a different class so that it doesn't match
        Assert.assertFalse(event.matches(new ComponentDescriptorAddedEvent(ComponentDescriptorAddedEvent.class,
            "rolehint")));
    }
}
