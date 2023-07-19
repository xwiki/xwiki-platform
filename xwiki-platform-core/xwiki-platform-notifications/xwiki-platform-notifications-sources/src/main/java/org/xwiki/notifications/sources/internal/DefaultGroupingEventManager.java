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
package org.xwiki.notifications.sources.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.eventstream.Event;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.GroupingEventManager;
import org.xwiki.notifications.GroupingEventStrategy;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.user.UserReference;

/**
 * Default implementation of {@link GroupingEventManager}.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Component
@Singleton
public class DefaultGroupingEventManager implements GroupingEventManager
{
    @Inject
    private GroupingEventStrategy defaultGroupingEventStrategy;

    @Inject
    private NotificationPreferenceManager notificationPreferenceManager;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    @Override
    public List<CompositeEvent> getCompositeEvents(List<Event> events, UserReference userReference, String target)
            throws NotificationException
    {
        return getStrategy(userReference, target).group(events);
    }

    @Override
    public void augmentCompositeEvents(List<CompositeEvent> compositeEvents, List<Event> newEvents,
                                       UserReference userReference, String target) throws NotificationException
    {
        getStrategy(userReference, target).group(compositeEvents, newEvents);
    }

    private GroupingEventStrategy getStrategy(UserReference userReference, String target) throws NotificationException
    {
        GroupingEventStrategy groupingEventStrategy = this.defaultGroupingEventStrategy;

        // FIXME: We should have a way to fallback on wiki preference
        if (userReference != null) {
            String strategyHint =
                this.notificationPreferenceManager.getNotificationGroupingStrategy(userReference, target);

            if (this.componentManager.hasComponent(GroupingEventStrategy.class, strategyHint)) {
                try {
                    groupingEventStrategy =
                        this.componentManager.getInstance(GroupingEventStrategy.class, strategyHint);
                } catch (ComponentLookupException e) {
                    this.logger.error("Error when getting grouping event strategy instance with hint [{}]. "
                            + "It will fallback on default strategy. Root cause: [{}].", strategyHint,
                        ExceptionUtils.getRootCauseMessage(e));
                }
            } else {
                this.logger.warn(
                    "No grouping event strategy found with hint [{}]. It will fallback on default strategy.",
                    strategyHint);
            }
        }
        return groupingEventStrategy;
    }
}
