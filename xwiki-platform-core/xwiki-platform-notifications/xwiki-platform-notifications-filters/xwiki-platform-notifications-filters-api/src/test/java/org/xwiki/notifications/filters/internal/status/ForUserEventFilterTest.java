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
package org.xwiki.notifications.filters.internal.status;

import org.junit.jupiter.api.Test;
import org.xwiki.notifications.NotificationFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Tests for {@link ForUserEventFilter}.
 *
 * @version $Id$
 */
public class ForUserEventFilterTest
{
    @Test
    public void equals()
    {
        ForUserEventFilter forUserEventFilter1 = new ForUserEventFilter(NotificationFormat.ALERT, true);
        ForUserEventFilter forUserEventFilter2 = new ForUserEventFilter(NotificationFormat.ALERT, true);
        assertEquals(forUserEventFilter1, forUserEventFilter2);
        assertEquals(forUserEventFilter1.hashCode(), forUserEventFilter2.hashCode());

        forUserEventFilter2 = new ForUserEventFilter(NotificationFormat.EMAIL, true);
        assertNotEquals(forUserEventFilter1, forUserEventFilter2);
        assertNotEquals(forUserEventFilter1.hashCode(), forUserEventFilter2.hashCode());

        forUserEventFilter2 = new ForUserEventFilter(NotificationFormat.ALERT, false);
        assertNotEquals(forUserEventFilter1, forUserEventFilter2);
        assertNotEquals(forUserEventFilter1.hashCode(), forUserEventFilter2.hashCode());
    }
}
