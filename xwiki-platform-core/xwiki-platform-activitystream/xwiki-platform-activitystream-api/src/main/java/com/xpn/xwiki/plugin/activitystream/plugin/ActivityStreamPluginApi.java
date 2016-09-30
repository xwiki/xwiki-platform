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
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.plugin.PluginApi;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.plugin.activitystream.api.ActivityStream;
import com.xpn.xwiki.plugin.activitystream.api.ActivityStreamException;

/**
 * API for {@link ActivityStreamPlugin}.
 *
 * @version $Id$
 */
public class ActivityStreamPluginApi extends PluginApi<ActivityStreamPlugin>
{
    /** Logging helper object. */
    private static final Logger LOG = LoggerFactory.getLogger(ActivityStreamPlugin.class);

    /**
     * Constructor.
     *
     * @see PluginApi#PluginApi(XWikiPluginInterface, XWikiContext)
     * @param plugin plugin to wrap
     * @param context the XWiki context
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
        return getProtectedPlugin().getActivityStream();
    }

    /**
     * Record in database the given {@link com.xpn.xwiki.plugin.activitystream.api.ActivityEvent} event.
     *
     * @param event the event to record in databases
     * @throws ActivityStreamException if the event addition fails
     */
    public void addActivityEvent(com.xpn.xwiki.plugin.activitystream.api.ActivityEvent event)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            getActivityStream().addActivityEvent(event, this.context);
        }
    }

    /**
     * Records in database the a event built upon the passed parameters.
     *
     * @param streamName the name of the stream to record the event for
     * @param type the type of event. The type can be a value picked from the list of values defined in
     *            {@link com.xpn.xwiki.plugin.activitystream.api.ActivityEventType} or any other String
     * @param title the event title
     * @throws ActivityStreamException if the event addition fails
     */
    public void addActivityEvent(String streamName, String type, String title) throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            getActivityStream().addActivityEvent(streamName, type, title, this.context);
        }
    }

    /**
     * Records in database an event built upon the passed document and other parameters. The passed document is used to
     * retrieve document-related data for the event : date, page name, database.
     *
     * @param streamName name of the stream to use for the addition
     * @param type type of the event
     * @param title title of the event
     * @param params parameters of the event
     * @throws ActivityStreamException if the addition to the stream fails
     */
    public void addActivityEvent(String streamName, String type, String title, List<String> params)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            getActivityStream().addActivityEvent(streamName, type, title, params, this.context);
        }
    }

    /**
     * Records in database an event built upon the passed document and other parameters. The passed document is used to
     * retrieve document-related data for the event : date, page name, database.
     *
     * @param streamName the name of the stream to add the event to
     * @param doc the document from which to retrieve document data for the event
     * @param type the type of event. (see {@link #addActivityEvent(String, String, String)}
     * @param title the title of the event
     * @throws ActivityStreamException if the event addition fails
     */
    public void addDocumentActivityEvent(String streamName, Document doc, String type, String title)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            getActivityStream().addDocumentActivityEvent(streamName, doc.getDocument(), type, title, this.context);
        }
    }

    /**
     * Records in database an event built upon the passed document and other parameters. The passed document is used to
     * retrieve document-related data for the event : date, page name, database.
     *
     * @param streamName name of the stream to use for the addition
     * @param doc which fired the event
     * @param type type of the event
     * @param priority priority of the event
     * @param title title of the event
     * @throws ActivityStreamException if the addition to the stream fails
     */
    public void addDocumentActivityEvent(String streamName, Document doc, String type, int priority, String title)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            getActivityStream().addDocumentActivityEvent(streamName, doc.getDocument(), type, priority, title,
                this.context);
        }
    }

    /**
     * Delete the passed events from the database.
     *
     * @param evs the events to be deleted
     * @throws ActivityStreamException if the event deletion fails
     */
    public void deleteActivityEvents(List<ActivityEvent> evs) throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            List<com.xpn.xwiki.plugin.activitystream.api.ActivityEvent> events = unwrapEvents(evs);
            for (com.xpn.xwiki.plugin.activitystream.api.ActivityEvent ev : events) {
                getActivityStream().deleteActivityEvent(ev, this.context);
            }
        }
    }

    /**
     * Delete the passed event form the database.
     *
     * @param event the event to delete from database
     * @throws ActivityStreamException if the event deletion fails
     */
    public void deleteActivityEvent(ActivityEvent event) throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            getActivityStream().deleteActivityEvent(event.getEvent(), this.context);
        }
    }

    /**
     * Records in database an event built upon the passed document and other parameters. The passed document is used to
     * retrieve document-related data for the event : date, page name, database.
     *
     * @param streamName name of the stream to use for the addition
     * @param doc which fired the event
     * @param type type of the event
     * @param title title of the event
     * @param params parameters of the event
     * @throws ActivityStreamException if the addition to the stream fails
     */
    public void addDocumentActivityEvent(String streamName, Document doc, String type, String title,
        List<String> params) throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            getActivityStream().addDocumentActivityEvent(streamName, doc.getDocument(), type, title, params,
                this.context);
        }
    }

    /**
     * Records in database an event built upon the passed document and other parameters. The passed document is used to
     * retrieve document-related data for the event : date, page name, database.
     *
     * @param streamName name of the stream to use for the addition
     * @param doc which fired the event
     * @param type type of the event
     * @param priority priority of the event
     * @param title title of the event
     * @param params parameters of the event
     * @throws ActivityStreamException if the addition to the stream fails
     */
    public void addDocumentActivityEvent(String streamName, Document doc, String type, int priority, String title,
        List<String> params) throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            getActivityStream().addDocumentActivityEvent(streamName, doc.getDocument(), type, priority, title, params,
                this.context);
        }
    }

    /**
     * Search in database activity events matching the given hql query. Retrieved events are ordered by date descending.
     *
     * @param hql the "where" clause of the hql query to look events for
     * @param filter if true, group the matched events by priority
     * @param nb the number of events to retrieve
     * @param start the offset to start retrieving event at
     * @return a list of matching events, wrapped as {@link com.xpn.xwiki.plugin.activitystream.plugin.ActivityEvent}
     *         objects.
     * @throws ActivityStreamException if the search query fails
     */
    public List<ActivityEvent> searchEvents(String hql, boolean filter, int nb, int start)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            return wrapEvents(getActivityStream().searchEvents(hql, filter, nb, start, this.context));
        } else {
            return null;
        }
    }

    /**
     * Search in database activity events matching the given hql query. Retrieved events are ordered by date descending.
     *
     * @param hql the "where" clause of the hql query to look events for
     * @param filter if true, group the matched events by priority
     * @param globalSearch true if the request must be performed on the main database
     * @param nb the number of events to retrieve
     * @param start the offset to start retrieving event at
     * @return a list of matching events, wrapped as {@link com.xpn.xwiki.plugin.activitystream.plugin.ActivityEvent}
     *         objects.
     * @throws ActivityStreamException if the search query fails
     */
    public List<ActivityEvent> searchEvents(String hql, boolean filter, boolean globalSearch, int nb, int start)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            return wrapEvents(getActivityStream().searchEvents(hql, filter, globalSearch, nb, start, this.context));
        } else {
            return null;
        }
    }

    /**
     * Search in database activity events matching the given hql query. Retrieved events are ordered by date descending.
     *
     * @param hql the "where" clause of the hql query to look events for
     * @param filter if true, group the matched events by priority
     * @param nb the number of events to retrieve
     * @param start the offset to start retrieving event at
     * @param parameterValues list of parameters to insert in the query
     * @return a list of matching events, wrapped as {@link com.xpn.xwiki.plugin.activitystream.plugin.ActivityEvent}
     *         objects.
     * @throws ActivityStreamException if the search query fails
     */
    public List<ActivityEvent> searchEvents(String hql, boolean filter, int nb, int start, List<Object> parameterValues)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            return wrapEvents(getActivityStream().searchEvents("", hql, filter, nb, start, parameterValues,
                this.context));
        } else {
            return null;
        }
    }

    /**
     * Search in database activity events matching the given hql query. Retrieved events are ordered by date descending.
     *
     * @param hql the "where" clause of the hql query to look events for
     * @param filter if true, group the matched events by priority
     * @param globalSearch true if the request must be performed on the main database
     * @param nb the number of events to retrieve
     * @param start the offset to start retrieving event at
     * @param parameterValues list of parameters to insert in the query
     * @return a list of matching events, wrapped as {@link com.xpn.xwiki.plugin.activitystream.plugin.ActivityEvent}
     *         objects.
     * @throws ActivityStreamException if the search query fails
     */
    public List<ActivityEvent> searchEvents(String hql, boolean filter, boolean globalSearch, int nb, int start,
        List<Object> parameterValues) throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            return wrapEvents(getActivityStream().searchEvents("", hql, filter, globalSearch, nb, start,
                parameterValues, this.context));
        } else {
            return null;
        }
    }

    /**
     * Search in database activity events matching the given hql query. Retrieved events are ordered by date descending.
     *
     * @param fromHql the "from" clause of the hql query to look events for
     * @param hql the "where" clause of the hql query to look events for
     * @param filter if true, group the matched events by priority
     * @param nb the number of events to retrieve
     * @param start the offset to start retrieving event at
     * @return a list of matching events, wrapped as {@link com.xpn.xwiki.plugin.activitystream.plugin.ActivityEvent}
     *         objects.
     * @throws ActivityStreamException if the search query fails
     */
    public List<ActivityEvent> searchEvents(String fromHql, String hql, boolean filter, int nb, int start)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            return wrapEvents(getActivityStream().searchEvents(fromHql, hql, filter, nb, start, this.context));
        } else {
            return null;
        }
    }

    /**
     * Search in database activity events matching the given hql query. Retrieved events are ordered by date descending.
     *
     * @param fromHql the "from" clause of the hql query to look events for
     * @param hql the "where" clause of the hql query to look events for
     * @param filter if true, group the matched events by priority
     * @param globalSearch true if the request must be performed on the main database
     * @param nb the number of events to retrieve
     * @param start the offset to start retrieving event at
     * @return a list of matching events, wrapped as {@link com.xpn.xwiki.plugin.activitystream.plugin.ActivityEvent}
     *         objects.
     * @throws ActivityStreamException if the search query fails
     */
    public List<ActivityEvent> searchEvents(String fromHql, String hql, boolean filter, boolean globalSearch, int nb,
        int start) throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            return wrapEvents(getActivityStream().searchEvents(fromHql, hql, filter, globalSearch, nb, start,
                this.context));
        } else {
            return null;
        }
    }

    /**
     * Search in database activity events matching the given hql query. Retrieved events are ordered by date descending.
     *
     * @param fromHql the "from" clause of the hql query to look events for
     * @param hql the "where" clause of the hql query to look events for
     * @param filter if true, group the matched events by priority
     * @param nb the number of events to retrieve
     * @param start the offset to start retrieving event at
     * @param parameterValues list of parameters to insert in the query
     * @return a list of matching events, wrapped as {@link com.xpn.xwiki.plugin.activitystream.plugin.ActivityEvent}
     *         objects.
     * @throws ActivityStreamException if the search query fails
     */
    public List<ActivityEvent> searchEvents(String fromHql, String hql, boolean filter, int nb, int start,
        List<Object> parameterValues) throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            return wrapEvents(getActivityStream().searchEvents(fromHql, hql, filter, nb, start, this.context));
        } else {
            return null;
        }
    }

    /**
     * Search in database activity events matching the given hql query. Retrieved events are ordered by date descending.
     *
     * @param fromHql the "from" clause of the hql query to look events for
     * @param hql the "where" clause of the hql query to look events for
     * @param filter if true, group the matched events by priority
     * @param globalSearch true if the request must be performed on the main database
     * @param nb the number of events to retrieve
     * @param start the offset to start retrieving event at
     * @param parameterValues list of parameters to insert in the query
     * @return a list of matching events, wrapped as {@link com.xpn.xwiki.plugin.activitystream.plugin.ActivityEvent}
     *         objects.
     * @throws ActivityStreamException if the search query fails
     */
    public List<ActivityEvent> searchEvents(String fromHql, String hql, boolean filter, boolean globalSearch, int nb,
        int start, List<Object> parameterValues) throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            return wrapEvents(getActivityStream().searchEvents(fromHql, hql, filter, globalSearch, nb, start,
                this.context));
        } else {
            return null;
        }
    }

    /**
     * Return the latest recorded events.
     *
     * @param filter if true, group the matched events by priority
     * @param nb the number of events to retrieve
     * @param start the offset to start retrieving event at
     * @return the latest recorded events
     * @throws ActivityStreamException if the search query fails
     */
    public List<ActivityEvent> getEvents(boolean filter, int nb, int start) throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            return wrapEvents(getActivityStream().getEvents(filter, nb, start, this.context));
        } else {
            return null;
        }
    }

    /**
     * Return the latest recorded events for the given wiki space.
     *
     * @see #getEvents(boolean, int, int)
     * @param space the local serialized reference of the space to retrieve latest events for
     * @param filter if true, group the matched events by priority
     * @param nb the number of events to retrieve
     * @param start the offset to start retrieving event at
     * @return the latest recorded events
     * @throws ActivityStreamException if the search query fails
     */
    public List<ActivityEvent> getEventsForSpace(String space, boolean filter, int nb, int start)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            return wrapEvents(getActivityStream().getEventsForSpace(space, filter, nb, start, this.context));
        } else {
            return null;
        }
    }

    /**
     * Return the latest recorded events triggered by the given user.
     *
     * @see #getEvents(boolean, int, int)
     * @param user the user to retrieve latest events for
     * @param filter if true, group the matched events by priority
     * @param nb the number of events to retrieve
     * @param start the offset to start retrieving event at
     * @return the latest recorded events triggered by the given user.
     * @throws ActivityStreamException if the search query fails
     */
    public List<ActivityEvent> getEventsForUser(String user, boolean filter, int nb, int start)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            return wrapEvents(getActivityStream().getEventsForUser(user, filter, nb, start, this.context));
        } else {
            return null;
        }
    }

    /**
     * Return the latest events recorded for the given stream name.
     *
     * @see #getEvents(boolean, int, int)
     * @param streamName the name of the stream to retrieve latest events for
     * @param filter if true, group the matched events by priority
     * @param nb the number of events to retrieve
     * @param start the offset to start retrieving event at
     * @return the latest events recorded for the given stream name
     * @throws ActivityStreamException if the search query fails
     */
    public List<ActivityEvent> getEvents(String streamName, boolean filter, int nb, int start)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            return wrapEvents(getActivityStream().getEvents(streamName, filter, nb, start, this.context));
        } else {
            return null;
        }
    }

    /**
     * Return the latest events recorded for the given stream name in the given space.
     *
     * @param streamName the name of the stream to retrieve latest events for
     * @param space local serialized reference of the space in which the events have been fired
     * @param filter if true, group the matched events by priority
     * @param nb the number of events to retrieve
     * @param start the offset to start retrieving event at
     * @return the latest events recorded for the given stream name
     * @throws ActivityStreamException if the search query fails
     */
    public List<ActivityEvent> getEventsForSpace(String streamName, String space, boolean filter, int nb, int start)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            return wrapEvents(getActivityStream().getEventsForSpace(streamName, space, filter, nb, start, this.context));
        } else {
            return null;
        }
    }

    /**
     * Return the latest events recorded for the given stream name and the given user.
     *
     * @param streamName the name of the stream to retrieve latest events for
     * @param user context user at the time the events were fired
     * @param filter if true, group the matched events by priority
     * @param nb the number of events to retrieve
     * @param start the offset to start retrieving event at
     * @return the latest events recorded for the given stream name
     * @throws ActivityStreamException if the search query fails
     */
    public List<ActivityEvent> getEventsForUser(String streamName, String user, boolean filter, int nb, int start)
        throws ActivityStreamException
    {
        if (hasProgrammingRights()) {
            return wrapEvents(getActivityStream().getEventsForUser(streamName, user, filter, nb, start, this.context));
        } else {
            return null;
        }
    }

    /**
     * Wrap a list of events.
     *
     * @param events events to wrap
     * @return list of wrapped events
     */
    protected List<ActivityEvent> wrapEvents(List<com.xpn.xwiki.plugin.activitystream.api.ActivityEvent> events)
    {
        List<ActivityEvent> result = new ArrayList<ActivityEvent>();
        if (events != null) {
            for (com.xpn.xwiki.plugin.activitystream.api.ActivityEvent event : events) {
                com.xpn.xwiki.plugin.activitystream.plugin.ActivityEvent wrappedEvent =
                    new com.xpn.xwiki.plugin.activitystream.plugin.ActivityEvent(event, getXWikiContext());
                result.add(wrappedEvent);
            }
        }
        return result;
    }

    /**
     * Unwrap a list of events.
     *
     * @param events events to unwrap
     * @return list of unwrapped events
     */
    protected List<com.xpn.xwiki.plugin.activitystream.api.ActivityEvent> unwrapEvents(List<ActivityEvent> events)
    {
        List<com.xpn.xwiki.plugin.activitystream.api.ActivityEvent> result =
            new ArrayList<com.xpn.xwiki.plugin.activitystream.api.ActivityEvent>();
        if (events != null) {
            for (ActivityEvent event : events) {
                com.xpn.xwiki.plugin.activitystream.api.ActivityEvent unwrappedEvent = event.getEvent();
                result.add(unwrappedEvent);
            }
        }
        return result;
    }

    /**
     * Get the feed entry for the given event.
     *
     * @param event event to get the entry for
     * @return the feed entry corresponding to the event
     */
    public SyndEntry getFeedEntry(com.xpn.xwiki.plugin.activitystream.plugin.ActivityEvent event)
    {
        return getActivityStream().getFeedEntry(event.getEvent(), this.context);
    }

    /**
     * Get the feed entry for the given event.
     *
     * @param event event to get the entry for
     * @param suffix suffix to add to entry title and body strings
     * @return the feed entry corresponding to the event
     */
    public SyndEntry getFeedEntry(com.xpn.xwiki.plugin.activitystream.plugin.ActivityEvent event, String suffix)
    {
        return getActivityStream().getFeedEntry(event.getEvent(), suffix, this.context);
    }

    /**
     * Get a feed from the given events.
     *
     * @param events events to create the feed from
     * @return the feed entry corresponding to the given events
     */
    public SyndFeed getFeed(List<ActivityEvent> events)
    {
        return getActivityStream().getFeed(unwrapEvents(events), this.context);
    }

    /**
     * Get a feed from the given events.
     *
     * @param events events to create the feed from
     * @param suffix suffix to add to entries title and body strings
     * @return the feed entry corresponding to the given events
     */
    public SyndFeed getFeed(List<ActivityEvent> events, String suffix)
    {
        return getActivityStream().getFeed(unwrapEvents(events), suffix, this.context);
    }

    /**
     * Get a feed from the given events.
     *
     * @param events events to create the feed from
     * @param author author to set in the feed metadata
     * @param title title to set in the feed metadata
     * @param description description to set in the feed metadata
     * @param copyright copyright to set in the feed metadata
     * @param encoding encoding to set in the feed metadata
     * @param url URL to set in the feed metadata
     * @return the feed entry corresponding to the given events
     */
    public SyndFeed getFeed(List<ActivityEvent> events, String author, String title, String description,
        String copyright, String encoding, String url)
    {
        return getActivityStream().getFeed(unwrapEvents(events), author, title, description, copyright, encoding, url,
            this.context);
    }

    /**
     * Get a feed from the given events.
     *
     * @param events events to create the feed from
     * @param author author to set in the feed metadata
     * @param title title to set in the feed metadata
     * @param description description to set in the feed metadata
     * @param copyright copyright to set in the feed metadata
     * @param encoding encoding to set in the feed metadata
     * @param url URL to set in the feed metadata
     * @param suffix suffix to add to entries title and body strings
     * @return the feed entry corresponding to the given events
     */
    public SyndFeed getFeed(List<ActivityEvent> events, String author, String title, String description,
        String copyright, String encoding, String url, String suffix)
    {
        return getActivityStream().getFeed(unwrapEvents(events), author, title, description, copyright, encoding, url,
            suffix, this.context);
    }

    /**
     * Get the string representation of a feed from the given events.
     *
     * @param events events to create the feed from
     * @param author author to set in the feed metadata
     * @param title title to set in the feed metadata
     * @param description description to set in the feed metadata
     * @param copyright copyright to set in the feed metadata
     * @param encoding encoding to set in the feed metadata
     * @param url URL to set in the feed metadata
     * @param type the feed type (syntax) to use, <b>null</b> if none. It can be any version of RSS or Atom. Some
     *            possible values are "rss_1.0", "rss_2.0" and "atom_1.0"
     * @return the feed entry corresponding to the given events
     */
    public String getFeedOutput(List<ActivityEvent> events, String author, String title, String description,
        String copyright, String encoding, String url, String type)
    {
        return getActivityStream().getFeedOutput(unwrapEvents(events), author, title, description, copyright, encoding,
            url, type, this.context);
    }

    /**
     * Get the string representation of a feed from the given events.
     *
     * @param events events to create the feed from
     * @param author author to set in the feed metadata
     * @param title title to set in the feed metadata
     * @param description description to set in the feed metadata
     * @param copyright copyright to set in the feed metadata
     * @param encoding encoding to set in the feed metadata
     * @param url URL to set in the feed metadata
     * @param type the feed type (syntax) to use, <b>null</b> if none. It can be any version of RSS or Atom. Some
     *            possible values are "rss_1.0", "rss_2.0" and "atom_1.0"
     * @param suffix suffix to add to entries title and body strings
     * @return the feed entry corresponding to the given events
     */
    public String getFeedOutput(List<ActivityEvent> events, String author, String title, String description,
        String copyright, String encoding, String url, String type, String suffix)
    {
        return getActivityStream().getFeedOutput(unwrapEvents(events), author, title, description, copyright, encoding,
            url, type, suffix, this.context);
    }

    /**
     * Get the string representation of a feed from the given feed.
     *
     * @param feed the feed to get the string representation from
     * @param type the feed type (syntax) to use, <b>null</b> if none. It can be any version of RSS or Atom. Some
     *            possible values are "rss_1.0", "rss_2.0" and "atom_1.0"
     * @return the feed entry corresponding to the given events
     */
    public String getFeedOutput(SyndFeed feed, String type)
    {
        return getActivityStream().getFeedOutput(feed, type);
    }

    /**
     * @param space the space local serialized reference to transform
     * @return the name of the event stream associated with the given space
     */
    public String getStreamName(String space)
    {
        return getActivityStream().getStreamName(space, this.context);
    }

    /**
     * Get events that have the same requestId as the event passed as parameter. The provided event is also included in
     * the returned list.
     *
     * @param event the event for which to look for related events
     * @return a list of events
     * @throws ActivityStreamException if the retrieval fails
     * @see ActivityStream#getRelatedEvents(com.xpn.xwiki.plugin.activitystream.api.ActivityEvent, XWikiContext)
     */
    public List<ActivityEvent> getRelatedEvents(ActivityEvent event) throws ActivityStreamException
    {
        return wrapEvents(getActivityStream().getRelatedEvents(event.getEvent(), this.context));
    }

    /**
     * Get unique pages with events sorted by date. A document is returned at most once, regardless of the number of
     * events.
     *
     * @param optionalWhereClause HQL where query statement
     * @param maxItems number of events to retrieve
     * @param startAt query offset
     * @return matching pages with events
     */
    public List<Object[]> searchUniquePages(String optionalWhereClause, int maxItems, int startAt)
    {
        try {
            return getActivityStream().searchUniquePages(optionalWhereClause, maxItems, startAt, this.context);
        } catch (ActivityStreamException ex) {
            LOG.error("Failed to query events: " + ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }

    /**
     * Get unique pages with events sorted by date. A document is returned at most once, regardless of the number of
     * events.
     *
     * @param optionalWhereClause HQL where query statement
     * @param parametersValues values for the query parameters
     * @param maxItems number of events to retrieve
     * @param startAt query offset
     * @return matching pages with events
     */
    public List<Object[]> searchUniquePages(String optionalWhereClause, List<Object> parametersValues, int maxItems,
        int startAt)
    {
        try {
            return getActivityStream().searchUniquePages(optionalWhereClause, parametersValues, maxItems, startAt,
                this.context);
        } catch (ActivityStreamException ex) {
            LOG.error("Failed to query events: " + ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }

    /**
     * Get unique pages with events sorted by date, grouped by days. A document is returned at most once per day, but
     * might appear more than once if it has associated events in different days.
     *
     * @param optionalWhereClause HQL where query statement
     * @param maxItems number of events to retrieve
     * @param startAt query offset
     * @return matching pages with events
     */
    public List<Object[]> searchDailyPages(String optionalWhereClause, int maxItems, int startAt)
    {
        try {
            return getActivityStream().searchDailyPages(optionalWhereClause, maxItems, startAt, this.context);
        } catch (ActivityStreamException ex) {
            LOG.error("Failed to query events: " + ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }

    /**
     * Get unique pages with events sorted by date, grouped by days. A document is returned at most once per day, but
     * might appear more than once if it has associated events in different days.
     *
     * @param optionalWhereClause HQL where query statement
     * @param parametersValues values for the query parameters
     * @param maxItems number of events to retrieve
     * @param startAt query offset
     * @return matching pages with events
     */
    public List<Object[]> searchDailyPages(String optionalWhereClause, List<Object> parametersValues, int maxItems,
        int startAt)
    {
        try {
            return getActivityStream().searchDailyPages(optionalWhereClause, parametersValues, maxItems, startAt,
                this.context);
        } catch (ActivityStreamException ex) {
            LOG.error("Failed to query events: " + ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }
}
