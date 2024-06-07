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

/**
 * Deprecated methods of {@link NotificationFilterPreference}.
 *
 * @version $Id$
 * @since 16.5.0RC1
 */
public interface CompatibilityNotificationFilterPreference
{
    /**
     * @return the name of the provider hint associated with this preference.
     * @deprecated this information is now useless with support of a single location for storing preferences
     */
    @Deprecated(since = "16.5.0RC1")
    default String getProviderHint()
    {
        return "";
    }

    /**
     * A filter preference can either be active or passive. It the preference is active, then it should force the
     * retrieval of notifications when used in conjunction with a {@link NotificationFilter}.
     *
     * On the other hand, a passive (non-active) notification filter should not automatically trigger the retrieval of
     * notifications.
     *
     * @return true if the filter preference is active.
     * @deprecated this behaviour doesn't make sense anymore with usage of prefiltering as there's no trigger for
     * retrieving the notifications nowadays.
     */
    @Deprecated(since = "16.5.0RC1")
    default boolean isActive()
    {
        return true;
    }
}
