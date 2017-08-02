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
package org.xwiki.notifications.preferences.internal;

import java.util.Collections;
import java.util.Date;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.NotificationProperty;
import org.xwiki.notifications.preferences.TargetableNotificationPreference;

/**
 * Define a {@link org.xwiki.notifications.preferences.NotificationPreference} that is characterized by the
 * event type of a notification.
 *
 * @version $Id$
 * @since 9.7RC1
 */
public class TargetableNotificationEventTypePreference extends AbstractNotificationPreference implements
        TargetableNotificationPreference
{
    private DocumentReference target;

    /**
     * Constructs a new {@link TargetableNotificationEventTypePreference}.
     *
     * @param eventType the type of the event concerned by the preference
     * @param target the target of the preference
     * @param isEnabled is the preference enabled ?
     * @param format the format of the notification triggered by this preference
     * @param startDate the date from which notifications matching this preference should be retrieved
     */
    public TargetableNotificationEventTypePreference(String eventType, DocumentReference target,
            boolean isEnabled, NotificationFormat format, Date startDate) {
        super(isEnabled, format, startDate, UserProfileNotificationPreferenceProvider.NAME,
                Collections.singletonMap(NotificationProperty.EVENT_TYPE, eventType));
        this.target = target;
    }

    @Override
    public DocumentReference getTarget()
    {
        return target;
    }
}
