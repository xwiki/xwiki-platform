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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.text.XWikiToStringBuilder;

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
        if (StringUtils.isNotBlank(filterPreference.getPageOnly())) {
            scopeReference = entityReferenceResolver.resolve(filterPreference.getPageOnly(), EntityType.DOCUMENT);
        } else if (StringUtils.isNotBlank(filterPreference.getPage())) {
            scopeReference = entityReferenceResolver.resolve(filterPreference.getPage(), EntityType.SPACE);
        } else if (StringUtils.isNotBlank(filterPreference.getWiki())) {
            scopeReference = entityReferenceResolver.resolve(filterPreference.getWiki(), EntityType.WIKI);
        }
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
    public String getId()
    {
        return filterPreference.getId();
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
    public boolean isActive()
    {
        return true;
    }

    @Override
    public NotificationFilterType getFilterType()
    {
        return filterPreference.getFilterType();
    }

    @Override
    public Set<NotificationFormat> getNotificationFormats()
    {
        return filterPreference.getNotificationFormats();
    }

    @Override
    public Date getStartingDate()
    {
        return filterPreference.getStartingDate();
    }

    @Override
    public Set<String> getEventTypes()
    {
        return filterPreference.getEventTypes();
    }

    @Override
    public String getUser()
    {
        return filterPreference.getUser();
    }

    @Override
    public String getPageOnly()
    {
        return filterPreference.getPageOnly();
    }

    @Override
    public String getPage()
    {
        return filterPreference.getPage();
    }

    @Override
    public String getWiki()
    {
        return filterPreference.getWiki();
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        this.filterPreference.setEnabled(enabled);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ScopeNotificationFilterPreference that = (ScopeNotificationFilterPreference) o;

        return new EqualsBuilder()
            .append(hasParent, that.hasParent)
            .append(filterPreference, that.filterPreference)
            .append(scopeReference, that.scopeReference)
            .append(children, that.children)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(filterPreference)
            .append(scopeReference)
            .append(hasParent)
            .append(children)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this).append("hasParent", hasParent)
            .append("filterPreference", filterPreference)
            .append("scopeReference", scopeReference)
            .append("children", children)
            .toString();
    }
}
