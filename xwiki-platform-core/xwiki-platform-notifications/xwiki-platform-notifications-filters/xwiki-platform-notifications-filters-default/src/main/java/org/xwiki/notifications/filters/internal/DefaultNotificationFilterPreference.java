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
 * Those wiki users and those event types are like parameters to the filter, a {@link DefaultNotificationFilterPreference}
 * is nothing else than a combination of those parameters.
 *
 * @version $Id$
 * @since 10.7RC1
 * @since 9.11.8
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

    private boolean alertEnabled;

    private boolean emailEnabled;

    public DefaultNotificationFilterPreference()
    {

    }

    public DefaultNotificationFilterPreference(NotificationFilterPreference notificationFilterPreference)
    {
        if (notificationFilterPreference instanceof DefaultNotificationFilterPreference) {
            this.internalId =( (DefaultNotificationFilterPreference) notificationFilterPreference).internalId;
            this.owner =( (DefaultNotificationFilterPreference) notificationFilterPreference).owner;
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

    public void setId(String id)
    {
        this.id = id;
    }

    public long getInternalId()
    {
        return internalId;
    }

    public void setInternalId(long internalId)
    {
        this.internalId = internalId;
        this.id = String.format("NFP_%x", internalId);
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

    public void setNotificationFormats(Set<NotificationFormat> filterFormats)
    {
        this.notificationFormats = filterFormats;
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

    public boolean isAlertEnabled()
    {
        return alertEnabled;
    }

    public void setAlertEnabled(boolean alertEnabled)
    {
        this.alertEnabled = alertEnabled;
        if (alertEnabled) {
            this.notificationFormats.add(NotificationFormat.ALERT);
        } else {
            this.notificationFormats.remove(NotificationFormat.ALERT);
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
            this.notificationFormats.add(NotificationFormat.EMAIL);
        } else {
            this.notificationFormats.remove(NotificationFormat.EMAIL);
        }
    }
}
