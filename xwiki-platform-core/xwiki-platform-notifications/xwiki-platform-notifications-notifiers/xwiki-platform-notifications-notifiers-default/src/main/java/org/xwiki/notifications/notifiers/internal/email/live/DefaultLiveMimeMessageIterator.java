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
package org.xwiki.notifications.notifiers.internal.email.live;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.notifiers.internal.email.AbstractMimeMessageIterator;
import org.xwiki.notifications.notifiers.internal.email.NotificationUserIterator;

/**
 * Default implementation for {@link LiveMimeMessageIterator}.
 *
 * @since 9.10RC1
 * @version $Id$
 * @deprecated This component is only used in case of post-filtering events. We stopped supporting those.
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
@Deprecated(since = "15.5RC1")
public class DefaultLiveMimeMessageIterator extends AbstractMimeMessageIterator
    implements LiveMimeMessageIterator
{
    private CompositeEvent compositeEvent;

    @Inject
    private NotificationFilterManager notificationFilterManager;

    @Inject
    private LiveNotificationEmailEventFilter eventFilter;

    @Override
    public void initialize(NotificationUserIterator userIterator, Map<String, Object> factoryParameters,
            CompositeEvent event, DocumentReference templateReference)
    {
        this.compositeEvent = event;
        super.initialize(userIterator, factoryParameters, templateReference);
    }

    /**
     * For the given user, we will have to check that the composite event that we have to send matches the user
     * preferences.
     *
     * If, for any reason, one of the events of the original composite event is not meant for the user, we clone
     * the original composite event and remove the incriminated event.
     */
    @Override
    protected List<CompositeEvent> retrieveCompositeEventList(DocumentReference user) throws NotificationException
    {
        CompositeEvent resultCompositeEvent = new CompositeEvent(this.compositeEvent);

        // TODO: handle followed user for who we don't cate about the notification preference, we just want to receive
        // all actions the person is doing

        if (this.eventFilter.canAccessEvent(user, resultCompositeEvent)
            && this.eventFilter.isCompositeEventHandled(user, resultCompositeEvent)) {
            // Apply the filters that the user has defined in its notification preferences
            // If one of the events present in the composite event does not match a user filter, remove the event
            List<NotificationFilter> filters
                    = new ArrayList<>(notificationFilterManager.getAllFilters(user, true));
            Collections.sort(filters);
            Iterator<Event> it = resultCompositeEvent.getEvents().iterator();
            while (it.hasNext()) {
                Event event = it.next();
                if (this.eventFilter.isEventFiltered(filters, event, user)) {
                    it.remove();
                }
            }
            if (resultCompositeEvent.getEvents().size() == 0) {
                return Collections.emptyList();
            }

            return Collections.singletonList(resultCompositeEvent);
        }

        return Collections.emptyList();
    }
}
