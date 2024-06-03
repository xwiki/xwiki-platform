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

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterScope;
import org.xwiki.notifications.filters.NotificationFilterType;

/**
 * Default implementation of {@link NotificationFilterPreference}.
 *
 * @since 10.8RC1
 * @since 9.11.8
 * @version $Id $
 */
public class DefaultNotificationFilterPreference implements NotificationFilterPreference
{
    private String id;

    private long internalId;

    private String owner;

    private String filterName;

    private boolean enabled;

    private NotificationFilterType filterType;

    private Set<NotificationFormat> formats = new HashSet<>();
    private NotificationFilterScope scope;
    private String entity;
    private Date startingDate;

    private Set<String> eventTypes = new HashSet<>();

    /**
     * Construct an empty DefaultNotificationFilterPreference.
     */
    public DefaultNotificationFilterPreference()
    {

    }

    /**
     * Construct a DefaultNotificationFilterPreference which is a copy of the given notificationFilterPreference.
     * @param notificationFilterPreference object to copy
     */
    public DefaultNotificationFilterPreference(NotificationFilterPreference notificationFilterPreference)
    {
        this(notificationFilterPreference, true);
    }

    /**
     * Construct a DefaultNotificationFilterPreference which is a copy of the given notificationFilterPreference.
     * @param notificationFilterPreference object to copy
     * @param keepId if {@code true} and the object to copy is of type {@link DefaultNotificationFilterPreference} then
     *              the {@link #internalId} and {@link #owner} are preserved.
     * @since 13.4RC1
     */
    public DefaultNotificationFilterPreference(NotificationFilterPreference notificationFilterPreference,
        boolean keepId)
    {
        if (keepId && notificationFilterPreference instanceof DefaultNotificationFilterPreference) {
            this.internalId = ((DefaultNotificationFilterPreference) notificationFilterPreference).internalId;
            this.owner = ((DefaultNotificationFilterPreference) notificationFilterPreference).owner;
        }

        this.id = notificationFilterPreference.getId();
        this.filterName = notificationFilterPreference.getFilterName();
        this.enabled = notificationFilterPreference.isEnabled();
        this.filterType = notificationFilterPreference.getFilterType();
        this.startingDate = notificationFilterPreference.getStartingDate();
        this.eventTypes = new HashSet<>(notificationFilterPreference.getEventTypes());
        this.scope = notificationFilterPreference.getScope();
        this.entity = notificationFilterPreference.getEntity();
        this.formats = new HashSet<>(notificationFilterPreference.getNotificationFormats());
    }

    /**
     * @param id the unique identifier to set
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * @return the internal id used to store the preference
     */
    public long getInternalId()
    {
        return internalId;
    }

    /**
     * @param internalId the internal id used to store the preference
     */
    public void setInternalId(long internalId)
    {
        this.internalId = internalId;
        this.id = String.format("%s%d", DB_ID_FILTER_PREFIX, internalId);
    }

    /**
     * @return the owner of the preference
     */
    public String getOwner()
    {
        return owner;
    }

    /**
     * @param owner the owner of the preference
     */
    public void setOwner(String owner)
    {
        this.owner = owner;
    }

    /**
     * @param filterName the name of the filter concerned by the preference
     */
    public void setFilterName(String filterName)
    {
        this.filterName = filterName;
    }

    /**
     * @param enabled if the preference is enabled or not
     */
    @Override
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * @param filterType the type of the filter described by this preference.
     */
    public void setFilterType(NotificationFilterType filterType)
    {
        this.filterType = filterType;
    }

    /**
     * @param startingDate the date from which the filter preference is enabled.
     */
    public void setStartingDate(Date startingDate)
    {
        this.startingDate = startingDate;
    }

    /**
     * @param eventTypes the event types concerned by the preference
     * @since 10.8RC1
     * @since 9.11.8
     */
    public void setEventTypes(Set<String> eventTypes)
    {
        this.eventTypes = eventTypes;
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public String getFilterName()
    {
        return filterName;
    }

    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    @Override
    public NotificationFilterType getFilterType()
    {
        return filterType;
    }

    @Override
    public Date getStartingDate()
    {
        return startingDate;
    }

    @Override
    public Set<String> getEventTypes()
    {
        return eventTypes;
    }


    @Override
    public Set<NotificationFormat> getNotificationFormats()
    {
        return formats;
    }

    /**
     * @param formats the formats that the filter will target
     * @since 16.5.0RC1
     */
    public void setNotificationFormats(Set<NotificationFormat> formats)
    {
        this.formats = formats;
    }

    @Override
    public NotificationFilterScope getScope()
    {
        return scope;
    }

    /**
     * @param scope the scope of the entity this filter is about
     * @since 16.5.0RC1
     */
    public void setScope(NotificationFilterScope scope)
    {
        this.scope = scope;
    }

    @Override
    public String getEntity()
    {
        return entity;
    }

    /**
     * @param entity the entity the filter is about
     * @since 16.5.0RC1
     */
    public void setEntity(String entity)
    {
        this.entity = entity;
    }

    @Override
    public boolean isFromWiki(String wikiId)
    {
        return this.entity.startsWith(wikiId);
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

        DefaultNotificationFilterPreference that = (DefaultNotificationFilterPreference) o;

        return new EqualsBuilder()
            .append(internalId, that.internalId)
            .append(enabled, that.enabled)
            .append(id, that.id)
            .append(owner, that.owner)
            .append(filterName, that.filterName)
            .append(filterType, that.filterType)
            .append(formats, that.formats)
            .append(scope, that.scope)
            .append(entity, that.entity)
            .append(startingDate, that.startingDate)
            .append(eventTypes, that.eventTypes)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 63)
            .append(id)
            .append(internalId)
            .append(owner)
            .append(filterName)
            .append(enabled)
            .append(filterType)
            .append(formats)
            .append(scope)
            .append(entity)
            .append(startingDate)
            .append(eventTypes)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append("id", id)
            .append("internalId", internalId)
            .append("owner", owner)
            .append("filterName", filterName)
            .append("enabled", enabled)
            .append("filterType", filterType)
            .append("formats", formats)
            .append("scope", scope)
            .append("entity", entity)
            .append("startingDate", startingDate)
            .append("eventTypes", eventTypes)
            .toString();
    }
}
