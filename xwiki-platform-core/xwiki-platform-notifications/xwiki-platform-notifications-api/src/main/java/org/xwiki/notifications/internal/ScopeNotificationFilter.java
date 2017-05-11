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

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFilter;
import org.xwiki.notifications.NotificationPreferenceScope;

/**
 * Notification filter that handle the generic {@link NotificationPreferenceScope}.
 *
 * @version $Id$
 * @since 9.4RC1
 */
@Component
@Named("scope")
public class ScopeNotificationFilter implements NotificationFilter
{
    @Inject
    private ModelBridge modelBridge;

    @Inject
    private Logger logger;

    @Override
    public boolean filterEvent(Event event, DocumentReference user)
    {
        // Indicate if a restriction exist concerning this type of event
        boolean hasRestriction = false;
        // Indicate if a restriction matches the document of the event
        boolean matchRestriction = false;

        try {
            for (NotificationPreferenceScope scope : modelBridge.getNotificationPreferenceScopes(user)) {
                if (scope.getEventType().equals(event.getType())) {
                    hasRestriction = true;

                    if (event.getDocument().equals(scope.getScopeReference())
                            || event.getDocument().hasParent(scope.getScopeReference())) {
                        matchRestriction = true;
                        break;
                    }
                }
            }
        } catch (NotificationException e) {
            logger.warn("Failed to filter the notifications.", e);
        }

        return hasRestriction && !matchRestriction;
    }
}
