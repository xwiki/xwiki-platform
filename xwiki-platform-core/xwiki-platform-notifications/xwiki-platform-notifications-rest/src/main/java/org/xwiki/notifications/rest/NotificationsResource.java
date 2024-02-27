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

import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * Retrieve notifications.
 *
 * @version $Id$
 * @since 10.4RC1
 */
@Path("/notifications")
public interface NotificationsResource
{
    /**
     * Get notifications for the given parameters.
     * @return notifications
     * @throws Exception if an error occurs
     */
    @GET
    Response getNotifications(
            @QueryParam("useUserPreferences") String useUserPreferences,
            @QueryParam("userId") String userId,
            @QueryParam("untilDate") String untilDate,
            @QueryParam("untilDateIncluded") @DefaultValue("true") boolean untilDateIncluded,
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
            @QueryParam("tags") String tags,
            @QueryParam("currentWiki") String currentWiki,
            @QueryParam("async") String async,
            @QueryParam("asyncId") String asyncId,
            @QueryParam("target") @DefaultValue("alert") String target
            ) throws Exception;

    /**
     * Get the number of notifications for the given parameters.
     * @return notifications
     * @throws Exception if an error occurs
     * @since 10.11.4
     * @since 11.2
     */
    @GET
    @Path("/count")
    Response getNotificationsCount(
            @QueryParam("useUserPreferences") String useUserPreferences,
            @QueryParam("userId") String userId,
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
            @QueryParam("tags") String tags,
            @QueryParam("currentWiki") String currentWiki,
            @QueryParam("async") String async,
            @QueryParam("asyncId") String asyncId
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
            @QueryParam("tags") String tags,
            @QueryParam("currentWiki") String currentWiki
    ) throws Exception;

    /**
     * Get notifications matching the given parameters. The POST method is used to allow large content in the
     * parameters. For example, the parameter "blackList" could be very long, and the associated URL with the GET method
     * would be too long (generating HTTP 414 error).
     * <p>
     * While signature of the method changed from Java point of view in 16.2.0RC1, it's still exactly the same API from
     * REST point of view so the same &#64;since has been kept.
     *
     * @return notifications
     * @throws Exception if an error occurs
     * @since 10.8RC1
     * @since 10.8.1
     * @since 9.11.8
     */
    @POST
    Response postNotifications(
        @FormParam("useUserPreferences") String useUserPreferences,
        @FormParam("userId") String userId,
        @FormParam("untilDate") String untilDate,
        @FormParam("untilDateIncluded") @DefaultValue("true") boolean untilDateIncluded,
        @FormParam("blackList") String blackList,
        @FormParam("pages") String pages,
        @FormParam("spaces") String spaces,
        @FormParam("wikis") String wikis,
        @FormParam("users") String users,
        @FormParam("count") String count,
        @FormParam("displayOwnEvents") String displayOwnEvents,
        @FormParam("displayMinorEvents") String displayMinorEvents,
        @FormParam("displaySystemEvents") String displaySystemEvents,
        @FormParam("displayReadEvents") String displayReadEvents,
        @FormParam("displayReadStatus") String displayReadStatus,
        @FormParam("tags") String tags,
        @FormParam("currentWiki") String currentWiki,
        @FormParam("async") String async,
        @FormParam("asyncId") String asyncId,
        @FormParam("target") @DefaultValue("alert") String target
    ) throws Exception;
}
