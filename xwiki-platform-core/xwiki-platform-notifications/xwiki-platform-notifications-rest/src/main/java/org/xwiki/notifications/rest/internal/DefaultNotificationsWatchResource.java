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

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.filters.watch.WatchedEntitiesManager;
import org.xwiki.notifications.filters.watch.WatchedEntityFactory;
import org.xwiki.notifications.filters.watch.WatchedEntityReference;
import org.xwiki.notifications.filters.watch.WatchedLocationReference;
import org.xwiki.notifications.rest.NotificationsWatchResource;
import org.xwiki.rest.XWikiResource;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

@Component
@Named("org.xwiki.notifications.rest.internal.DefaultNotificationsWatchResource")
public class DefaultNotificationsWatchResource extends XWikiResource implements NotificationsWatchResource
{
    @Inject
    private WatchedEntitiesManager watchedEntitiesManager;

    @Inject
    private WatchedEntityFactory watchedEntityFactory;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @Override
    public Response getPageWatchStatus(String wikiName, String spaceNames, String pageName) throws Exception
    {
        UserReference user = getUser();
        if (user == GuestUserReference.INSTANCE) {
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode(),
                "Only logged-in users can access this.").build();
        }
        List<String> spaces = parseSpaceSegments(spaceNames);
        if (StringUtils.isEmpty(wikiName) || StringUtils.isEmpty(spaceNames) || StringUtils.isEmpty(pageName)) {
            throw new IllegalArgumentException(
                String.format("wikiName, spaceName and pageName must all be not null. Current values: (%s:%s.%s)",
                    wikiName, spaces, pageName));
        }
        DocumentReference documentReference = new DocumentReference(wikiName, spaces, pageName);
        WatchedLocationReference watchedLocationReference =
            this.watchedEntityFactory.createWatchedLocationReference(documentReference);
        WatchedEntityReference.WatchedStatus watchedStatus = watchedLocationReference.getWatchedStatus(user);
        return Response.ok(watchedStatus).build();
    }

    private UserReference getUser()
    {
        return this.userReferenceResolver.resolve(getXWikiContext().getUserReference());
    }

    private Response watchLocation(EntityReference location, boolean ignore) throws Exception
    {
        UserReference user = getUser();
        if (user == GuestUserReference.INSTANCE) {
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode(),
                "Only logged-in users can access this.").build();
        }

        WatchedLocationReference watchedLocationReference =
            this.watchedEntityFactory.createWatchedLocationReference(location);
        boolean result;
        if (ignore) {
            result = this.watchedEntitiesManager.block(watchedLocationReference, getUser());
        } else {
            result = this.watchedEntitiesManager.watch(watchedLocationReference, getUser());
        }
        return Response.ok(result).build();
    }

    @Override
    public Response watchWiki(String wikiName, boolean ignore) throws Exception
    {
        if (StringUtils.isEmpty(wikiName)) {
            throw new IllegalArgumentException(
                String.format("wikiName must be not null. Current value: [%s]", wikiName));
        }
        return watchLocation(new WikiReference(wikiName), ignore);
    }

    @Override
    public Response watchSpace(String wikiName, String spaceNames, boolean ignore) throws Exception
    {
        if (StringUtils.isEmpty(wikiName) || StringUtils.isEmpty(spaceNames)) {
            throw new IllegalArgumentException(
                String.format("wikiName and spaceNames must be not null. Current value: [%s:%s]",
                    wikiName, spaceNames));
        }
        List<String> spaces = parseSpaceSegments(spaceNames);
        SpaceReference spaceReference = new SpaceReference(wikiName, spaces);
        return watchLocation(spaceReference, ignore);
    }

    @Override
    public Response watchPage(String wikiName, String spaceNames, String pageName, boolean ignore) throws Exception
    {
        if (StringUtils.isEmpty(wikiName) || StringUtils.isEmpty(spaceNames) || StringUtils.isEmpty(pageName)) {
            throw new IllegalArgumentException(
                String.format("wikiName, spaceName and pageName must all be not null. Current values: (%s:%s.%s)",
                    wikiName, spaceNames, pageName));
        }
        List<String> spaces = parseSpaceSegments(spaceNames);
        DocumentReference documentReference = new DocumentReference(wikiName, spaces, pageName);
        return watchLocation(documentReference, ignore);
    }

    private Response unwatchLocation(EntityReference location) throws Exception
    {
        UserReference user = getUser();
        if (user == GuestUserReference.INSTANCE) {
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode(),
                "Only logged-in users can access this.").build();
        }

        WatchedLocationReference watchedLocationReference =
            this.watchedEntityFactory.createWatchedLocationReference(location);
        boolean result = this.watchedEntitiesManager.removeWatchFilter(watchedLocationReference, user);
        return Response.ok(result).build();
    }

    @Override
    public Response unwatchWiki(String wikiName) throws Exception
    {
        if (StringUtils.isEmpty(wikiName)) {
            throw new IllegalArgumentException(
                String.format("wikiName must be not null. Current value: [%s]", wikiName));
        }

        return unwatchLocation(new WikiReference(wikiName));
    }

    @Override
    public Response unwatchSpace(String wikiName, String spaceNames) throws Exception
    {
        if (StringUtils.isEmpty(wikiName) || StringUtils.isEmpty(spaceNames)) {
            throw new IllegalArgumentException(
                String.format("wikiName and spaceNames must be not null. Current value: [%s:%s]",
                    wikiName, spaceNames));
        }
        List<String> spaces = parseSpaceSegments(spaceNames);
        SpaceReference spaceReference = new SpaceReference(wikiName, spaces);
        return unwatchLocation(spaceReference);
    }

    @Override
    public Response unwatchPage(String wikiName, String spaceNames, String pageName) throws Exception
    {
        if (StringUtils.isEmpty(wikiName) || StringUtils.isEmpty(spaceNames) || StringUtils.isEmpty(pageName)) {
            throw new IllegalArgumentException(
                String.format("wikiName, spaceName and pageName must all be not null. Current values: (%s:%s.%s)",
                    wikiName, spaceNames, pageName));
        }
        List<String> spaces = parseSpaceSegments(spaceNames);
        DocumentReference documentReference = new DocumentReference(wikiName, spaces, pageName);
        return unwatchLocation(documentReference);
    }
}
