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

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.NotificationFilterType;

/**
 * Represent a preferences that filter some event type to given scope (a wiki, a space, a page...).
 * This wraps a {@link NotificationFilterPreference} in order to extract its properties.
 *
 * @version $Id$
 * @since 9.5RC1
 */
public class ScopeNotificationFilterPreference implements NotificationFilterPreference
{
    private NotificationFilterPreference filterPreference;

    private EntityReference scopeReference;

    /**
     * Construct a new ScopeNotificationFilterPreference.
     *
     * @param filterPreference the {@link NotificationFilterPreference} to wrap
     * @param entityReferenceResolver the {@link EntityReferenceResolver} that should be used to resolve
     * the filter preference references.
     */
    public ScopeNotificationFilterPreference(NotificationFilterPreference filterPreference,
            EntityReferenceResolver<String> entityReferenceResolver)
    {
        this.filterPreference = filterPreference;

        // Determine which scope reference to return when needed
        if (!filterPreference.getProperties(NotificationFilterProperty.PAGE).isEmpty()) {
            scopeReference = entityReferenceResolver.resolve(
                    filterPreference.getProperties(NotificationFilterProperty.PAGE).get(0), EntityType.DOCUMENT);
        } else if (!filterPreference.getProperties(NotificationFilterProperty.SPACE).isEmpty()) {
            scopeReference = entityReferenceResolver.resolve(
                    filterPreference.getProperties(NotificationFilterProperty.SPACE).get(0), EntityType.SPACE);
        } else if (!filterPreference.getProperties(NotificationFilterProperty.WIKI).isEmpty()) {
            scopeReference = entityReferenceResolver.resolve(
                    filterPreference.getProperties(NotificationFilterProperty.WIKI).get(0), EntityType.WIKI);
        }
    }

    /**
     * @return the resolved reference of the current notification preference.
     */
    public EntityReference getScopeReference()
    {
        return scopeReference;
    }

    @Override
    public String getFilterPreferenceName()
    {
        return filterPreference.getFilterPreferenceName();
    }

    @Override
    public String getFilterName()
    {
        return filterPreference.getFilterName();
    }

    @Override
    public boolean isEnabled()
    {
        return filterPreference.isEnabled();
    }

    @Override
    public NotificationFilterType getFilterType()
    {
        return filterPreference.getFilterType();
    }

    @Override
    public Set<NotificationFormat> getFilterFormats()
    {
        return filterPreference.getFilterFormats();
    }

    @Override
    public List<String> getProperties(NotificationFilterProperty property)
    {
        return filterPreference.getProperties(property);
    }
}
