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

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.xwiki.notifications.filters.watch.WatchedEntityReference;
import org.xwiki.stability.Unstable;
import org.xwiki.user.UserReference;

/**
 * REST resources for handling watch filters.
 *
 * @version $Id$
 * @since 16.5.0RC1
 */
@Unstable
@Path("/wikis/{wikiName}{spaceName : (/spaces/[^/]+)*}{pageName : (/pages/[^/]+)?}/notificationsWatches")
public interface NotificationsWatchResource
{
    /**
     * Compute and return the {@link org.xwiki.notifications.filters.watch.WatchedEntityReference.WatchedStatus} of
     * given page for current user.
     * This method should return an error when called with guest.
     *
     * @param wikiName the name of the wiki in the page reference
     * @param spaceNames the list of spaces in the page reference
     * @param pageName the name of the page in the page reference
     * @return the {@link org.xwiki.notifications.filters.watch.WatchedEntityReference.WatchedStatus} or an error
     * @throws Exception in case of problem for computing the status
     */
    @GET
    Response getPageWatchStatus(
        @PathParam("wikiName") String wikiName,
        @PathParam("spaceName") @Encoded String spaceNames,
        @PathParam("pageName") String pageName) throws Exception;

    /**
     * Create a new filter for the given page: either for watching or for ignoring, depending on the {@param ignore}.
     *
     * @param wikiName the name of the wiki for the page reference
     * @param spaceNames the list of spaces for the page reference
     * @param pageName the name of the page for the page reference
     * @param ignore {@code false} to watch the page and {@code true} to ignore it
     * @return a boolean value corresponding to the creation of a filter or not (depending if it already exists) or
     * an error.
     * @see org.xwiki.notifications.filters.watch.WatchedEntitiesManager#watch(WatchedEntityReference, UserReference)
     * @see org.xwiki.notifications.filters.watch.WatchedEntitiesManager#block(WatchedEntityReference, UserReference)
     * @throws Exception in case of problem for creating the filter
     */
    @PUT
    Response watchPage(
        @PathParam("wikiName") String wikiName,
        @PathParam("spaceName") @Encoded String spaceNames,
        @PathParam("pageName") String pageName,
        @QueryParam("ignore") @DefaultValue("false") boolean ignore) throws Exception;

    /**
     * Remove the filter corresponding to the exact page reference.
     *
     * @param wikiName the name of the wiki for the page reference
     * @param spaceNames the list of spaces for the page reference
     * @param pageName the name of the page for the page reference
     * @return {@code true} if a filter has been removed, {@code false} if no filter has been found and an error code
     *         if an argument was wrong.
     * @throws Exception in case of problem when removing the filter
     * @see org.xwiki.notifications.filters.watch.WatchedEntitiesManager#removeWatchFilter(WatchedEntityReference,
     * UserReference)
     */
    @DELETE
    Response unwatchPage(
        @PathParam("wikiName") String wikiName,
        @PathParam("spaceName") @Encoded String spaceNames,
        @PathParam("pageName") String pageName) throws Exception;
}
