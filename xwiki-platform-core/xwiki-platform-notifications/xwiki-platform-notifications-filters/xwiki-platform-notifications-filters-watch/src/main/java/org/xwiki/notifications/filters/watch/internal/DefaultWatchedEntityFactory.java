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
package org.xwiki.notifications.filters.watch.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.notifications.filters.NotificationFilterPreferenceManager;
import org.xwiki.notifications.filters.internal.user.EventUserFilterPreferencesGetter;
import org.xwiki.notifications.filters.watch.WatchedEntityFactory;
import org.xwiki.notifications.filters.watch.WatchedLocationReference;
import org.xwiki.notifications.filters.watch.WatchedUserReference;
import org.xwiki.user.UserReferenceSerializer;

/**
 * Default implementation of {@link WatchedEntityFactory}.
 *
 * @version $Id$
 * @since 9.8RC1
 */
@Component
@Singleton
public class DefaultWatchedEntityFactory implements WatchedEntityFactory
{
    @Inject
    private EventUserFilterPreferencesGetter eventUserFilterPreferencesGetter;

    @Inject
    private NotificationFilterPreferenceManager notificationFilterPreferenceManager;

    @Inject
    @Named("document")
    private UserReferenceSerializer<DocumentReference> userReferenceSerializer;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Override
    public WatchedLocationReference createWatchedLocationReference(EntityReference location)
    {
        return new WatchedLocationReference(location, componentManager);
    }

    @Override
    public WatchedUserReference createWatchedUserReference(String userId)
    {
        return new WatchedUserReference(userId, eventUserFilterPreferencesGetter, notificationFilterPreferenceManager,
            userReferenceSerializer);
    }
}
