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
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.GroupingEventManager;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.internal.email.AbstractMimeMessageIterator;
import org.xwiki.notifications.preferences.NotificationEmailInterval;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

/**
 * Default implementation for {@link PrefilteringMimeMessageIterator}.
 *
 * @since 12.6
 * @version $Id$
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultPrefilteringLiveMimeMessageIterator extends AbstractMimeMessageIterator
    implements PrefilteringMimeMessageIterator
{
    @Inject
    private GroupingEventManager groupingEventManager;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @Inject
    private LiveNotificationEmailEventFilter eventFilter;

    private Map<DocumentReference, List<Event>> events;

    @Override
    public void initialize(Map<DocumentReference, List<Event>> events, Map<String, Object> factoryParameters,
        EntityReference templateReference)
    {
        this.events = events;
        super.initialize(events.keySet().iterator(), factoryParameters, templateReference,
                NotificationEmailInterval.LIVE);
    }

    /**
     * For the given user, we will have to check that the composite event that we have to send matches the user
     * preferences. If, for any reason, one of the events of the original composite event is not meant for the user, we
     * clone the original composite event and remove the incriminated event.
     */
    @Override
    protected List<CompositeEvent> retrieveCompositeEventList(DocumentReference user) throws NotificationException
    {
        List<Event> eventList = this.events.get(user);
        UserReference userReference = this.userReferenceResolver.resolve(user);
        // FIXME: Should we use a dedicated target for live email?
        List<CompositeEvent> compositeEvents =
            this.groupingEventManager.getCompositeEvents(eventList, userReference, "email");

        List<CompositeEvent> result = new ArrayList<>();

        // We perform the filtering here for optimization: it avoids checking each individual event one by one
        // We could in the future change the implementation to put directly the filtering in the GroupingEventManager
        for (CompositeEvent compositeEvent : compositeEvents) {
            if (this.eventFilter.canAccessEvent(user, compositeEvent)) {
                result.add(compositeEvent);
            }
        }
        return result;
    }
}
