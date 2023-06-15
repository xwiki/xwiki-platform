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
package org.xwiki.notifications.preferences.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.notifications.preferences.internal.event.NotificationPreferenceAddedEvent;
import org.xwiki.notifications.preferences.internal.event.NotificationPreferenceDeletedEvent;
import org.xwiki.notifications.preferences.internal.event.NotificationPreferenceUpdatedEvent;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

/**
 * A listener used to invalidate the notification event cache when a new event is stored.
 * 
 * @version $Id$
 * @since 13.4.4
 * @since 12.10.10
 * @since 13.8RC1
 */
@Component
@Singleton
@Named(CachedModelBridgeInvalidatorListener.NAME)
public class CachedModelBridgeInvalidatorListener extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME =
        "org.xwiki.notifications.preferences.internal.CachedModelBridgeInvalidatorListener";

    @Inject
    @Named("cached")
    private NotificationPreferenceModelBridge bridge;

    /**
     * The default constructor.
     */
    public CachedModelBridgeInvalidatorListener()
    {
        super(NAME,
            new NotificationPreferenceAddedEvent(),
            new NotificationPreferenceUpdatedEvent(),
            new NotificationPreferenceDeletedEvent()
        );
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Assume it's a wiki reference
        ((CachedNotificationPreferenceModelBridge) this.bridge).invalidatePreference((EntityReference) source);
    }
}
