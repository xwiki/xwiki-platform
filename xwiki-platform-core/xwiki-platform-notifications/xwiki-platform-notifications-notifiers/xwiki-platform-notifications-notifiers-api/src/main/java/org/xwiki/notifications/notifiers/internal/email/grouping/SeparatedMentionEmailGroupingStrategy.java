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

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.email.NotificationEmailGroupingStrategy;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 * This component offers a strategy where the notifications related to mentions are sent in a separated email than all
 * others notifications. So two emails are sent in case of mentions: one for the mentions, and another one for other
 * events.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Component
@Singleton
@Named("separatedmention")
public class SeparatedMentionEmailGroupingStrategy implements NotificationEmailGroupingStrategy
{
    @Override
    public List<List<CompositeEvent>> groupEventsPerMail(List<CompositeEvent> compositeEvents)
            throws NotificationException
    {
        List<CompositeEvent> otherEvents = new ArrayList<>();
        List<CompositeEvent> mention = new ArrayList<>();
        List<List<CompositeEvent>> result = new ArrayList<>();

        for (CompositeEvent compositeEvent : compositeEvents) {
            if (StringUtils.equals("mention", compositeEvent.getType())) {
                mention.add(compositeEvent);
            } else {
                otherEvents.add(compositeEvent);
            }
        }

        if (!otherEvents.isEmpty()) {
            result.add(otherEvents);
        }
        if (!mention.isEmpty()) {
            result.add(mention);
        }
        return result;
    }
}
