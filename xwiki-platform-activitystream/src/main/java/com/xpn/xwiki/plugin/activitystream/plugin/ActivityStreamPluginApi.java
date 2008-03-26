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
package com.xpn.xwiki.plugin.activitystream.plugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.plugin.PluginApi;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.plugin.activitystream.api.ActivityStream;
import com.xpn.xwiki.plugin.activitystream.api.ActivityStreamException;

/**
 * API for {@link ActivityStreamPlugin}
 */
public class ActivityStreamPluginApi extends PluginApi
{
    /**
     * @see PluginApi#PluginApi(XWikiPluginInterface, XWikiContext)
     */
    public ActivityStreamPluginApi(ActivityStreamPlugin plugin, XWikiContext context)
    {
        super(plugin, context);
    }

    /**
     * @return The {@link ActivityStream} component to use inside the API
     */
    protected ActivityStream getActivityStream()
    {
        return ((ActivityStreamPlugin) getProtectedPlugin()).getActivityStream();
    }

    public void addActivityEvent(com.xpn.xwiki.plugin.activitystream.api.ActivityEvent event)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            getActivityStream().addActivityEvent(event, context);
        }
    }

    public void addActivityEvent(String streamName, String type, String title)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            getActivityStream().addActivityEvent(streamName, type, title, context);
        }
    }

    public void addDocumentActivityEvent(String streamName, Document doc, String type,
        String title) throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            getActivityStream().addDocumentActivityEvent(streamName, doc.getDocument(), type,
                title, context);
        }
    }

    public void addDocumentActivityEvent(String streamName, Document doc, String type,
        int priority, String title) throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            getActivityStream().addDocumentActivityEvent(streamName, doc.getDocument(), type,
                priority, title, context);
        }
    }

    public void addActivityEvent(String streamName, String type, String title, List params)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            getActivityStream().addActivityEvent(streamName, type, title, params, context);
        }
    }

    public void addDocumentActivityEvent(String streamName, Document doc, String type,
        String title, List params) throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            getActivityStream().addDocumentActivityEvent(streamName, doc.getDocument(), type,
                title, params, context);
        }
    }

    public void addDocumentActivityEvent(String streamName, Document doc, String type,
        int priority, String title, List params) throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            getActivityStream().addDocumentActivityEvent(streamName, doc.getDocument(), type,
                priority, title, params, context);
        }
    }

    public List searchEvents(String hql, boolean filter, int nb, int start)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            return wrapEvents(getActivityStream().searchEvents(hql, filter, nb, start, context));
        } else {
            return null;
        }
    }

    public List getEvents(boolean filter, int nb, int start) throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            return wrapEvents(getActivityStream().getEvents(filter, nb, start, context));
        } else {
            return null;
        }
    }

    public List getEventsForSpace(String space, boolean filter, int nb, int start)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            return wrapEvents(getActivityStream().getEventsForSpace(space, filter, nb, start,
                context));
        } else {
            return null;
        }
    }

    public List getEventsForUser(String user, boolean filter, int nb, int start)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            return wrapEvents(getActivityStream().getEventsForUser(user, filter, nb, start,
                context));
        } else {
            return null;
        }
    }

    public List getEvents(String streamName, boolean filter, int nb, int start)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            return wrapEvents(getActivityStream().getEvents(streamName, filter, nb, start,
                context));
        } else {
            return null;
        }
    }

    public List getEventsForSpace(String streamName, String space, boolean filter, int nb,
        int start) throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            return wrapEvents(getActivityStream().getEventsForSpace(streamName, space, filter,
                nb, start, context));
        } else {
            return null;
        }
    }

    public List getEventsForUser(String streamName, String user, boolean filter, int nb, int start)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            return wrapEvents(getActivityStream().getEventsForUser(streamName, user, filter, nb,
                start, context));
        } else {
            return null;
        }
    }

    protected List wrapEvents(List events)
    {
        List result = new ArrayList();
        if (events != null) {
            for (Iterator iter = events.iterator(); iter.hasNext();) {
                Object obj = iter.next();
                com.xpn.xwiki.plugin.activitystream.api.ActivityEvent event =
                    (com.xpn.xwiki.plugin.activitystream.api.ActivityEvent) obj;
                com.xpn.xwiki.plugin.activitystream.plugin.ActivityEvent wrappedEvent =
                    new com.xpn.xwiki.plugin.activitystream.plugin.ActivityEvent(event,
                        getXWikiContext());
                result.add(wrappedEvent);
            }
        }
        return result;
    }

    protected List unwrapEvents(List events)
    {
        List result = new ArrayList();
        if (events != null) {
            for (Iterator iter = events.iterator(); iter.hasNext();) {
                Object obj = iter.next();
                com.xpn.xwiki.plugin.activitystream.plugin.ActivityEvent event =
                    (com.xpn.xwiki.plugin.activitystream.plugin.ActivityEvent) obj;
                com.xpn.xwiki.plugin.activitystream.api.ActivityEvent unwrappedEvent =
                    event.getEvent();
                result.add(unwrappedEvent);
            }
        }
        return result;
    }

    public SyndEntry getFeedEntry(com.xpn.xwiki.plugin.activitystream.plugin.ActivityEvent event)
    {
        return getActivityStream().getFeedEntry(event.getEvent(), context);
    }

    public SyndFeed getFeed(List events)
    {
        return getActivityStream().getFeed(unwrapEvents(events), context);
    }

    public SyndFeed getFeed(List events, String author, String title, String description,
        String copyright, String encoding, String url)
    {
        return getActivityStream().getFeed(unwrapEvents(events), author, title, description,
            copyright, encoding, url, context);
    }

    public String getFeedOutput(List events, String author, String title, String description,
        String copyright, String encoding, String url, String type)
    {
        return getActivityStream().getFeedOutput(unwrapEvents(events), author, title,
            description, copyright, encoding, url, type, context);
    }

    public String getFeedOutput(SyndFeed feed, String type)
    {
        return getActivityStream().getFeedOutput(feed, type);
    }

    /**
     * @return The name of the event stream associated with the given space
     */
    public String getStreamName(String spaceName)
    {
        return getActivityStream().getStreamName(spaceName, context);
    }
}
