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

package org.xwiki.notifications.rest.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.Response;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.filters.watch.WatchedEntitiesManager;
import org.xwiki.notifications.filters.watch.WatchedEntityFactory;
import org.xwiki.notifications.filters.watch.WatchedEntityReference;
import org.xwiki.notifications.filters.watch.WatchedLocationReference;
import org.xwiki.notifications.rest.NotificationsWatchResource;
import org.xwiki.rest.XWikiResource;

@Component
@Named("org.xwiki.notifications.rest.internal.DefaultNotificationsWatchResource")
public class DefaultNotificationsWatchResource extends XWikiResource implements NotificationsWatchResource
{
    @Inject
    private WatchedEntitiesManager watchedEntitiesManager;

    @Inject
    private WatchedEntityFactory watchedEntityFactory;

    @Override
    public Response getPageWatchStatus(String wikiName, String spaceNames, String pageName) throws Exception
    {
        DocumentReference userReference = getXWikiContext().getUserReference();
        if (userReference == null) {
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode(),
                "Only logged-in users can access this.").build();
        }
        List<String> spaces = parseSpaceSegments(spaceNames);
        if ((wikiName == null) || (spaces == null || spaces.isEmpty()) || (pageName == null)) {
            throw new IllegalArgumentException(
                String.format("wikiName, spaceName and pageName must all be not null. Current values: (%s:%s.%s)",
                    wikiName, spaces, pageName));
        }
        DocumentReference documentReference = new DocumentReference(wikiName, spaces, pageName);
        WatchedLocationReference watchedLocationReference =
            this.watchedEntityFactory.createWatchedLocationReference(documentReference);
        WatchedEntityReference.WatchedStatus watchedStatus = watchedLocationReference.getWatchedStatus(userReference);
        return Response.ok(watchedStatus).build();
    }

    @Override
    public Response watchWiki(String wikiName)
    {
        return null;
    }

    @Override
    public Response watchSpace(String wikiName, String spaceNames)
    {
        return null;
    }

    @Override
    public Response watchPage(String wikiName, String spaceNames, String pageName)
    {
        return null;
    }

    @Override
    public Response unwatchWiki(String wikiName)
    {
        return null;
    }

    @Override
    public Response unwatchSpace(String wikiName, String spaceNames)
    {
        return null;
    }

    @Override
    public Response unwatchPage(String wikiName, String spaceNames, String pageName)
    {
        return null;
    }
}
