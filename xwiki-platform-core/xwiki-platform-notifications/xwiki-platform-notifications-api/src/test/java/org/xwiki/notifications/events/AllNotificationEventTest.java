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
package org.xwiki.notifications.events;

import java.util.List;

import org.junit.Test;
import org.xwiki.bridge.event.DocumentDeletingEvent;
import org.xwiki.observation.event.Event;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @version $Id$
 */
public class AllNotificationEventTest
{
    @Test
    public void matches() throws Exception
    {
        Event event = new DocumentDeletingEvent();

        assertFalse(AllNotificationEvent.ALL_NOTIFICATION_EVENT.matches(event));

        Event notificationEvent = new NotificationEvent()
        {
            @Override
            public List<String> getAudience()
            {
                return null;
            }

            @Override
            public boolean matches(Object o)
            {
                return false;
            }
        };

        assertTrue(AllNotificationEvent.ALL_NOTIFICATION_EVENT.matches(notificationEvent));
    }
}
