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
package org.xwiki.notifications.notifiers.internal;

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
    private static final UserNotificationEvent EVENT =
        new UserNotificationEvent(new DocumentReference("wiki", "space", "page"), NotificationFormat.ALERT);

    @Test
    public void matchesAll()
    {
        assertTrue((new UserNotificationEvent()).matches(EVENT));
    }

    @Test
    public void matchesFormat()
    {
        assertTrue(new UserNotificationEvent(NotificationFormat.ALERT).matches(EVENT));
        assertFalse(new UserNotificationEvent(NotificationFormat.EMAIL).matches(EVENT));
    }

    @Test
    public void matchesReference()
    {
        assertTrue(new UserNotificationEvent(new DocumentReference("wiki", "space", "page")).matches(EVENT));
        assertFalse(new UserNotificationEvent(new DocumentReference("wiki", "space", "otherpage")).matches(EVENT));
    }

    @Test
    public void matchesReferenceAndFormat()
    {
        assertTrue(new UserNotificationEvent(new DocumentReference("wiki", "space", "page"), NotificationFormat.ALERT)
            .matches(EVENT));

        assertFalse(
            new UserNotificationEvent(new DocumentReference("wiki", "space", "otherpage"), NotificationFormat.ALERT)
                .matches(EVENT));
        assertFalse(new UserNotificationEvent(new DocumentReference("wiki", "space", "page"), NotificationFormat.EMAIL)
            .matches(EVENT));
    }
}
