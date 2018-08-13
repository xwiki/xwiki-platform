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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.mockito.internal.util.collections.Sets;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.NotificationFilterType;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link DefaultNotificationFilterPreference}.
 *
 * @version $Id$
 * @since 9.8RC1
 */
public class DefaultNotificationFilterPreferenceTest
{
    @Test
    public void defaultNotificationFilterPreference() throws Exception
    {
        String filterPreferenceName = "fp1";
        String filterName = "f1";
        boolean isEnabled = true;
        boolean isActive = true;
        NotificationFilterType filterType = NotificationFilterType.INCLUSIVE;
        Set<NotificationFormat> notificationFormats = Sets.newSet(NotificationFormat.ALERT, NotificationFormat.EMAIL);
        Map<NotificationFilterProperty, List<String>> propertiesMap = new HashMap<>();

        propertiesMap.put(NotificationFilterProperty.APPLICATION, Arrays.asList("a1", "a2"));

        DefaultNotificationFilterPreference preference = new DefaultNotificationFilterPreference(filterPreferenceName);
        preference.setFilterName(filterName);
        preference.setEnabled(isEnabled);
        preference.setActive(isActive);
        preference.setFilterType(filterType);
        preference.setNotificationFormats(notificationFormats);
        preference.setProperties(propertiesMap);

        assertEquals(filterPreferenceName, preference.getFilterPreferenceName());
        assertEquals(filterName, preference.getFilterName());
        assertEquals(isEnabled, preference.isEnabled());
        assertEquals(isActive, preference.isActive());
        assertEquals(filterType, preference.getFilterType());
        assertEquals(notificationFormats, preference.getFilterFormats());
        assertEquals(propertiesMap.get(NotificationFilterProperty.APPLICATION),
                preference.getProperties(NotificationFilterProperty.APPLICATION));
    }
}
