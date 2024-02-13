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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.stability.Unstable;

import static com.xpn.xwiki.doc.XWikiDocument.DB_SPACE_SEP;

/**
 * Define the preference of a notification filter.
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
 * @since 9.7RC1
 */
public interface NotificationFilterPreference
{
    /**
     * Prefix to be used for the ID only when the preference is stored in database.
     * @since 16.5.0RC1
     */
    @Unstable
    String DB_STORED_FILTER_PREFIX = "NFP_";
    
    /**
     * @return the unique identifier of the filter preference.
     * @since 10.8RC1
     * @since 9.11.8
     */
    String getId();

    /**
     * @return the name of the filter corresponding to this preference.
     */
    String getFilterName();

    /**
     * @return true if the current notification preference is enabled.
     */
    boolean isEnabled();

    /**
     * A filter preference can either be active or passive. It the preference is active, then it should force the
     * retrieval of notifications when used in conjunction with a {@link NotificationFilter}.
     *
     * On the other hand, a passive (non-active) notification filter should not automatically trigger the retrieval of
     * notifications.
     *
     * @return true if the filter preference is active.
     */
    boolean isActive();

    /**
     * @return the type of the filter described by this preference.
     */
    NotificationFilterType getFilterType();

    /**
     * @return a set of {@link NotificationFormat} for which the filter should be applied.
     * @since 10.8RC1
     * @since 9.11.8
     */
    Set<NotificationFormat> getNotificationFormats();

    /**
     * @return the date from which the filter preference is enabled.
     *
     * @since 10.5RC1
     * @since 10.4
     * @since 9.11.5
     */
    Date getStartingDate();

    /**
     * @return the event types concerned by the preference (can be empty to affect all event types)
     *
     * @since 10.8RC1
     * @since 9.11.8
     */
    Set<String> getEventTypes();

    /**
     * @return the user concerned by the preference (can be null)
     * @since 10.8RC1
     * @since 9.11.8
     */
    String getUser();

    /**
     * @return the page concerned by the preference (can be null)
     * @since 10.8RC1
     * @since 9.11.8
     */
    String getPageOnly();

    /**
     * @return the page (and its children) concerned by the preference (can be null)
     * @since 10.8RC1
     * @since 9.11.8
     */
    String getPage();

    /**
     * @return the wiki concerned by the preference (can be null)
     * @since 10.8RC1
     * @since 9.11.8
     */
    String getWiki();

    /**
     * @param enabled if the preference is enabled or not
     * @since 10.8RC1
     * @since 9.11.8
     */
    void setEnabled(boolean enabled);

    /**
     * Return {@code true} if the filter preference is related to a given wiki. This is the case if {@link #getWiki()}
     * returns {@code wikiId}, or if {@link #getPage()}, {@link #getPageOnly()} or {@link #getUser()} are containing the
     * wiki identifier.
     *
     * @param wikiId a wiki identifier
     * @return {@code true} if the notification filter preference is related to a given wiki, {@code false} otherwise
     * @since 14.5
     * @since 14.4.1
     * @since 13.10.7
     */
    default boolean isFromWiki(String wikiId)
    {
        String wikiIdWithPrefix = wikiId + DB_SPACE_SEP;
        return Objects.equals(getWiki(), wikiId)
            || StringUtils.startsWith(getPage(), wikiIdWithPrefix)
            || StringUtils.startsWith(getPageOnly(), wikiIdWithPrefix)
            || StringUtils.startsWith(getUser(), wikiIdWithPrefix);
    }

    /**
     * {@link #getWiki()}, {@link #getPage()}, {@link #getPageOnly()} and {@link #getUser()} are analyzed (in this
     * order) to find the wiki identifier. The first non-null value is used.
     *
     * @return the wiki identifier of the resource concerned by the filter preference, {@link Optional#empty()} if no
     *     wiki identifier can be found
     * @since 14.5
     * @since 14.4.1
     * @since 13.10.7
     */
    default Optional<String> getWikiId()
    {
        String wikiId = null;
        if (getWiki() != null) {
            wikiId = getWiki();
        } else if (getPage() != null) {
            wikiId = StringUtils.substringBefore(getPage(), DB_SPACE_SEP);
        } else if (getPageOnly() != null) {
            wikiId = StringUtils.substringBefore(getPageOnly(), DB_SPACE_SEP);
        } else if (getUser() != null) {
            wikiId = StringUtils.substringBefore(getUser(), DB_SPACE_SEP);
        }
        return Optional.ofNullable(wikiId);
    }
}
