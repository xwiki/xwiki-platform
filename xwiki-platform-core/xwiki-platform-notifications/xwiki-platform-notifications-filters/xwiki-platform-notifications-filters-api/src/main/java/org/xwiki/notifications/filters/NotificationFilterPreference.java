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
package org.xwiki.notifications.filters;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.xwiki.notifications.NotificationFormat;

/**
 * Define the preference of a notification filter.
 * A notification filter preference represents a set of parameters that can be passed to a filter in order to
 * customize it.
 *
 * Example :
 * Let’s say that we have defined a filter based on the wiki users : in order to work, this filter needs to know
 * which wiki users he has to filter, and for which types of events he should be applied.
 *
 * Those wiki users and those event types are like parameters to the filter, a {@link NotificationFilterPreference}
 * is nothing else than a combination of those parameters.
 *
 * @version $Id$
 * @since 9.7RC1
 */
public interface NotificationFilterPreference
{
    /**
     * Get a list of the properties that corresponds to the given {@link NotificationFilterProperty}.
     * If the property is not used by the current preference, return an empty set.
     *
     * @param property a corresponding {@link NotificationFilterProperty}
     * @return a set of properties.
     */
    List<String> getProperties(NotificationFilterProperty property);

    /**
     * @return the name of the filter preference.
     */
    String getFilterPreferenceName();

    /**
     * @return the name of the filter corresponding to this preference.
     */
    String getFilterName();

    /**
     * @return the name of the {@link NotificationFilterPreferenceProvider} associated with this preference.
     */
    String getProviderHint();

    /**
     * @return true if the current notification preference is enabled.
     */
    boolean isEnabled();

    /**
     * A filter preference can either be active or passive. It the preference is active, then it should force the
     * retrieval of notifications when used in conjunction with a {@link NotificationFilter}.
     *
     * On the other hand, a passive (non-active) notification filter should not automatically trigger the retrieval of
     * notifications.
     *
     * @return true if the filter preference is active.
     */
    boolean isActive();

    /**
     * @return the type of the filter described by this preference.
     */
    NotificationFilterType getFilterType();

    /**
     * @return a set of {@link NotificationFormat} for which the filter should be applied.
     */
    Set<NotificationFormat> getFilterFormats();

    /**
     * @return the date from which the filter preference is enabled.
     * @since 10.5RC1
     * @since 10.4
     * @since 9.11.5
     */
    default Date getStartingDate() {
        return null;
    };
}
