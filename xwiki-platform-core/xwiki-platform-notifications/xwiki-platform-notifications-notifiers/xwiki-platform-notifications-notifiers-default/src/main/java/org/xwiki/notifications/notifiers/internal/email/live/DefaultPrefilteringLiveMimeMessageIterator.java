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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.internal.email.AbstractMimeMessageIterator;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Default implementation for {@link PrefilteringMimeMessageIterator}.
 *
 * @since 12.6RC1
 * @version $Id$
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultPrefilteringLiveMimeMessageIterator extends AbstractMimeMessageIterator
    implements PrefilteringMimeMessageIterator
{
    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private Map<DocumentReference, CompositeEvent> events;

    @Override
    public void initialize(Map<DocumentReference, CompositeEvent> events, Map<String, Object> factoryParameters,
        EntityReference templateReference)
    {
        super.initialize(events.keySet().iterator(), factoryParameters, templateReference);

        this.events = events;
    }

    /**
     * For the given user, we will have to check that the composite event that we have to send matches the user
     * preferences. If, for any reason, one of the events of the original composite event is not meant for the user, we
     * clone the original composite event and remove the incriminated event.
     */
    @Override
    protected List<CompositeEvent> retrieveCompositeEventList(DocumentReference user) throws NotificationException
    {
        CompositeEvent event = this.events.get(user);

        if (canAccessEvent(user, event)) {
            return Collections.singletonList(event);
        }

        return Collections.emptyList();
    }

    private boolean canAccessEvent(DocumentReference user, CompositeEvent event)
    {
        DocumentReference document = event.getDocument();
        return (document != null && authorizationManager.hasAccess(Right.VIEW, user, document));
    }
}
