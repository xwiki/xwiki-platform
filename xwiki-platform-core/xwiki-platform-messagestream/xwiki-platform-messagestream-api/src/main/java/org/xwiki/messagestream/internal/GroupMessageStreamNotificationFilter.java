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
package org.xwiki.messagestream.internal;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.xpn.xwiki.api.User;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.messagestream.GroupMessageDescriptor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;

import com.xpn.xwiki.XWikiContext;

/**
 * Filter that make sure a group message (to a group) from the message stream is visible only to members of this group.
 *
 * @version $Id$
 * @since 12.10
 */
@Component
@Singleton
@Named("GroupMessageStreamNotificationFilter")
public class GroupMessageStreamNotificationFilter extends AbstractMessageStreamNotificationFilter
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public FilterPolicy filterEvent(Event event, DocumentReference userDocumentReference,
        Collection<NotificationFilterPreference> filterPreferences, NotificationFormat format)
    {
        FilterPolicy ret;
        // Don't handle events that are not group messages.
        if (!getEventType().equals(event.getType())) {
            ret = FilterPolicy.NO_EFFECT;
        } else if (userDocumentReference != null) {
            XWikiContext xWikiContext = this.xcontextProvider.get();
            // If the user is not in the messaged group, the event is filtered.
            // If the user is in the group, we let it pass and let downstream filter analyze the event.
            User user = xWikiContext.getWiki().getUser(userDocumentReference, xWikiContext);
            if (user == null) {
                ret = FilterPolicy.NO_EFFECT;
            } else {
                boolean userInGroup = user.isUserInGroup(event.getStream());
                if (!userInGroup) {
                    ret = FilterPolicy.FILTER;
                } else {
                    ret = FilterPolicy.NO_EFFECT;
                }
            }
        } else {
            ret = FilterPolicy.FILTER;
        }

        return ret;
    }

    @Override
    public String getName()
    {
        return "Group Message Stream Notification Filter";
    }

    @Override
    String getEventType()
    {
        return GroupMessageDescriptor.EVENT_TYPE;
    }

    @Override
    public int getPriority()
    {
        // Makes the priority higher than org.xwiki.notifications.filters.internal.user.EventUserFilter.getPriority, to
        // force the group memberships to be checked before other filters.
        // EventUserFilter in particular because it keeps events if they are send by followers of the sender, even
        // if the follower is not part of the targeted group in our context (and consequently, skipping this filter).
        return 2001;
    }
}
