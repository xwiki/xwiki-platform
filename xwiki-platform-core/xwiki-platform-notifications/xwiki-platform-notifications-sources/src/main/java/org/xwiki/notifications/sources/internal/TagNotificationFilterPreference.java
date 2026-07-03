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
package org.xwiki.notifications.sources.internal;

import java.util.Date;
import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterType;

/**
 * A notification filter preference for the Tag notification filter (for internal use).
 *
 * @version $Id$
 * @since 10.9
 */
public class TagNotificationFilterPreference implements NotificationFilterPreference
{
    private String tag;

    private String currentWiki;

    /**
     * Construct a TagNotificationFilterPreference for the given tag.
     * @param tag the tag to watch
     * @param currentWiki the current wiki (where the tags should be loaded)
     */
    public TagNotificationFilterPreference(String tag, String currentWiki)
    {
        this.tag = tag;
        this.currentWiki = currentWiki;
    }

    /**
     * @return the tag to watch
     */
    public String getTag()
    {
        return tag;
    }

    /**
     * @return the current wiki (where the tags should be loaded)
     */
    public String getCurrentWiki()
    {
        return currentWiki;
    }

    @Override
    public String getId()
    {
        return null;
    }

    @Override
    public String getFilterName()
    {
        return TagNotificationFilter.NAME;
    }

    @Override
    public boolean isEnabled()
    {
        return true;
    }

    @Override
    public NotificationFilterType getFilterType()
    {
        return NotificationFilterType.INCLUSIVE;
    }

    @Override
    public Set<NotificationFormat> getNotificationFormats()
    {
        return null;
    }

    @Override
    public Date getStartingDate()
    {
        return null;
    }

    @Override
    public Set<String> getEventTypes()
    {
        return null;
    }

    @Override
    public String getUser()
    {
        return null;
    }

    @Override
    public String getPageOnly()
    {
        return null;
    }

    @Override
    public String getPage()
    {
        return null;
    }

    @Override
    public String getWiki()
    {
        return null;
    }

    @Override
    public void setEnabled(boolean enabled)
    {

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

        TagNotificationFilterPreference that = (TagNotificationFilterPreference) o;

        return new EqualsBuilder()
            .append(tag, that.tag)
            .append(currentWiki, that.currentWiki)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(tag)
            .append(currentWiki)
            .toHashCode();
    }
}
