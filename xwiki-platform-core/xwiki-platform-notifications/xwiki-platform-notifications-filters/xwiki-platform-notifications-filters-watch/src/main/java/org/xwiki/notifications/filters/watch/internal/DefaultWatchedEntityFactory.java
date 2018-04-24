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
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.internal.scope.ScopeNotificationFilterLocationStateComputer;
import org.xwiki.notifications.filters.internal.user.EventUserFilterPreferencesGetter;
import org.xwiki.notifications.filters.watch.WatchedEntityFactory;
import org.xwiki.notifications.filters.watch.WatchedLocationReference;
import org.xwiki.notifications.filters.watch.WatchedUserReference;

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
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private EntityReferenceResolver<String> resolver;

    @Inject
    private ScopeNotificationFilterLocationStateComputer stateComputer;

    @Inject
    private EventUserFilterPreferencesGetter eventUserFilterPreferencesGetter;

    @Inject
    private NotificationFilterManager notificationFilterManager;

    @Override
    public WatchedLocationReference createWatchedLocationReference(EntityReference location)
    {
        return new WatchedLocationReference(location, serializer.serialize(location), resolver, stateComputer,
                notificationFilterManager);
    }

    @Override
    public WatchedUserReference createWatchedUserReference(String userId)
    {
        return new WatchedUserReference(userId, eventUserFilterPreferencesGetter, notificationFilterManager);
    }
}
