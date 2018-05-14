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
package org.xwiki.notifications.filters.internal.scope;

import java.util.ArrayList;
import java.util.Date;
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
 * @since 9.9RC1
 */
public class ScopeNotificationFilterPreference implements NotificationFilterPreference
{
    private NotificationFilterPreference filterPreference;

    private EntityReference scopeReference;

    private boolean hasParent;

    private List<ScopeNotificationFilterPreference> children = new ArrayList<>();

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
     * Construct a new ScopeNotificationFilterPreference.
     *
     * @param filterPreference the {@link NotificationFilterPreference} to wrap
     * @param scopeReference the reference of the location concerned by the scope notification filter
     */
    public ScopeNotificationFilterPreference(NotificationFilterPreference filterPreference,
            EntityReference scopeReference)
    {
        this.filterPreference = filterPreference;
        this.scopeReference = scopeReference;
    }

    /**
     * @param hasParent either the preference has a parent preference
     */
    public void setHasParent(boolean hasParent)
    {
        this.hasParent = hasParent;
    }

    /**
     * @return either the preference has a parent preference
     */
    public boolean hasParent()
    {
        return hasParent;
    }

    /**
     * @return the list of children preferences
     */
    public List<ScopeNotificationFilterPreference> getChildren()
    {
        return children;
    }

    /**
     * Add a child to the preference.
     * @param child a preference that is a child of the current preference
     */
    public void addChild(ScopeNotificationFilterPreference child)
    {
        children.add(child);
        child.setHasParent(true);
    }

    /**
     * @param other an other preference
     * @return either the current preference is a parent of the other preference
     */
    public boolean isParentOf(ScopeNotificationFilterPreference other)
    {
        // The aim is to generate a white list of locations that are located under a black listed location
        // Ex:   "wiki1:Space1" is blacklisted but:
        //     - "wiki1:Space1.Space2" is white listed
        //     - "wiki1:Space1.Space3" is white listed too
        //
        // So a filter could be the parent of an other only if the other is an inclusive filter and if the current
        // is a exclusive filter
        return getFilterType() == NotificationFilterType.EXCLUSIVE
                && other.getFilterType() == NotificationFilterType.INCLUSIVE
                && other.getScopeReference().hasParent(this.getScopeReference());
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
    public String getProviderHint()
    {
        return filterPreference.getProviderHint();
    }

    @Override
    public boolean isEnabled()
    {
        return filterPreference.isEnabled();
    }

    @Override
    public boolean isActive()
    {
        return filterPreference.isActive();
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
    public Date getStartingDate()
    {
        return filterPreference.getStartingDate();
    }

    @Override
    public List<String> getProperties(NotificationFilterProperty property)
    {
        return filterPreference.getProperties(property);
    }
}
