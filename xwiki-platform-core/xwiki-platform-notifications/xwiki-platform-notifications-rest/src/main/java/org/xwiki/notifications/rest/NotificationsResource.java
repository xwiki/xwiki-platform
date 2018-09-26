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
package org.xwiki.notifications.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.xwiki.stability.Unstable;

/**
 * Retrieve notifications.
 *
 * @version $Id$
 * @since 10.4RC1
 */
@Path("/notifications")
@Unstable
public interface NotificationsResource
{
    /**
     * Get notifications RSS for the given parameters.
     * @return notifications
     * @throws Exception if an error occurs
     */
    @GET
    Response getNotifications(
            @QueryParam("useUserPreferences") String useUserPreferences,
            @QueryParam("userId") String userId,
            @QueryParam("untilDate") String untilDate,
            @QueryParam("blackList") String blackList,
            @QueryParam("pages") String pages,
            @QueryParam("spaces") String spaces,
            @QueryParam("wikis") String wikis,
            @QueryParam("users") String users,
            @QueryParam("count") String count,
            @QueryParam("displayOwnEvents") String displayOwnEvents,
            @QueryParam("displayMinorEvents") String displayMinorEvents,
            @QueryParam("displaySystemEvents") String displaySystemEvents,
            @QueryParam("displayReadEvents") String displayReadEvents,
            @QueryParam("displayReadStatus") String displayReadStatus,
            @QueryParam("tags") String tags
            ) throws Exception;

    /**
     * Get notifications RSS for the given parameters. Since the URL and the return type is different, I had no choice
     * but duplicating the same parameters than <code>getNotifications()</code>.
     *
     * @return the RSS feed as a string
     * @throws Exception if an error occurs
     *
     * @since 10.6RC1
     */
    @GET
    @Path("/rss")
    String getNotificationsRSS(
            @QueryParam("useUserPreferences") String useUserPreferences,
            @QueryParam("userId") String userId,
            @QueryParam("untilDate") String untilDate,
            @QueryParam("blackList") String blackList,
            @QueryParam("pages") String pages,
            @QueryParam("spaces") String spaces,
            @QueryParam("wikis") String wikis,
            @QueryParam("users") String users,
            @QueryParam("count") String count,
            @QueryParam("displayOwnEvents") String displayOwnEvents,
            @QueryParam("displayMinorEvents") String displayMinorEvents,
            @QueryParam("displaySystemEvents") String displaySystemEvents,
            @QueryParam("displayReadEvents") String displayReadEvents,
            @QueryParam("displayReadStatus") String displayReadStatus,
            @QueryParam("tags") String tags
    ) throws Exception;
}
