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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.text.StringUtils;

/**
 * Default implementation of {@link NotificationFilterPreference}.
 *
 * @since 10.8RC1
 * @since 9.11.8
 * @version $Id $
 */
public class DefaultNotificationFilterPreference implements NotificationFilterPreference
{
    private static final String LIST_SEPARATOR = ",";

    private String id;

    private long internalId;

    private String owner;

    private String filterName;

    private String providerHint;

    private boolean enabled;

    private boolean active;

    private NotificationFilterType filterType;

    private Set<NotificationFormat> notificationFormats = new HashSet<>();

    private Date startingDate;

    private Set<String> eventTypes = new HashSet<>();

    private String user;

    private String pageOnly;

    private String page;

    private String wiki;

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
        if (notificationFilterPreference instanceof DefaultNotificationFilterPreference) {
            this.internalId = ((DefaultNotificationFilterPreference) notificationFilterPreference).internalId;
            this.owner = ((DefaultNotificationFilterPreference) notificationFilterPreference).owner;
        }

        this.id = notificationFilterPreference.getId();
        this.filterName = notificationFilterPreference.getFilterName();
        this.providerHint = notificationFilterPreference.getProviderHint();
        this.enabled = notificationFilterPreference.isEnabled();
        this.active = notificationFilterPreference.isActive();
        this.filterType = notificationFilterPreference.getFilterType();
        this.notificationFormats = notificationFilterPreference.getNotificationFormats();
        this.startingDate = notificationFilterPreference.getStartingDate();
        this.eventTypes = new HashSet<>(notificationFilterPreference.getEventTypes());
        this.user = notificationFilterPreference.getUser();
        this.pageOnly = notificationFilterPreference.getPageOnly();
        this.page = notificationFilterPreference.getPage();
        this.wiki = notificationFilterPreference.getWiki();

        this.setNotificationFormats(notificationFilterPreference.getNotificationFormats());
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
        this.id = String.format("NFP_%x", internalId);
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
     * @param providerHint the name of the provider that have built this preference
     */
    public void setProviderHint(String providerHint)
    {
        this.providerHint = providerHint;
    }

    /**
     * @param enabled if the preference is enabled or not
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * @param active if the preference is active or not
     */
    public void setActive(boolean active)
    {
        this.active = active;
    }

    /**
     * @param filterType the type of the filter described by this preference.
     */
    public void setFilterType(NotificationFilterType filterType)
    {
        this.filterType = filterType;
    }

    /**
     * @param filterFormats a set of {@link NotificationFormat} for which the filter should be applied.
     */
    public void setNotificationFormats(Set<NotificationFormat> filterFormats)
    {
        this.notificationFormats = filterFormats;
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

    /**
     * @param user the user concerned by the preference
     * @since 10.8RC1
     * @since 9.11.8
     */
    public void setUser(String user)
    {
        this.user = user;
    }

    /**
     * @param pageOnly the page concerned by the preference
     * @since 10.8RC1
     * @since 9.11.8
     */
    public void setPageOnly(String pageOnly)
    {
        this.pageOnly = pageOnly;
    }

    /**
     * @param page the page (and its children) concerned by the preference
     * @since 10.8RC1
     * @since 9.11.8
     */
    public void setPage(String page)
    {
        this.page = page;
    }

    /**
     * @param wiki the wiki concerned by the preference
     * @since 10.8RC1
     * @since 9.11.8
     */
    public void setWiki(String wiki)
    {
        this.wiki = wiki;
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
    public String getProviderHint()
    {
        return providerHint;
    }

    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    @Override
    public boolean isActive()
    {
        return active;
    }

    @Override
    public NotificationFilterType getFilterType()
    {
        return filterType;
    }

    @Override
    public Set<NotificationFormat> getNotificationFormats()
    {
        return notificationFormats;
    }

    @Override
    public Date getStartingDate() {
        return startingDate;
    }

    @Override
    public Set<String> getEventTypes()
    {
        return eventTypes;
    }

    @Override
    public String getUser()
    {
        return user;
    }

    @Override
    public String getPageOnly()
    {
        return pageOnly;
    }

    @Override
    public String getPage()
    {
        return page;
    }

    @Override
    public String getWiki()
    {
        return wiki;
    }

    /**
     * @return if the alert format is enabled (for storage use)
     */
    public boolean isAlertEnabled()
    {
        return this.notificationFormats.contains(NotificationFormat.ALERT);
    }

    /**
     * @param alertEnabled if the alert format is enabled (for storage use)
     */
    public void setAlertEnabled(boolean alertEnabled)
    {
        if (alertEnabled) {
            this.notificationFormats.add(NotificationFormat.ALERT);
        } else {
            this.notificationFormats.remove(NotificationFormat.ALERT);
        }
    }

    /**
     * @return if the email format is enabled (for storage used)
     */
    public boolean isEmailEnabled()
    {
        return this.notificationFormats.contains(NotificationFormat.EMAIL);
    }

    /**
     * @param emailEnabled if the email format is enabled (for storage used)
     */
    public void setEmailEnabled(boolean emailEnabled)
    {
        if (emailEnabled) {
            this.notificationFormats.add(NotificationFormat.EMAIL);
        } else {
            this.notificationFormats.remove(NotificationFormat.EMAIL);
        }
    }

    /**
     * To store a list in hibernate without the need to create a new table, we create this accessor that simply
     * join the values together, separated by commas.
     *
     * @return a unique string containing all event types, separated by commas
     */
    public String getAllEventTypes()
    {
        if (eventTypes.isEmpty()) {
            return "";
        }
        // We add a separator (",") at the beginning and at the end so we can make query like
        // "event.eventTypes LIKE '%,someEventType,%'" without of matching an other event type
        return LIST_SEPARATOR + StringUtils.join(eventTypes, LIST_SEPARATOR) + LIST_SEPARATOR;
    }

    /**
     * Allow to load a list stored in hibernate as a commas-separated list of values.
     *
     * @param eventTypes unique string containing all event types, separated by commas
     */
    public void setAllEventTypes(String eventTypes)
    {
        this.eventTypes.clear();

        if (eventTypes != null) {
            String[] types = eventTypes.split(LIST_SEPARATOR);
            for (int i = 0; i < types.length; ++i) {
                if (StringUtils.isNotBlank(types[i])) {
                    this.eventTypes.add(types[i]);
                }
            }
        }
    }

    @Override
    public String toString()
    {
        return "DefaultNotificationFilterPreference{"
                + "id='" + id + '\''
                + ", internalId=" + internalId
                + ", owner='" + owner + '\''
                + ", filterName='" + filterName + '\''
                + ", providerHint='" + providerHint + '\''
                + ", enabled=" + enabled
                + ", active=" + active
                + ", filterType=" + filterType
                + ", notificationFormats=" + notificationFormats
                + ", startingDate=" + startingDate
                + ", eventTypes=" + eventTypes
                + ", user='" + user + '\''
                + ", pageOnly='" + pageOnly + '\''
                + ", page='" + page + '\''
                + ", wiki='" + wiki + '\''
                + '}';
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

        DefaultNotificationFilterPreference other = (DefaultNotificationFilterPreference) o;
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder.append(internalId, other.internalId)
            .append(enabled, other.enabled)
            .append(active, other.active)
            .append(id, other.id)
            .append(owner, other.owner)
            .append(filterName, other.filterName)
            .append(providerHint, other.providerHint)
            .append(filterType, other.filterType)
            .append(notificationFormats, other.notificationFormats)
            .append(startingDate, other.startingDate)
            .append(eventTypes, other.eventTypes)
            .append(user, other.user)
            .append(pageOnly, other.pageOnly)
            .append(page, other.page)
            .append(wiki, other.wiki);
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode()
    {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder.append(internalId)
            .append(enabled)
            .append(active)
            .append(id)
            .append(owner)
            .append(filterName)
            .append(providerHint)
            .append(filterType)
            .append(notificationFormats)
            .append(startingDate)
            .append(eventTypes)
            .append(user)
            .append(pageOnly)
            .append(page)
            .append(wiki);
        return hashCodeBuilder.toHashCode();
    }
}
