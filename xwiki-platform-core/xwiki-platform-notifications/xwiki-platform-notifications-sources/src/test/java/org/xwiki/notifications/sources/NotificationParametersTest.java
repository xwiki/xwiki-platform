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
package org.xwiki.notifications.sources;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.preferences.NotificationPreference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link NotificationParameters}.
 *
 * @version $Id$
 */
public class NotificationParametersTest
{
    @Test
    public void equals()
    {
        NotificationParameters parameters1 = new NotificationParameters();
        NotificationParameters parameters2 = new NotificationParameters();

        assertEquals(parameters1, parameters2);
        assertEquals(parameters1.hashCode(), parameters2.hashCode());

        NotificationFilterPreference filterPref1 = mock(NotificationFilterPreference.class);
        NotificationPreference pref1 = mock(NotificationPreference.class);
        NotificationFilter filter1 = mock(NotificationFilter.class);

        parameters1.user = new DocumentReference("xwiki", "XWiki", "Foo");
        parameters1.onlyUnread = true;
        parameters1.format = NotificationFormat.ALERT;
        parameters1.blackList = Arrays.asList("foo", "bar");
        parameters1.fromDate = new Date(4242);
        parameters1.endDate = new Date(4343);
        parameters1.expectedCount = 888;
        parameters1.filterPreferences = Collections.singletonList(filterPref1);
        parameters1.filters = Collections.singletonList(filter1);
        parameters1.preferences = Collections.singletonList(pref1);

        assertNotEquals(parameters1, parameters2);
        assertNotEquals(parameters1.hashCode(), parameters2.hashCode());

        parameters2.user = new DocumentReference("xwiki", "XWiki", "Foo");
        assertNotEquals(parameters1, parameters2);
        assertNotEquals(parameters1.hashCode(), parameters2.hashCode());

        parameters2.onlyUnread = true;
        assertNotEquals(parameters1, parameters2);
        assertNotEquals(parameters1.hashCode(), parameters2.hashCode());

        parameters2.format = NotificationFormat.ALERT;
        assertNotEquals(parameters1, parameters2);
        assertNotEquals(parameters1.hashCode(), parameters2.hashCode());

        parameters2.blackList = Arrays.asList("foo", "bar");
        assertNotEquals(parameters1, parameters2);
        assertNotEquals(parameters1.hashCode(), parameters2.hashCode());

        parameters2.fromDate = new Date(4242);
        assertNotEquals(parameters1, parameters2);
        assertNotEquals(parameters1.hashCode(), parameters2.hashCode());

        parameters2.endDate = new Date(4343);
        assertNotEquals(parameters1, parameters2);
        assertNotEquals(parameters1.hashCode(), parameters2.hashCode());

        parameters2.expectedCount = 888;
        assertNotEquals(parameters1, parameters2);
        assertNotEquals(parameters1.hashCode(), parameters2.hashCode());

        parameters2.filterPreferences = Collections.singletonList(filterPref1);
        assertNotEquals(parameters1, parameters2);
        assertNotEquals(parameters1.hashCode(), parameters2.hashCode());

        parameters2.filters = Collections.singletonList(filter1);
        assertNotEquals(parameters1, parameters2);
        assertNotEquals(parameters1.hashCode(), parameters2.hashCode());

        parameters2.preferences = Collections.singletonList(pref1);
        assertEquals(parameters1, parameters2);
        assertEquals(parameters1.hashCode(), parameters2.hashCode());
    }
}
