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
package com.xpn.xwiki.plugin.activitystream.api;

import java.util.List;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Manages the activity stream.
 *
 * @version $Id$
 */
public interface ActivityStream
{
    /**
     * Init method, must be called on plugin init.
     *
     * @param context the XWiki context
     * @throws XWikiException if the init of the activity stream failed
     */
    void init(XWikiContext context) throws XWikiException;

    /**
     * Transforms space reference into stream name.
     *
     * @param space the space local serialized reference to transform
     * @param context the XWiki context
     * @return the name of the stream for the given space
     */
    String getStreamName(String space, XWikiContext context);

    /**
     * Adding an activity event. The Id does not need to be filled as it will be created. Date and Wiki are optional.
     *
     * @param event event to add to the stream
     * @param context the XWiki context
     * @throws ActivityStreamException if the addition to the stream fails
     */
    void addActivityEvent(ActivityEvent event, XWikiContext context) throws ActivityStreamException;

    /**
     * Adding an activity event. The Id does not need to be filled as it will be created. Date and Wiki are optional.
     *
     * @param streamName name of the stream to use for the addition
     * @param type type of the event
     * @param title title of the event
     * @param context the XWiki context
     * @throws ActivityStreamException if the addition to the stream fails
     */
    void addActivityEvent(String streamName, String type, String title, XWikiContext context)
        throws ActivityStreamException;

    /**
     * Adding an activity event. The Id does not need to be filled as it will be created. Date and Wiki are optional.
     *
     * @param streamName name of the stream to use for the addition
     * @param type type of the event
     * @param title title of the event
     * @param params parameters of the event
     * @param context the XWiki context
     * @throws ActivityStreamException if the addition to the stream fails
     */
    void addActivityEvent(String streamName, String type, String title, List<String> params, XWikiContext context)
        throws ActivityStreamException;

    /**
     * Adding an activity event. The Id does not need to be filled as it will be created. Date and Wiki are optional.
     *
     * @param streamName name of the stream to use for the addition
     * @param doc which fired the event
     * @param type type of the event
     * @param title title of the event
     * @param context the XWiki context
     * @throws ActivityStreamException if the addition to the stream fails
     */
    void addDocumentActivityEvent(String streamName, XWikiDocument doc, String type, String title, XWikiContext context)
        throws ActivityStreamException;

    /**
     * Adding an activity event. The Id does not need to be filled as it will be created. Date and Wiki are optional.
     *
     * @param streamName name of the stream to use for the addition
     * @param doc which fired the event
     * @param type type of the event
     * @param priority priority of the event
     * @param title title of the event
     * @param context the XWiki context
     * @throws ActivityStreamException if the addition to the stream fails
     */
    void addDocumentActivityEvent(String streamName, XWikiDocument doc, String type, int priority, String title,
        XWikiContext context) throws ActivityStreamException;

    /**
     * Adding an activity event. The Id does not need to be filled as it will be created. Date and Wiki are optional.
     *
     * @param streamName name of the stream to use for the addition
     * @param doc which fired the event
     * @param type type of the event
     * @param title title of the event
     * @param params parameters of the event
     * @param context the XWiki context
     * @throws ActivityStreamException if the addition to the stream fails
     */
    void addDocumentActivityEvent(String streamName, XWikiDocument doc, String type, String title, List<String> params,
        XWikiContext context) throws ActivityStreamException;

    /**
     * Adding an activity event. The Id does not need to be filled as it will be created. Date and Wiki are optional.
     *
     * @param streamName name of the stream to use for the addition
     * @param doc which fired the event
     * @param type type of the event
     * @param priority priority of the event
     * @param title title of the event
     * @param params parameters of the event
     * @param context the XWiki context
     * @throws ActivityStreamException if the addition to the stream fails
     */
    void addDocumentActivityEvent(String streamName, XWikiDocument doc, String type, int priority, String title,
        List<String> params, XWikiContext context) throws ActivityStreamException;

    /**
     * Delete the given event from the database.
     *
     * @param event the event to delete
     * @param context the XWiki context
     * @throws ActivityStreamException if the delete of the event fails
     */
    void deleteActivityEvent(ActivityEvent event, XWikiContext context) throws ActivityStreamException;

    /**
     * Search events.
     *
     * @param hql HQL where query statement
     * @param filter true if the events should be filtered by priority
     * @param nb number of events to retrieve
     * @param start query offset
     * @param context the XWiki context
     * @return matching events
     * @throws ActivityStreamException if the search query fails
     */
    List<ActivityEvent> searchEvents(String hql, boolean filter, int nb, int start, XWikiContext context)
        throws ActivityStreamException;

    /**
     * Search events.
     *
     * @param hql HQL where query statement
     * @param filter true if the events should be filtered by priority
     * @param globalSearch true if the request must be performed on the main database
     * @param nb number of events to retrieve
     * @param start query offset
     * @param context the XWiki context
     * @return matching events
     * @throws ActivityStreamException if the search query fails
     */
    List<ActivityEvent> searchEvents(String hql, boolean filter, boolean globalSearch, int nb, int start,
        XWikiContext context) throws ActivityStreamException;

    /**
     * Search events.
     *
     * @param hql HQL where query statement
     * @param filter true if the events should be filtered by priority
     * @param globalSearch true if the request must be performed on the main database
     * @param nb number of events to retrieve
     * @param start query offset
     * @param parameterValues values of the parametrized query
     * @param context the XWiki context
     * @return matching events
     * @throws ActivityStreamException if the search query fails
     */
    List<ActivityEvent> searchEvents(String hql, boolean filter, boolean globalSearch, int nb, int start,
        List<Object> parameterValues, XWikiContext context) throws ActivityStreamException;

    /**
     * Search events.
     *
     * @param fromHql HQL from query statement
     * @param hql HQL where query statement
     * @param filter true if the events should be filtered by priority
     * @param nb number of events to retrieve
     * @param start query offset
     * @param context the XWiki context
     * @return matching events
     * @throws ActivityStreamException if the search query fails
     */
    List<ActivityEvent> searchEvents(String fromHql, String hql, boolean filter, int nb, int start,
        XWikiContext context) throws ActivityStreamException;

    /**
     * Search events.
     *
     * @param fromHql HQL from query statement
     * @param hql HQL where query statement
     * @param filter true if the events should be filtered by priority
     * @param globalSearch true if the request must be performed on the main database
     * @param nb number of events to retrieve
     * @param start query offset
     * @param context the XWiki context
     * @return matching events
     * @throws ActivityStreamException if the search query fails
     */
    List<ActivityEvent> searchEvents(String fromHql, String hql, boolean filter, boolean globalSearch, int nb,
        int start, XWikiContext context) throws ActivityStreamException;

    /**
     * Search events.
     *
     * @param fromHql HQL from query statement
     * @param hql HQL where query statement
     * @param filter true if the events should be filtered by priority
     * @param nb number of events to retrieve
     * @param start query offset
     * @param parameterValues values of the parametrized query
     * @param context the XWiki context
     * @return matching events
     * @throws ActivityStreamException if the search query fails
     */
    List<ActivityEvent> searchEvents(String fromHql, String hql, boolean filter, int nb, int start,
        List<Object> parameterValues, XWikiContext context) throws ActivityStreamException;

    /**
     * Search events.
     *
     * @param fromHql HQL from query statement
     * @param hql HQL where query statement
     * @param filter true if the events should be filtered by priority
     * @param globalSearch true if the request must be performed on the main database
     * @param nb number of events to retrieve
     * @param start query offset
     * @param parameterValues values of the parametrized query
     * @param context the XWiki context
     * @return matching events
     * @throws ActivityStreamException if the search query fails
     */
    List<ActivityEvent> searchEvents(String fromHql, String hql, boolean filter, boolean globalSearch, int nb,
        int start, List<Object> parameterValues, XWikiContext context) throws ActivityStreamException;

    /**
     * Get events from the activity stream.
     *
     * @param filter true if the events should be filtered by priority
     * @param nb number of events to retrieve
     * @param start query offset
     * @param context the XWiki context
     * @return a list of events
     * @throws ActivityStreamException if the retrieval fails
     */
    List<ActivityEvent> getEvents(boolean filter, int nb, int start, XWikiContext context)
        throws ActivityStreamException;

    /**
     * Get events from the activity stream of a space.
     *
     * @param space local serialized reference of the space to retrieve the events from
     * @param filter true if the events should be filtered by priority
     * @param nb number of events to retrieve
     * @param start query offset
     * @param context the XWiki context
     * @return a list of events
     * @throws ActivityStreamException if the retrieval fails
     */
    List<ActivityEvent> getEventsForSpace(String space, boolean filter, int nb, int start, XWikiContext context)
        throws ActivityStreamException;

    /**
     * Get events from the activity stream of a user.
     *
     * @param user user to retrieve the events from
     * @param filter true if the events should be filtered by priority
     * @param nb number of events to retrieve
     * @param start query offset
     * @param context the XWiki context
     * @return a list of events
     * @throws ActivityStreamException if the retrieval fails
     */
    List<ActivityEvent> getEventsForUser(String user, boolean filter, int nb, int start, XWikiContext context)
        throws ActivityStreamException;

    /**
     * Get events from a particular activity stream.
     *
     * @param streamName name of the activity stream to retrieve the events from
     * @param filter true if the events should be filtered by priority
     * @param nb number of events to retrieve
     * @param start query offset
     * @param context the XWiki context
     * @return a list of events
     * @throws ActivityStreamException if the retrieval fails
     */
    List<ActivityEvent> getEvents(String streamName, boolean filter, int nb, int start, XWikiContext context)
        throws ActivityStreamException;

    /**
     * Get events from the given activity stream which happened in a particular space.
     *
     * @param streamName name of the activity stream to retrieve the events from
     * @param space local serialized reference of the space to retrieve the events from
     * @param filter true if the events should be filtered by priority
     * @param nb number of events to retrieve
     * @param start query offset
     * @param context the XWiki context
     * @return a list of events
     * @throws ActivityStreamException if the retrieval fails
     */
    List<ActivityEvent> getEventsForSpace(String streamName, String space, boolean filter, int nb, int start,
        XWikiContext context) throws ActivityStreamException;

    /**
     * Get events from the given activity stream which have been fired by a particular user.
     *
     * @param streamName name of the activity stream to retrieve the events from
     * @param user user to retrieve the events from
     * @param filter true if the events should be filtered by priority
     * @param nb number of events to retrieve
     * @param start query offset
     * @param context the XWiki context
     * @return a list of events
     * @throws ActivityStreamException if the retrieval fails
     */
    List<ActivityEvent> getEventsForUser(String streamName, String user, boolean filter, int nb, int start,
        XWikiContext context) throws ActivityStreamException;

    /**
     * Get the feed entry for the given event.
     *
     * @param event event to get the entry for
     * @param context the XWiki context
     * @return the feed entry corresponding to the event
     */
    SyndEntry getFeedEntry(ActivityEvent event, XWikiContext context);

    /**
     * Get the feed entry for the given event.
     *
     * @param event event to get the entry for
     * @param suffix suffix to add to entry title and body strings
     * @param context the XWiki context
     * @return the feed entry corresponding to the event
     */
    SyndEntry getFeedEntry(ActivityEvent event, String suffix, XWikiContext context);

    /**
     * Get a feed from the given events.
     *
     * @param events events to create the feed from
     * @param context the XWiki context
     * @return the feed entry corresponding to the given events
     */
    SyndFeed getFeed(List<ActivityEvent> events, XWikiContext context);

    /**
     * Get a feed from the given events.
     *
     * @param events events to create the feed from
     * @param suffix suffix to add to entries title and body strings
     * @param context the XWiki context
     * @return the feed entry corresponding to the given events
     */
    SyndFeed getFeed(List<ActivityEvent> events, String suffix, XWikiContext context);

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
     * @param context the XWiki context
     * @return the feed entry corresponding to the given events
     */
    SyndFeed getFeed(List<ActivityEvent> events, String author, String title, String description, String copyright,
        String encoding, String url, XWikiContext context);

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
     * @param context the XWiki context
     * @return the feed entry corresponding to the given events
     */
    SyndFeed getFeed(List<ActivityEvent> events, String author, String title, String description, String copyright,
        String encoding, String url, String suffix, XWikiContext context);

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
     * @param context the XWiki context
     * @return the feed entry corresponding to the given events
     */
    String getFeedOutput(List<ActivityEvent> events, String author, String title, String description, String copyright,
        String encoding, String url, String type, XWikiContext context);

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
     * @param context the XWiki context
     * @return the feed entry corresponding to the given events
     */
    String getFeedOutput(List<ActivityEvent> events, String author, String title, String description, String copyright,
        String encoding, String url, String type, String suffix, XWikiContext context);

    /**
     * @param feed the feed to get the string representation for
     * @param type the feed type (syntax) to use, <b>null</b> if none. It can be any version of RSS or Atom. Some
     *            possible values are "rss_1.0", "rss_2.0" and "atom_1.0"
     * @return the string representation of the given feed.
     */
    String getFeedOutput(SyndFeed feed, String type);

    /**
     * Get events that have the same requestId as the event passed as parameter. The provided event is also included in
     * the returned list.
     *
     * @param event the event for which to look for related events
     * @param context the XWiki context
     * @return a list of events
     * @throws ActivityStreamException if the retrieval fails
     */
    List<ActivityEvent> getRelatedEvents(ActivityEvent event, XWikiContext context) throws ActivityStreamException;

    /**
     * Get unique pages with events sorted by date. A document is returned at most once, regardless of the number of
     * events.
     *
     * @param optionalWhereClause optional HQL where query statement
     * @param maxItems maximum number of documents to retrieve
     * @param startAt query offset
     * @param context the XWiki context
     * @return pairs of [document name, last event date], in descending order of the last event date
     * @throws ActivityStreamException if the search fails
     */
    List<Object[]> searchUniquePages(String optionalWhereClause, int maxItems, int startAt, XWikiContext context)
        throws ActivityStreamException;

    /**
     * Get unique pages with events sorted by date. A document is returned at most once, regardless of the number of
     * events.
     *
     * @param optionalWhereClause optional HQL where query statement
     * @param parametersValues values for the query parameters
     * @param maxItems maximum number of documents to retrieve
     * @param startAt query offset
     * @param context the XWiki context
     * @return pairs of [document name, last event date], in descending order of the last event date
     * @throws ActivityStreamException if the search fails
     */
    List<Object[]> searchUniquePages(String optionalWhereClause, List<Object> parametersValues, int maxItems,
        int startAt, XWikiContext context) throws ActivityStreamException;

    /**
     * Get unique pages with events sorted by date, grouped by days. A document is returned at most once per day, but
     * might appear more than once if it has associated events in different days.
     *
     * @param optionalWhereClause optional HQL where query statement
     * @param maxItems maximum number of documents to retrieve
     * @param startAt query offset
     * @param context the XWiki context
     * @return pairs of [document name, event date], in descending order of the last event date
     * @throws ActivityStreamException if the search fails
     */
    List<Object[]> searchDailyPages(String optionalWhereClause, int maxItems, int startAt, XWikiContext context)
        throws ActivityStreamException;

    /**
     * Get unique pages with events sorted by date, grouped by days. A document is returned at most once per day, but
     * might appear more than once if it has associated events in different days.
     *
     * @param optionalWhereClause optional HQL where query statement
     * @param parametersValues values for the query parameters
     * @param maxItems maximum number of documents to retrieve
     * @param startAt query offset
     * @param context the XWiki context
     * @return pairs of [document name, event date], in descending order of the last event date
     * @throws ActivityStreamException if the search fails
     */
    List<Object[]> searchDailyPages(String optionalWhereClause, List<Object> parametersValues, int maxItems,
        int startAt, XWikiContext context) throws ActivityStreamException;
}
