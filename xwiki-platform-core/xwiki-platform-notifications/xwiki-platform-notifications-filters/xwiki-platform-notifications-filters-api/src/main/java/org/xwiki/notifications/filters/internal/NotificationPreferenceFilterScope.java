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

import org.xwiki.model.reference.EntityReference;

/**
 * Represent a preferences that filter some event type to given scope (a wiki, a space, a page...).
 * This preference can be either incusive or exclusive compared to other filters.
 *
 * Example: only get "update" events when it concern pages inside the space "xwiki:Space1".
 *
 * This scopes are used either for custom filters defined by the user in the notification center, or by the
 * notification center watchlist.
 *
 * @version $Id$
 * @since 9.5RC1
 */
public class NotificationPreferenceFilterScope
{
    private String eventType;

    private String applicationId;

    private boolean isWatchList;

    private EntityReference scopeReference;

    private NotificationPreferenceScopeFilterType scopeFilterType;

    /**
     * Construct a NotificationPreferenceFilterScope.
     *
     * @param eventType name of the event type to refine
     * @param applicationId the id of the application to filter
     * @param isWatchList wether this filter should be used for the notifications watchlist or not
     * @param scopeReference reference of the scope
     * @param scopeFilterType the type of filter associated with the scope
     */
    public NotificationPreferenceFilterScope(String eventType, String applicationId, boolean isWatchList,
            EntityReference scopeReference, NotificationPreferenceScopeFilterType scopeFilterType)
    {
        this.eventType = eventType;
        this.applicationId = applicationId;
        this.isWatchList = isWatchList;
        this.scopeReference = scopeReference;
        this.scopeFilterType = scopeFilterType;
    }

    /**
     * @return the name of the event type to refine
     */
    public String getEventType()
    {
        return eventType;
    }

    /**
     * @return the ID of the related application
     */
    public String getApplicationId()
    {
        return applicationId;
    }

    /**
     * @return whether this scope should be used for watchlist filtering or not
     */
    public boolean isWatchList()
    {
        return isWatchList;
    }

    /**
     * @return the reference of the scope
     */
    public EntityReference getScopeReference()
    {
        return scopeReference;
    }

    /**
     * @return the type of filter associated to the scope
     */
    public NotificationPreferenceScopeFilterType getScopeFilterType()
    {
        return this.scopeFilterType;
    }
}
