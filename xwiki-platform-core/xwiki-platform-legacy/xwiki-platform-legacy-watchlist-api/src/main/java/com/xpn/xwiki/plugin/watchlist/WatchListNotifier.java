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
package com.xpn.xwiki.plugin.watchlist;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.Utils;

/**
 * Utility class used by the watchlist plugin to send notifications to users. The current implementation offers email
 * notifications only.
 * 
 * @version $Id$
 */
@Deprecated
public class WatchListNotifier
{
    /**
     * Wiki page which contains the default watchlist email template.
     */
    public static final String DEFAULT_EMAIL_TEMPLATE = "XWiki.WatchListMessage";

    /**
     * XWiki User Class.
     */
    public static final String XWIKI_USER_CLASS = "XWiki.XWikiUsers";

    /**
     * XWiki User Class first name property name.
     */
    public static final String XWIKI_USER_CLASS_FIRST_NAME_PROP = "first_name";

    /**
     * XWiki User Class last name property name.
     */
    public static final String XWIKI_USER_CLASS_LAST_NAME_PROP = "last_name";

    /**
     * Sends the email notifying the subscriber that the updatedDocuments have been changed.
     * 
     * @param subscriber user to notify
     * @param events list of events
     * @param emailTemplate email template to use
     * @param previousFireTime last time the notification was fired
     * @param context the XWiki context
     * @throws XWikiException if mail sending fails
     */
    public void sendEmailNotification(String subscriber, List<WatchListEvent> events, String emailTemplate,
        Date previousFireTime, XWikiContext context) throws XWikiException
    {
        // Convert the parameters.
        List<org.xwiki.watchlist.internal.api.WatchListEvent> watchListEvents = new ArrayList<>();
        for (WatchListEvent event : events) {
            // Possibly composite event.
            org.xwiki.watchlist.internal.api.WatchListEvent watchListEvent =
                new org.xwiki.watchlist.internal.api.WatchListEvent(event.getData(), context);

            watchListEvents.add(watchListEvent);
        }

        // Delegate to the component.
        org.xwiki.watchlist.internal.api.WatchListNotifier notifier =
            Utils.getComponent(org.xwiki.watchlist.internal.api.WatchListNotifier.class);
        notifier.sendNotification(subscriber, watchListEvents, emailTemplate, previousFireTime);
    }
}
