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
import java.util.Date;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.text.XWikiToStringBuilder;

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
     * True if the end date should be included.
     * 
     * @since 12.6.1
     * @since 12.7RC1
     */
    public boolean endDateIncluded = true;

    /**
     * Don't get notification that have been triggered before the following date.
     */
    public Date fromDate;

    /**
     * Display only unread notifications.
     */
    public Boolean onlyUnread;

    /**
     * List of event IDs not to return.
     */
    public Collection<String> blackList = new ArrayList<>();

    /**
     * List of preferences to apply.
     */
    public Collection<NotificationPreference> preferences = new ArrayList<>();

    /**
     * List of filter preferences to apply.
     */
    public Collection<NotificationFilterPreference> filterPreferences = new ArrayList<>();

    /**
     * List of filters to apply.
     */
    public Collection<NotificationFilter> filters = new ArrayList<>();

    /**
     * The output target of the notification for composite events computation with the
     * {@link org.xwiki.notifications.GroupingEventManager}.
     * @since 15.5RC1
     */
    public String groupingEventTarget = "alert";

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        NotificationParameters that = (NotificationParameters) o;

        return new EqualsBuilder()
            .append(expectedCount, that.expectedCount)
            .append(user, that.user)
            .append(format, that.format)
            .append(endDate, that.endDate)
            .append(endDateIncluded, that.endDateIncluded)
            .append(fromDate, that.fromDate)
            .append(onlyUnread, that.onlyUnread)
            .append(blackList, that.blackList)
            .append(preferences, that.preferences)
            .append(filterPreferences, that.filterPreferences)
            .append(filters, that.filters)
            .append(groupingEventTarget, that.groupingEventTarget)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(user)
            .append(format)
            .append(expectedCount)
            .append(endDate)
            .append(endDateIncluded)
            .append(fromDate)
            .append(onlyUnread)
            .append(blackList)
            .append(preferences)
            .append(filterPreferences)
            .append(filters)
            .append(groupingEventTarget)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("user", user)
            .append("format", format)
            .append("expectedCount", expectedCount)
            .append("endDate", endDate)
            .append("endDateIncluded", endDateIncluded)
            .append("fromDate", fromDate)
            .append("onlyUnread", onlyUnread)
            .append("blackList", blackList)
            .append("preferences", preferences)
            .append("filterPreferences", filterPreferences)
            .append("filters", filters)
            .append("notificationGroupingStrategyHint", groupingEventTarget)
            .toString();
    }
}
