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
package org.xwiki.notifications.filters.internal;

import java.util.List;
import java.util.Set;

import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.NotificationFilterType;

/**
 * @version $Id$
 * @since
 */
public class NotificationFilterPreferenceWrapper implements NotificationFilterPreference
{
    private NotificationFilterPreference original;

    private boolean enabled;

    public NotificationFilterPreferenceWrapper(NotificationFilterPreference original, boolean enabled)
    {
        this.original = original;
        this.enabled = enabled;
    }

    @Override
    public List<String> getProperties(NotificationFilterProperty property)
    {
        return original.getProperties(property);
    }

    @Override
    public String getFilterPreferenceName()
    {
        return original.getFilterPreferenceName();
    }

    @Override
    public String getFilterName()
    {
        return original.getFilterName();
    }

    @Override
    public String getProviderHint()
    {
        return original.getProviderHint();
    }

    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    @Override
    public boolean isActive()
    {
        return original.isActive();
    }

    @Override
    public NotificationFilterType getFilterType()
    {
        return original.getFilterType();
    }

    @Override
    public Set<NotificationFormat> getFilterFormats()
    {
        return original.getFilterFormats();
    }
}
