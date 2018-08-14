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
import java.util.HashSet;
import java.util.Set;

import org.xwiki.notifications.NotificationFormat;

/**
 * Define the preference of a notification filter.
 *
 * This class replaces the previous interface that had the same name.
 *
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
 * @since 10.7RC1
 * @since 9.11.8
 */
public class NotificationFilterPreference
{
    private long id;

    private String owner;

    private String filterName;

    private String providerHint;

    private boolean enabled;

    private boolean active;

    private NotificationFilterType filterType;

    private Set<NotificationFormat> filterFormats = new HashSet<>();

    private Date startingDate;

    private String eventType;

    private String user;

    private String pageOnly;

    private String page;

    private String wiki;

    private boolean alertEnabled;

    private boolean emailEnabled;

    public NotificationFilterPreference()
    {

    }

    public NotificationFilterPreference(NotificationFilterPreference notificationFilterPreference)
    {
        this.id = notificationFilterPreference.id;
        this.owner = notificationFilterPreference.filterName;
        this.filterName = notificationFilterPreference.filterName;
        this.providerHint = notificationFilterPreference.providerHint;
        this.enabled = notificationFilterPreference.enabled;
        this.active = notificationFilterPreference.active;
        this.filterType = notificationFilterPreference.filterType;
        this.filterFormats = notificationFilterPreference.filterFormats;
        this.startingDate = notificationFilterPreference.startingDate;
        this.eventType = notificationFilterPreference.eventType;
        this.user = notificationFilterPreference.user;
        this.pageOnly = notificationFilterPreference.pageOnly;
        this.page = notificationFilterPreference.page;
        this.wiki = notificationFilterPreference.wiki;
        this.alertEnabled = notificationFilterPreference.alertEnabled;
        this.emailEnabled = notificationFilterPreference.emailEnabled;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public String getOwner()
    {
        return owner;
    }

    public void setOwner(String owner)
    {
        this.owner = owner;
    }

    public void setFilterName(String filterName)
    {
        this.filterName = filterName;
    }

    public void setProviderHint(String providerHint)
    {
        this.providerHint = providerHint;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public void setActive(boolean active)
    {
        this.active = active;
    }

    public void setFilterType(NotificationFilterType filterType)
    {
        this.filterType = filterType;
    }

    public void setFilterFormats(Set<NotificationFormat> filterFormats)
    {
        this.filterFormats = filterFormats;
        this.alertEnabled = filterFormats.contains(NotificationFormat.ALERT);
        this.emailEnabled = filterFormats.contains(NotificationFormat.EMAIL);
    }

    public void setStartingDate(Date startingDate)
    {
        this.startingDate = startingDate;
    }

    public void setEventType(String eventType)
    {
        this.eventType = eventType;
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    public void setPageOnly(String pageOnly)
    {
        this.pageOnly = pageOnly;
    }

    public void setPage(String page)
    {
        this.page = page;
    }

    public void setWiki(String wiki)
    {
        this.wiki = wiki;
    }

    /**
     * @return the unique identifier of the filter preference.
     */
    public long getId()
    {
        return id;
    }

    /**
     * @return the name of the filter corresponding to this preference.
     */
    public String getFilterName()
    {
        return filterName;
    }

    /**
     * @return the name of the {@link NotificationFilterPreferenceProvider} associated with this preference.
     */
    public String getProviderHint()
    {
        return providerHint;
    }

    /**
     * @return true if the current notification preference is enabled.
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * A filter preference can either be active or passive. It the preference is active, then it should force the
     * retrieval of notifications when used in conjunction with a {@link NotificationFilter}.
     *
     * On the other hand, a passive (non-active) notification filter should not automatically trigger the retrieval of
     * notifications.
     *
     * @return true if the filter preference is active.
     */
    public boolean isActive()
    {
        return active;
    }

    /**
     * @return the type of the filter described by this preference.
     */
    public NotificationFilterType getFilterType()
    {
        return filterType;
    }

    /**
     * @return a set of {@link NotificationFormat} for which the filter should be applied.
     */
    public Set<NotificationFormat> getFilterFormats()
    {
        return filterFormats;
    }

    /**
     * @return the date from which the filter preference is enabled.
     * @since 10.5RC1
     * @since 10.4
     * @since 9.11.5
     */
    public Date getStartingDate() {
        return startingDate;
    }

    public String getEventType()
    {
        return eventType;
    }

    public String getUser()
    {
        return user;
    }

    public String getPageOnly()
    {
        return pageOnly;
    }

    public String getPage()
    {
        return page;
    }

    public String getWiki()
    {
        return wiki;
    }

    public boolean isAlertEnabled()
    {
        return alertEnabled;
    }

    public void setAlertEnabled(boolean alertEnabled)
    {
        this.alertEnabled = alertEnabled;
        if (alertEnabled) {
            this.filterFormats.add(NotificationFormat.ALERT);
        } else {
            this.filterFormats.remove(NotificationFormat.ALERT);
        }
    }

    public boolean isEmailEnabled()
    {
        return emailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled)
    {
        this.emailEnabled = emailEnabled;
        if (emailEnabled) {
            this.filterFormats.add(NotificationFormat.EMAIL);
        } else {
            this.filterFormats.remove(NotificationFormat.EMAIL);
        }
    }
}
