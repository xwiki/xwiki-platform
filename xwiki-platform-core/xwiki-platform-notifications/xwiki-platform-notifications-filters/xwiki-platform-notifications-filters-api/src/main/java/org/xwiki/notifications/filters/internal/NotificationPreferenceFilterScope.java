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
    private List<String> eventTypes;

    private EntityReference scopeReference;

    private NotificationFilterType scopeFilterType;

    /**
     * Construct a NotificationPreferenceFilterScope.
     *
     * @param eventTypes names of the event types to refine
     * @param scopeReference reference of the scope
     * @param scopeFilterType the type of filter associated with the scope
     */
    public NotificationPreferenceFilterScope(List<String> eventTypes,
            EntityReference scopeReference, NotificationFilterType scopeFilterType)
    {
        this.eventTypes = eventTypes;
        this.scopeReference = scopeReference;
        this.scopeFilterType = scopeFilterType;
    }

    /**
     * @return the names of the event types to refine
     */
    public List<String> getEventTypes()
    {
        return eventTypes;
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
    public NotificationFilterType getScopeFilterType()
    {
        return this.scopeFilterType;
    }
}
