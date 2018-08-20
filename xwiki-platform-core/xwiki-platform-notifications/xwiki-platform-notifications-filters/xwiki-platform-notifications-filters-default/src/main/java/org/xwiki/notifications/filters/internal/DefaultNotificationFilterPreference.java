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

import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;
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

    private String providerHint;

    private boolean enabled;

    private boolean active;

    private NotificationFilterType filterType;

    private Set<NotificationFormat> notificationFormats = new HashSet<>();

    private Date startingDate;

    private String eventType;

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
        this.eventType = notificationFilterPreference.getEventType();
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
     * @param eventType the event type concerned by the preference
     */
    public void setEventType(String eventType)
    {
        this.eventType = eventType;
    }

    /**
     * @param user the user concerned by the preference
     */
    public void setUser(String user)
    {
        this.user = user;
    }

    /**
     * @param pageOnly the page concerned by the preference
     */
    public void setPageOnly(String pageOnly)
    {
        this.pageOnly = pageOnly;
    }

    /**
     * @param page the page (and its children) concerned by the preference
     */
    public void setPage(String page)
    {
        this.page = page;
    }

    /**
     * @param wiki the wiki concerned by the preference
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
    public String getEventType()
    {
        return eventType;
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
        return this.notificationFormats.contains(NotificationFormat.ALERT);
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
}
