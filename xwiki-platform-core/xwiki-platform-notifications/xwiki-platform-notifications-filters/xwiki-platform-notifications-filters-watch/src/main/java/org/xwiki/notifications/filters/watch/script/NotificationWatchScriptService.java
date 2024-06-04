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
package org.xwiki.notifications.filters.watch.script;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.watch.AutomaticWatchMode;
import org.xwiki.notifications.filters.watch.WatchedEntitiesConfiguration;
import org.xwiki.notifications.filters.watch.WatchedEntitiesManager;
import org.xwiki.notifications.filters.watch.WatchedEntityFactory;
import org.xwiki.notifications.filters.watch.WatchedEntityReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.internal.document.DocumentUserReference;

/**
 * Script Service to handle the watched entities. We call `Watched Entities` the locations or the users for that we
 * receive all events.
 *
 * @version $Id$
 * @since 9.8RC1
 */
@Component
@Singleton
@Named(NotificationWatchScriptService.ROLE_HINT)
public class NotificationWatchScriptService implements ScriptService
{
    /**
     * Hint of the component.
     */
    public static final String ROLE_HINT = "notification.watch";

    @Inject
    private WatchedEntitiesManager watchedEntitiesManager;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private WatchedEntityFactory watchedEntityFactory;

    @Inject
    private WatchedEntitiesConfiguration configuration;

    /**
     * @return if the "watched entities" feature is enabled
     */
    public boolean isEnabled()
    {
        return configuration.isEnabled();
    }

    /**
     * Add a filter to watch the specified location.
     *
     * @param location the location to watch
     * @throws NotificationException if an error happens
     */
    public void watchLocation(EntityReference location) throws NotificationException
    {
        watchedEntitiesManager.watchEntity(watchedEntityFactory.createWatchedLocationReference(location),
                documentAccessBridge.getCurrentUserReference()
        );
    }

    /**
     * Remove a filter to stop watching the specified entity.
     *
     * @param location the location
     * @throws NotificationException if an error happens
     */
    public void unwatchLocation(EntityReference location) throws NotificationException
    {
        watchedEntitiesManager.unwatchEntity(watchedEntityFactory.createWatchedLocationReference(location),
                documentAccessBridge.getCurrentUserReference()
        );
    }

    /**
     * Retrieve the specific watched status of a location for the current user.
     *
     * @param location the location for which to compute the watched status
     * @return the specific watched status of the location by the current user
     * @throws NotificationException in case of errors
     * @since 15.5RC1
     */
    @Unstable
    public WatchedEntityReference.WatchedStatus getLocationWatchedStatus(EntityReference location)
            throws NotificationException
    {
        return watchedEntityFactory.createWatchedLocationReference(location)
                .getWatchedStatus(CurrentUserReference.INSTANCE);
    }

    /**
     * Try to retrieve the first ancestor of the location which have a status whose status is watched or blocked.
     * This method returns {@code null} if current watch status is already watched or blocked, and if
     * no ancestor can be found. The first matching ancestor reference is returned along with its computed
     * {@link WatchedEntityReference.WatchedStatus}.
     * @param location the location for which to find an ancestor with a watched status
     * @return a pair containing the watched entity reference of the matching ancestor and its watched status or
     * {@code null}.
     * @see WatchedEntityReference.WatchedStatus#isWatched()
     * @see WatchedEntityReference.WatchedStatus#isBlocked()
     * @throws NotificationException in case of problem for computing the status
     * @since 16.5.0RC1
     */
    @Unstable
    public Pair<EntityReference, WatchedEntityReference.WatchedStatus> getFirstFilteredAncestor(EntityReference
        location) throws NotificationException
    {
        return watchedEntityFactory
            .createWatchedLocationReference(location)
            .getFirstFilteredAncestor(CurrentUserReference.INSTANCE)
            .orElse(null);
    }

    /**
     * @param location the location
     * @return either or not the location is already watched by the current user for any event type.
     * @throws NotificationException if an error happens
     */
    public boolean isLocationWatched(EntityReference location) throws NotificationException
    {
        return watchedEntityFactory.createWatchedLocationReference(location).isWatched(
                documentAccessBridge.getCurrentUserReference()
        );
    }

    /**
     * @param location the location
     * @return either or not the location is already watched by the current user, only for all events type.
     * @throws NotificationException if an error happens
     * @since 12.8RC1
     */
    public boolean isLocationWatchedWithAllEventTypes(EntityReference location) throws NotificationException
    {
        return watchedEntityFactory.createWatchedLocationReference(location).isWatchedWithAllEventTypes(
            documentAccessBridge.getCurrentUserReference()
        );
    }

    /**
     * @param userId id of the user
     * @return either or not the user is already watched by the current user
     * @throws NotificationException if an error happens
     * @since 9.10RC1
     */
    public boolean isUserWatched(String userId) throws NotificationException
    {
        return watchedEntityFactory.createWatchedUserReference(userId).isWatched(
                documentAccessBridge.getCurrentUserReference()
        );
    }

    /**
     * Add a filter to watch the specified user.
     *
     * @param userId the user to watch
     * @throws NotificationException if an error happens
     * @since 9.10RC1
     */
    public void watchUser(String userId) throws NotificationException
    {
        watchedEntitiesManager.watchEntity(watchedEntityFactory.createWatchedUserReference(userId),
                documentAccessBridge.getCurrentUserReference()
        );
    }

    /**
     * Remove a filter to stop watching the specified user.
     *
     * @param userId the user to unwatch
     * @throws NotificationException if an error happens
     * @since 9.10RC1
     */
    public void unwatchUser(String userId) throws NotificationException
    {
        watchedEntitiesManager.unwatchEntity(watchedEntityFactory.createWatchedUserReference(userId),
                documentAccessBridge.getCurrentUserReference()
        );
    }

    /**
     * Utility method to convert a given {@link UserReference} to {@link DocumentReference}.
     * This conversion is only possible if the given UserReference is a DocumentUserReference, or the instance of
     * {@link CurrentUserReference}. In other cases this will throw an exception.
     * This method should be removed once all APIs will use {@link UserReference}.
     * @param userReference the reference for which to retrieve a DocumentReference.
     * @return the context document reference if the user reference is null or the current user reference, else
     *         return the document reference contains in the DocumentUserReference.
     * @throws NotificationException if the user reference is not an instance of DocumentUserReference and not the
     *                               CurrentUserReference.
     * @deprecated Since 13.2RC1: the various API using DocumentReference for users should be refactored
     *              to use UserReference directly.
     */
    @Deprecated
    private DocumentReference convertReference(UserReference userReference) throws NotificationException
    {
        DocumentReference result;
        if (userReference == null || userReference == CurrentUserReference.INSTANCE) {
            result = documentAccessBridge.getCurrentUserReference();
        } else if (userReference instanceof DocumentUserReference) {
            result = ((DocumentUserReference) userReference).getReference();
        } else {
            throw new NotificationException(
                String.format("This should only be used with DocumentUserReference, "
                    + "the given reference was a [%s]", userReference.getClass().getSimpleName()));
        }
        return result;
    }

    /**
     * @return the automatic watch mode configured for the current user
     * @since 9.9RC1
     * @since 9.8.2
     */
    public AutomaticWatchMode getAutomaticWatchMode()
    {
        return configuration.getAutomaticWatchMode(documentAccessBridge.getCurrentUserReference());
    }

    /**
     * @param userReference the user for which to retrieve the watch mode
     * @return the automatic watch mode configured for the given user
     * @since 13.2RC1
     */
    public AutomaticWatchMode getAutomaticWatchMode(UserReference userReference)
        throws NotificationException
    {
        return configuration.getAutomaticWatchMode(this.convertReference(userReference));
    }

    /**
     * @return the default automatic watch mode configured for the current wiki
     * @since 9.11.8
     * @since 10.6RC1
     */
    public AutomaticWatchMode getDefaultAutomaticWatchMode()
    {
        return configuration.getDefaultAutomaticWatchMode(
                documentAccessBridge.getCurrentDocumentReference().getWikiReference());
    }

    /**
     * @return the user watched by the current user
     * @throws NotificationException if an error occurs
     * @since 10.4RC1
     */
    public Collection<String> getWatchedUsers() throws NotificationException
    {
        return watchedEntitiesManager.getWatchedUsers(documentAccessBridge.getCurrentUserReference());
    }
}
