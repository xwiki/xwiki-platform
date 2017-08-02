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

import java.util.Date;

import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.NotificationProperty;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.internal.UserProfileNotificationPreferenceProvider;
import org.xwiki.notifications.preferences.internal.TargetableNotificationEventTypePreference;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link TargetableNotificationEventTypePreference}.
 *
 * @since 9.6RC1
 * @version $Id$
 */
public class TargetableNotificationEventTypePreferenceTest
{
    @Test
    public void testPublicGetters() throws Exception
    {
        Date testDate = new Date();
        DocumentReference target = new DocumentReference("xwiki", "space", "user");

        NotificationPreference testPreference = new TargetableNotificationEventTypePreference(
                "eventType", target, true, NotificationFormat.ALERT, testDate);

        assertEquals("eventType", testPreference.getProperties().get(NotificationProperty.EVENT_TYPE));

        assertEquals(NotificationFormat.ALERT, testPreference.getFormat());

        assertEquals(UserProfileNotificationPreferenceProvider.NAME, testPreference.getProviderHint());

        assertEquals(testDate, testPreference.getStartDate());

        assertEquals(target, ((TargetableNotificationEventTypePreference) testPreference).getTarget());
    }
}
