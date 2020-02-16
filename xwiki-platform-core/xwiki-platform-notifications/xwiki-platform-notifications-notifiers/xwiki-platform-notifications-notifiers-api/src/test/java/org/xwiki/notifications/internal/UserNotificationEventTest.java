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
package org.xwiki.notifications.internal;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.notifiers.internal.UserNotificationEvent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@ink UserNotificationEvent}.
 *
 * @version $Id$
 */
public class UserNotificationEventTest
{
    @Test
    void matchesWhenNoReference()
    {
        UserNotificationEvent event = new UserNotificationEvent();
        assertTrue(event.matches(new UserNotificationEvent()));
        assertTrue(event.matches(new UserNotificationEvent(new DocumentReference("wiki", "space", "page"))));
    }

    @Test
    void matchesWhenNotAUserNotificationEvent()
    {
        UserNotificationEvent event = new UserNotificationEvent();
        assertFalse(event.matches("whatever"));
    }

    @Test
    void matchesWhenReferenceSetButFormatEmpty()
    {
        UserNotificationEvent event = new UserNotificationEvent(new DocumentReference("wiki", "space", "page"));

        assertFalse(event.matches(new UserNotificationEvent()));
        assertTrue(event.matches(new UserNotificationEvent(new DocumentReference("wiki", "space", "page"))));
        assertFalse(event.matches(new UserNotificationEvent(new DocumentReference("wiki2", "space", "page"))));
    }

    @Test
    void matchesWhenFormatSetButReferenceEmpty()
    {
        UserNotificationEvent event = new UserNotificationEvent(NotificationFormat.ALERT);

        assertTrue(event.matches(new UserNotificationEvent()));
        assertTrue(event.matches(new UserNotificationEvent(NotificationFormat.ALERT)));
        assertTrue(event.matches(new UserNotificationEvent(
            new DocumentReference("wiki2", "space", "page"), NotificationFormat.ALERT)));
    }

    @Test
    void matchesWhenReferenceAndFormatSet()
    {
        UserNotificationEvent event = new UserNotificationEvent(
            new DocumentReference("wiki", "space", "page"), NotificationFormat.ALERT);

        assertFalse(event.matches(new UserNotificationEvent(new DocumentReference("wiki", "space", "page"))));
        assertFalse(event.matches(new UserNotificationEvent(
            new DocumentReference("wiki", "space", "page"), NotificationFormat.EMAIL)));
        assertTrue(event.matches(new UserNotificationEvent(
            new DocumentReference("wiki", "space", "page"), NotificationFormat.ALERT)));
    }
}
