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

import java.util.Date;
import java.util.Map;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.preferences.NotificationPreferenceCategory;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.notifications.preferences.TargetableNotificationPreference;
import org.xwiki.notifications.preferences.TargetableNotificationPreferenceBuilder;

/**
 * This is the default implementation of {@link TargetableNotificationPreferenceBuilder}. This implementation is not
 * thread-safe and should be instantiated each time a thread is using it.
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultTargetableNotificationPreferenceBuilder implements TargetableNotificationPreferenceBuilder
{
    private TargetablePreference preference;

    private final class TargetablePreference extends AbstractNotificationPreference
            implements TargetableNotificationPreference
    {
        /*
         * Note that we specifically don't want to check the equality with the target since this is used as part of
         * NotificationPreferencesManager#getAllPreferences to ensure that only the top priority preference is kept.
         * Check DefaultNotificationPreferenceManager#getAllPreferences for more information.
         */
        private EntityReference target;

        @Override
        public EntityReference getTarget()
        {
            return target;
        }
    }

    @Override
    public TargetableNotificationPreference build()
    {
        return preference;
    }

    @Override
    public TargetableNotificationPreferenceBuilder prepare()
    {
        preference = new TargetablePreference();
        preference.category = NotificationPreferenceCategory.DEFAULT;
        return this;
    }

    @Override
    public TargetableNotificationPreferenceBuilder setEnabled(boolean isEnabled)
    {
        preference.isNotificationEnabled = isEnabled;
        return this;
    }

    @Override
    public TargetableNotificationPreferenceBuilder setFormat(NotificationFormat format)
    {
        preference.format = format;
        return this;
    }

    @Override
    public TargetableNotificationPreferenceBuilder setProperties(Map<NotificationPreferenceProperty, Object> properties)
    {
        preference.properties = properties;
        return this;
    }

    @Override
    public TargetableNotificationPreferenceBuilder setProviderHint(String providerHint)
    {
        preference.providerHint = providerHint;
        return this;
    }

    @Override
    public TargetableNotificationPreferenceBuilder setStartDate(Date startDate)
    {
        preference.startDate = startDate;
        return this;
    }

    @Override
    public TargetableNotificationPreferenceBuilder setTarget(EntityReference target)
    {
        preference.target = target;
        return this;
    }

    @Override
    public TargetableNotificationPreferenceBuilder setCategory(NotificationPreferenceCategory category)
    {
        preference.category = category;
        return this;
    }
}
