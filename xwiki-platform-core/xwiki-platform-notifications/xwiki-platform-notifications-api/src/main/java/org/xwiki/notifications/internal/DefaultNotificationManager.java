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
package org.xwiki.notifications.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStream;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.notifications.NotificationDisplayer;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationManager;
import org.xwiki.notifications.NotificationPreference;
import org.xwiki.notifications.events.NotificationEvent;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.text.StringUtils;

/**
 * @version $Id$
 * @since 9.2RC1
 */
@Component
@Singleton
public class DefaultNotificationManager implements NotificationManager
{
    @Inject
    private EventStream eventStream;

    @Inject
    private QueryManager queryManager;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private ModelBridge modelBridge;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private NotificationDisplayer defaultDisplayer;

    @Override
    public List<Event> getEvents(int offset, int limit) throws NotificationException
    {
        return getEvents(documentAccessBridge.getCurrentUserReference(), offset, limit);
    }

    @Override
    public List<Event> getEvents(String userId, int offset, int limit) throws NotificationException
    {
        return getEvents(documentReferenceResolver.resolve(userId), offset, limit);
    }

    private List<Event> getEvents(DocumentReference user, int offset, int limit) throws NotificationException
    {
        try {
            String hql = "";
            String appender = "where ";

            List<NotificationPreference> preferences = getPreferences(user);

            List<String> types = new ArrayList<>();
            for (NotificationPreference preference : preferences) {
                if (preference.isNotificationEnabled() && StringUtils.isNotBlank(preference.getEventType())) {
                    types.add(preference.getEventType());
                }
            }
            if (!types.isEmpty()) {
                hql += appender + "event.type IN :types";
                appender = " ";
            }

            List<String> apps = new ArrayList<>();
            for (NotificationPreference preference : preferences) {
                if (preference.isNotificationEnabled() && StringUtils.isNotBlank(preference.getApplicationId())) {
                    apps.add(preference.getApplicationId());
                }
            }
            if (!apps.isEmpty()) {
                hql += appender + " OR event.application IN :apps";
            }

            // No notification is returned if nothing is saved in the user settings
            // TODO: handle some defaults preferences that can be set in the administration
            if (preferences.isEmpty() || (types.isEmpty() && apps.isEmpty())) {
                return Collections.emptyList();
            }

            hql += " order by event.date DESC";

            // TODO: idea: handle the items of the watchlist too

            Query query = queryManager.createQuery(hql, Query.HQL);
            query.setOffset(offset);
            query.setLimit(limit);

            if (!types.isEmpty()) {
                query.bindValue("types", types);
            }

            if (!apps.isEmpty()) {
                query.bindValue("apps", apps);
            }

            return eventStream.searchEvents(query);

        } catch (Exception e) {
            throw new NotificationException("Fail to get the list of notifications.", e);
        }
    }

    @Override
    public List<NotificationPreference> getPreferences() throws NotificationException
    {
        return getPreferences(documentAccessBridge.getCurrentUserReference());
    }

    @Override
    public List<NotificationPreference> getPreferences(String userId) throws NotificationException
    {
        return getPreferences(documentReferenceResolver.resolve(userId));
    }

    @Override
    public XDOM render(Event event) throws NotificationException
    {
        try {
            return getDisplayer(event).renderNotification(event);
        } catch (Exception e) {
            throw new NotificationException("Failed to render the notification.", e);
        }
    }

    private NotificationDisplayer getDisplayer(Event event) throws ComponentLookupException
    {
        for (NotificationDisplayer displayer
                : componentManager.<NotificationDisplayer>getInstanceList(NotificationDisplayer.class)) {
            if (displayer == defaultDisplayer) {
                continue;
            }
            for (NotificationEvent ev : displayer.getSupportedEvents()) {
                if (ev.matches(event)) {
                    return displayer;
                }
            }
        }
        return defaultDisplayer;
    }

    private List<NotificationPreference> getPreferences(DocumentReference user) throws NotificationException
    {
       return modelBridge.getNotificationsPreferences(user);
    }
}
