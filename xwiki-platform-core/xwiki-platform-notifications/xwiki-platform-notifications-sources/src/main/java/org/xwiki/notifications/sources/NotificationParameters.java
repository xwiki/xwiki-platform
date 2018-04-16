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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.preferences.NotificationPreference;

/**
 * Parameters to fill to retrieve notifications using {@link ParametrizedNotificationManager}.
 *
 * @version $Id$
 * @since 10.4
 */
public class NotificationParameters
{
    /**
     * The user for who we should get the notifications.
     */
    public DocumentReference user;

    /**
     * The format of the notifications to get.
     */
    public NotificationFormat format;

    /**
     * The maximum number of notifications to return.
     */
    public int expectedCount;

    /**
     * Don't get notifications that have been triggered after the following date.
     */
    public Date endDate;

    /**
     * Don't get notification that have been triggered before the following date.
     */
    public Date fromDate;

    /**
     * List of event IDs not to return.
     */
    public Collection<String> blackList = new ArrayList<>();

    /**
     * List of preferences to apply.
     */
    public Collection<NotificationPreference> preferences = Collections.emptyList();

    /**
     * List of filter preferences to apply.
     */
    public Collection<NotificationFilterPreference> filterPreferences = Collections.emptyList();

    /**
     * List of filters to apply.
     */
    public Collection<NotificationFilter> filters = Collections.emptyList();
}
