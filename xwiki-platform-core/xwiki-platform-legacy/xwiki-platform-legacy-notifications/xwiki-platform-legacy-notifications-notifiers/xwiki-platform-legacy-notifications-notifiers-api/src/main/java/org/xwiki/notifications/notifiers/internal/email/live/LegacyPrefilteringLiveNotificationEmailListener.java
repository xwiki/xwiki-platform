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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.notifications.NotificationConfiguration;
import org.xwiki.observation.event.Event;

/**
 * This listener is responsible of sending mails to users who enabled live mails.
 *
 * @since 15.5RC1
 * @version $Id$
 */
@Component
@Singleton
@Named(LegacyPrefilteringLiveNotificationEmailListener.NAME)
public class LegacyPrefilteringLiveNotificationEmailListener extends PrefilteringLiveNotificationEmailListener
{
    @Inject
    private NotificationConfiguration configuration;

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (this.configuration.isEventPrefilteringEnabled()) {
            super.onEvent(event, source, data);
        }
    }
}
