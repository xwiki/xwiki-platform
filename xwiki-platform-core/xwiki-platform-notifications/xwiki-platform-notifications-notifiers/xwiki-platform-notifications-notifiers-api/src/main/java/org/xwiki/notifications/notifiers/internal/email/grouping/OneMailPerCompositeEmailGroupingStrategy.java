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
package org.xwiki.notifications.notifiers.internal.email.grouping;

import org.xwiki.component.annotation.Component;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.email.NotificationEmailGroupingStrategy;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This component offers a strategy for sending one email per composite event to notify to the user.
 * This strategy can be used in combination with
 * {@link org.xwiki.notifications.internal.ApplicationTypeGroupingStrategy} to send as many emails as there is type of
 * events to notify the users. It could also be used with the default event grouping strategy so that users received as
 * many emails as there is group of events.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Component
@Singleton
@Named("emailperevent")
public class OneMailPerCompositeEmailGroupingStrategy implements NotificationEmailGroupingStrategy
{
    @Override
    public List<List<CompositeEvent>> groupEventsPerMail(List<CompositeEvent> compositeEvents)
            throws NotificationException
    {
        return compositeEvents.stream().map(List::of).collect(Collectors.toList());
    }
}
