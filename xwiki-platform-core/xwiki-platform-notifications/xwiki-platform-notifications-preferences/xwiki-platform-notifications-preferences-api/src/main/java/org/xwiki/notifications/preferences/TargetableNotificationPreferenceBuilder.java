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
package org.xwiki.notifications.preferences;

import java.util.Date;
import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.notifications.NotificationFormat;

/**
 * Allows to build new {@link TargetableNotificationPreference}.
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Role
public interface TargetableNotificationPreferenceBuilder
{
    /**
     * @return a freshly instanciated {@link TargetableNotificationPreference}
     */
    TargetableNotificationPreference build();

    /**
     * Prepare the builder for a new instance.
     * @return the current instance.
     */
    TargetableNotificationPreferenceBuilder prepare();

    /**
     * Define if the notification preference is enabled or not.
     *
     * @param isEnabled true if the preference should be enabled
     * @return the current instance.
     */
    TargetableNotificationPreferenceBuilder setEnabled(boolean isEnabled);

    /**
     * @param format the format of the preference
     * @return the current instance.
     */
    TargetableNotificationPreferenceBuilder setFormat(NotificationFormat format);

    /**
     * @param properties a map of the preference properties
     * @return the current instance.
     */
    TargetableNotificationPreferenceBuilder setProperties(Map<NotificationPreferenceProperty, Object> properties);

    /**
     * @param providerHint the hint of the provider of the preference
     * @return the current instance.
     */
    TargetableNotificationPreferenceBuilder setProviderHint(String providerHint);

    /**
     * @param startDate the start date of the preference
     * @return the current instance.
     */
    TargetableNotificationPreferenceBuilder setStartDate(Date startDate);

    /**
     * @param target the target of the preference
     * @since 9.11.4
     * @since 10.2RC2
     * @return the current instance.
     */
    TargetableNotificationPreferenceBuilder setTarget(EntityReference target);

    /**
     * @param category the category of the preference
     * @return the current instance.
     */
    TargetableNotificationPreferenceBuilder setCategory(NotificationPreferenceCategory category);
}
