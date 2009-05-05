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
 * 
 * @version $Id: $
 */
public class ActivityStreamPluginApi extends PluginApi<ActivityStreamPlugin>
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

    /**
     * Record in database the given {@link com.xpn.xwiki.plugin.activitystream.api.ActivityEvent}
     * event
     * 
     * @param event the event to record in databases
     * @throws ActivityStreamException
     */
    public void addActivityEvent(com.xpn.xwiki.plugin.activitystream.api.ActivityEvent event)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            getActivityStream().addActivityEvent(event, context);
        }
    }

    /**
     * Records in database the a event built upon the passed parameters
     * 
     * @param streamName the name of the stream to record the event for
     * @param type the type of event. The type can be a value picked from the list of values defined
     *            in {@link com.xpn.xwiki.plugin.activitystream.api.ActivityEventType} or any other
     *            String
     * @param title the event title
     * @throws ActivityStreamException
     */
    public void addActivityEvent(String streamName, String type, String title)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            getActivityStream().addActivityEvent(streamName, type, title, context);
        }
    }

    /**
     * @see #addActivityEvent(String, String, String)
     * @param params a list of up to 5 "free" String parameters that will be associated with the
     *            event
     */
    public void addActivityEvent(String streamName, String type, String title, List params)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            getActivityStream().addActivityEvent(streamName, type, title, params, context);
        }
    }

    /**
     * Records in database an event built upon the passed document and other parameters. The passed
     * document is used to retrieve document-related data for the event : date, page name, database.
     * 
     * @param streamName the name of the stream to add the event to
     * @param doc the document from which to retrieve document data for the event
     * @param type the type of event. (see {@link #addActivityEvent(String, String, String)}
     * @param title the title of the event
     * @throws ActivityStreamException
     */
    public void addDocumentActivityEvent(String streamName, Document doc, String type,
        String title) throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            getActivityStream().addDocumentActivityEvent(streamName, doc.getDocument(), type,
                title, context);
        }
    }

    /**
     * @see #addDocumentActivityEvent(String, Document, String, String)
     * @param priority the priority for this event. see
     *            {@link com.xpn.xwiki.plugin.activitystream.api.ActivityEventPriority}
     * @throws ActivityStreamException
     */
    public void addDocumentActivityEvent(String streamName, Document doc, String type,
        int priority, String title) throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            getActivityStream().addDocumentActivityEvent(streamName, doc.getDocument(), type,
                priority, title, context);
        }
    }

    /**
     * Delete the passed events from the database.
     * 
     * @param evs the events to be deleted
     */
    public void deleteActivityEvents(List<ActivityEvent> evs) throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            List<com.xpn.xwiki.plugin.activitystream.api.ActivityEvent> events =
                unwrapEvents(evs);
            {
                for (com.xpn.xwiki.plugin.activitystream.api.ActivityEvent ev : events) {
                    getActivityStream().deleteActivityEvent(ev, context);
                }
            }
        }
    }

    /**
     * Delete the passed event form the database.
     * 
     * @param event the event to delete from database
     */
    public void deleteActivityEvent(ActivityEvent event) throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            getActivityStream().deleteActivityEvent(event.getEvent(), context);
        }
    }

    /**
     * @see #addDocumentActivityEvent(String, Document, String, String)
     * @param params a list of up to 5 "free" String parameters that will be associated with the
     *            event
     * @throws ActivityStreamException
     */
    public void addDocumentActivityEvent(String streamName, Document doc, String type,
        String title, List params) throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            getActivityStream().addDocumentActivityEvent(streamName, doc.getDocument(), type,
                title, params, context);
        }
    }

    /**
     * @see #addDocumentActivityEvent(String, Document, String, int, String)
     * @see #addDocumentActivityEvent(String, Document, String, String, List)
     */
    public void addDocumentActivityEvent(String streamName, Document doc, String type,
        int priority, String title, List params) throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            getActivityStream().addDocumentActivityEvent(streamName, doc.getDocument(), type,
                priority, title, params, context);
        }
    }

    /**
     * Search in database activity events matching the given hql query. Retrieve events are ordered
     * by date descending.
     * 
     * @param hql the "where" clause of the hql query to look events for
     * @param filter if true, group the matched events by priority
     * @param nb the number of events to retrieve
     * @param start the offset to start retrieving event at
     * @return a list of matching events, wrapped as
     *         {@link com.xpn.xwiki.plugin.activitystream.plugin.ActivityEvent} objects.
     * @throws ActivityStreamException
     */
    public List<ActivityEvent> searchEvents(String hql, boolean filter, int nb, int start)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            return wrapEvents(getActivityStream().searchEvents(hql, filter, nb, start, context));
        } else {
            return null;
        }
    }

    /**
     * Search in database activity events matching the given hql query. Retrieve events are ordered
     * by date descending.
     * 
     * @param fromHql the "from" clause of the hql query to look events for
     * @param hql the "where" clause of the hql query to look events for
     * @param filter if true, group the matched events by priority
     * @param nb the number of events to retrieve
     * @param start the offset to start retrieving event at
     * @return a list of matching events, wrapped as
     *         {@link com.xpn.xwiki.plugin.activitystream.plugin.ActivityEvent} objects.
     * @throws ActivityStreamException
     */
    public List<ActivityEvent> searchEvents(String fromHql, String hql, boolean filter, int nb, int start)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            return wrapEvents(getActivityStream().searchEvents(fromHql, hql, filter, nb, start, context));
        } else {
            return null;
        }
    }

    /**
     * Return the latest recorded events
     * 
     * @param filter if true, group the matched events by priority
     * @param nb the number of events to retrieve
     * @param start the offset to start retrieving event at
     * @throws ActivityStreamException
     */
    public List<ActivityEvent> getEvents(boolean filter, int nb, int start)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            return wrapEvents(getActivityStream().getEvents(filter, nb, start, context));
        } else {
            return null;
        }
    }

    /**
     * Return the latest recorded events for the given wiki space
     * 
     * @see #getEvents(boolean, int, int)
     * @param space the space to retrieve latest events for
     * @throws ActivityStreamException
     */
    public List<ActivityEvent> getEventsForSpace(String space, boolean filter, int nb, int start)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            return wrapEvents(getActivityStream().getEventsForSpace(space, filter, nb, start,
                context));
        } else {
            return null;
        }
    }

    /**
     * Return the latest recorded events triggered by the given user.
     * 
     * @see #getEvents(boolean, int, int)
     * @param user the user to retrieve latest events for
     * @throws ActivityStreamException
     */
    public List<ActivityEvent> getEventsForUser(String user, boolean filter, int nb, int start)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            return wrapEvents(getActivityStream().getEventsForUser(user, filter, nb, start,
                context));
        } else {
            return null;
        }
    }

    /**
     * Return the latest events recorded for the given stream name
     * 
     * @see #getEvents(boolean, int, int)
     * @param streamName the name of the stream to retrieve latest events for
     * @throws ActivityStreamException
     */
    public List<ActivityEvent> getEvents(String streamName, boolean filter, int nb, int start)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            return wrapEvents(getActivityStream().getEvents(streamName, filter, nb, start,
                context));
        } else {
            return null;
        }
    }

    /**
     * Returns the latest events recorded for the given stream name and wiki space name
     * 
     * @see #getEventsForSpace(String, boolean, int, int)
     * @see #getEvents(String, boolean, int, int)
     */
    public List<ActivityEvent> getEventsForSpace(String streamName, String space, boolean filter,
        int nb, int start) throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            return wrapEvents(getActivityStream().getEventsForSpace(streamName, space, filter,
                nb, start, context));
        } else {
            return null;
        }
    }

    /**
     * Returns the latest events recorded for the given stream name and user name
     * 
     * @see #getEventsForUser(String, boolean, int, int)
     * @see #getEvents(String, boolean, int, int)
     */
    public List<ActivityEvent> getEventsForUser(String streamName, String user, boolean filter,
        int nb, int start) throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            return wrapEvents(getActivityStream().getEventsForUser(streamName, user, filter, nb,
                start, context));
        } else {
            return null;
        }
    }

    protected List<ActivityEvent> wrapEvents(
        List<com.xpn.xwiki.plugin.activitystream.api.ActivityEvent> events)
    {
        List<ActivityEvent> result = new ArrayList<ActivityEvent>();
        if (events != null) {
            for (com.xpn.xwiki.plugin.activitystream.api.ActivityEvent event : events) {
                com.xpn.xwiki.plugin.activitystream.plugin.ActivityEvent wrappedEvent =
                    new com.xpn.xwiki.plugin.activitystream.plugin.ActivityEvent(event,
                        getXWikiContext());
                result.add(wrappedEvent);
            }
        }
        return result;
    }

    protected List<com.xpn.xwiki.plugin.activitystream.api.ActivityEvent> unwrapEvents(
        List<ActivityEvent> events)
    {
        List<com.xpn.xwiki.plugin.activitystream.api.ActivityEvent> result =
            new ArrayList<com.xpn.xwiki.plugin.activitystream.api.ActivityEvent>();
        if (events != null) {
            for (ActivityEvent event : events) {
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

    public SyndEntry getFeedEntry(com.xpn.xwiki.plugin.activitystream.plugin.ActivityEvent event, String suffix)
    {
        return getActivityStream().getFeedEntry(event.getEvent(), suffix, context);
    }

    public SyndFeed getFeed(List<ActivityEvent> events)
    {
        return getActivityStream().getFeed(unwrapEvents(events), context);
    }

    public SyndFeed getFeed(List<ActivityEvent> events, String suffix)
    {
        return getActivityStream().getFeed(unwrapEvents(events), suffix, context);
    }

    public SyndFeed getFeed(List<ActivityEvent> events, String author, String title,
        String description, String copyright, String encoding, String url)
    {
        return getActivityStream().getFeed(unwrapEvents(events), author, title, description,
            copyright, encoding, url, context);
    }

    public SyndFeed getFeed(List<ActivityEvent> events, String author, String title,
            String description, String copyright, String encoding, String url, String suffix)
        {
            return getActivityStream().getFeed(unwrapEvents(events), author, title, description,
                copyright, encoding, url, suffix, context);
        }

    public String getFeedOutput(List<ActivityEvent> events, String author, String title,
        String description, String copyright, String encoding, String url, String type)
    {
        return getActivityStream().getFeedOutput(unwrapEvents(events), author, title,
            description, copyright, encoding, url, type, context);
    }

    public String getFeedOutput(List<ActivityEvent> events, String author, String title,
            String description, String copyright, String encoding, String url, String type, String suffix)
        {
            return getActivityStream().getFeedOutput(unwrapEvents(events), author, title,
                description, copyright, encoding, url, type, suffix, context);
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
