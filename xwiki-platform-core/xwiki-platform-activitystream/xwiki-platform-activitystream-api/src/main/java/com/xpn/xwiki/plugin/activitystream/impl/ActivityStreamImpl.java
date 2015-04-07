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
package com.xpn.xwiki.plugin.activitystream.impl;

import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.annotation.event.AnnotationAddedEvent;
import org.xwiki.annotation.event.AnnotationDeletedEvent;
import org.xwiki.annotation.event.AnnotationUpdatedEvent;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.BeginFoldEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;

import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.SyndFeedOutput;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.AttachmentAddedEvent;
import com.xpn.xwiki.internal.event.AttachmentDeletedEvent;
import com.xpn.xwiki.internal.event.AttachmentUpdatedEvent;
import com.xpn.xwiki.internal.event.CommentAddedEvent;
import com.xpn.xwiki.internal.event.CommentDeletedEvent;
import com.xpn.xwiki.internal.event.CommentUpdatedEvent;
import com.xpn.xwiki.plugin.activitystream.api.ActivityEvent;
import com.xpn.xwiki.plugin.activitystream.api.ActivityEventPriority;
import com.xpn.xwiki.plugin.activitystream.api.ActivityEventType;
import com.xpn.xwiki.plugin.activitystream.api.ActivityStream;
import com.xpn.xwiki.plugin.activitystream.api.ActivityStreamException;
import com.xpn.xwiki.plugin.activitystream.plugin.ActivityStreamPlugin;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.web.Utils;

/**
 * Default implementation for {@link ActivityStream}.
 * 
 * @version $Id$
 */
@SuppressWarnings("serial")
public class ActivityStreamImpl implements ActivityStream, EventListener
{
    /** Logging helper object. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityStreamImpl.class);

    /**
     * Key used to store the request ID in the context.
     */
    private static final String REQUEST_ID_CONTEXT_KEY = "activitystream_requestid";

    /**
     * Character used as a separator in event IDs.
     */
    private static final String EVENT_ID_ELEMENTS_SEPARATOR = "-";

    /**
     * The name of the listener.
     */
    private static final String LISTENER_NAME = "activitystream";

    /**
     * The events to match.
     */
    private static final List<Event> LISTENER_EVENTS = new ArrayList<Event>()
    {
        {
            add(new DocumentCreatedEvent());
            add(new DocumentUpdatedEvent());
            add(new DocumentDeletedEvent());
            add(new CommentAddedEvent());
            add(new CommentDeletedEvent());
            add(new CommentUpdatedEvent());
            add(new AttachmentAddedEvent());
            add(new AttachmentDeletedEvent());
            add(new AttachmentUpdatedEvent());
            add(new AnnotationAddedEvent());
            add(new AnnotationDeletedEvent());
            add(new AnnotationUpdatedEvent());
        }
    };

    /**
     * Set fields related to the document which fired the event in the given event object.
     * 
     * @param event the event to prepare
     * @param doc document which fired the event
     * @param context the XWiki context
     */
    private void setEventDocumentRelatedInformation(ActivityEvent event, XWikiDocument doc, XWikiContext context)
    {
        if (doc != null) {
            if (event.getStream() == null) {
                event.setStream(getStreamName(doc.getSpace(), context));
            }

            if (event.getSpace() == null) {
                event.setSpace(doc.getSpace());
            }

            if (event.getPage() == null) {
                event.setPage(doc.getFullName());
            }

            if (event.getUrl() == null) {
                // Protection against NPEs, events can happen before the URL factory gets created.
                if (context.getURLFactory() != null) {
                    event.setUrl(doc.getURL("view", context));
                }
            }
        }
    }

    /**
     * Set fields in the given event object.
     * 
     * @param event the event to prepare
     * @param doc document which fired the event
     * @param context the XWiki context
     */
    protected void prepareEvent(ActivityEvent event, XWikiDocument doc, XWikiContext context)
    {
        if (event.getUser() == null) {
            event.setUser(getSerializedReference(context.getUserReference()));
        }

        if (event.getWiki() == null) {
            event.setWiki(context.getWikiId());
        }

        if (event.getApplication() == null) {
            event.setApplication("xwiki");
        }

        if (event.getDate() == null) {
            event.setDate(new Date());
        }

        if (event.getEventId() == null) {
            event.setEventId(generateEventId(event, context));
        }

        if (event.getRequestId() == null) {
            event.setRequestId((String) context.get(REQUEST_ID_CONTEXT_KEY));
        }

        setEventDocumentRelatedInformation(event, doc, context);
    }

    /**
     * Generate event ID for the given ID. Note that this method does not perform the set of the ID in the event object.
     * 
     * @param event event to generate the ID for
     * @param context the XWiki context
     * @return the generated ID
     */
    protected String generateEventId(ActivityEvent event, XWikiContext context)
    {
        String keySeparator = EVENT_ID_ELEMENTS_SEPARATOR;
        String wikiSpaceSeparator = ":";

        String key =
            event.getStream() + keySeparator + event.getApplication() + keySeparator + event.getWiki()
                + wikiSpaceSeparator + event.getPage() + keySeparator + event.getType();
        long hash = key.hashCode();
        if (hash < 0) {
            hash = -hash;
        }

        String id =
            "" + hash + keySeparator + event.getDate().getTime() + keySeparator
                + RandomStringUtils.randomAlphanumeric(8);
        if (context.get(REQUEST_ID_CONTEXT_KEY) == null) {
            context.put(REQUEST_ID_CONTEXT_KEY, id);
        }

        return id;
    }

    /**
     * @return a new instance of {@link ActivityEventImpl}.
     */
    protected ActivityEvent newActivityEvent()
    {
        return new ActivityEventImpl();
    }

    @Override
    public void init(XWikiContext context) throws XWikiException
    {
        // Listent to Events.
        ObservationManager observationManager = Utils.getComponent(ObservationManager.class);
        if (observationManager.getListener(getName()) == null) {
            observationManager.addListener(this);
        }
        // Init activitystream cleaner.
        ActivityStreamCleaner.getInstance().init(context);
    }

    @Override
    public String getStreamName(String space, XWikiContext context)
    {
        return space;
    }

    @Override
    public void addActivityEvent(ActivityEvent event, XWikiContext context) throws ActivityStreamException
    {
        addActivityEvent(event, null, context);
    }

    /**
     * This method determine if events must be store in the local wiki. If the activitystream is set not to store events
     * in the main wiki, the method will return true. If events are stored in the main wiki, the method retrieves the
     * 'platform.plugin.activitystream.uselocalstore' configuration option. If the option is not found the method
     * returns true (default behavior).
     * 
     * @param context the XWiki context
     * @return true if the activity stream is configured to store events in the main wiki, false otherwise
     */
    private boolean useLocalStore(XWikiContext context)
    {
        if (!useMainStore(context)) {
            // If the main store is disabled, force local store.
            return true;
        }

        ActivityStreamPlugin plugin =
            (ActivityStreamPlugin) context.getWiki().getPlugin(ActivityStreamPlugin.PLUGIN_NAME, context);
        return Integer.parseInt(plugin.getActivityStreamPreference("uselocalstore", "1", context)) == 1;
    }

    /**
     * This method determine if events must be store in the main wiki. If the current wiki is the main wiki, this method
     * returns false, otherwise if retrieves the 'platform.plugin.activitystream.usemainstore' configuration option. If
     * the option is not found the method returns true (default behavior).
     * 
     * @param context the XWiki context
     * @return true if the activity stream is configured to store events in the main wiki, false otherwise
     */
    private boolean useMainStore(XWikiContext context)
    {
        if (context.isMainWiki()) {
            // We're in the main database, we don't have to store the data twice.
            return false;
        }

        ActivityStreamPlugin plugin =
            (ActivityStreamPlugin) context.getWiki().getPlugin(ActivityStreamPlugin.PLUGIN_NAME, context);
        return Integer.parseInt(plugin.getActivityStreamPreference("usemainstore", "1", context)) == 1;
    }

    /**
     * @param event event to add to the stream
     * @param doc which fired the event
     * @param context the XWiki context
     * @throws ActivityStreamException if the addition to the stream fails
     */
    public void addActivityEvent(ActivityEvent event, XWikiDocument doc, XWikiContext context)
        throws ActivityStreamException
    {
        prepareEvent(event, doc, context);

        if (useLocalStore(context)) {
            // store event in the local database
            XWikiHibernateStore localHibernateStore = context.getWiki().getHibernateStore();
            try {
                localHibernateStore.beginTransaction(context);
                Session session = localHibernateStore.getSession(context);
                session.save(event);
                localHibernateStore.endTransaction(context, true);
            } catch (XWikiException e) {
                localHibernateStore.endTransaction(context, false);
            }
        }

        if (useMainStore(context)) {
            // store event in the main database
            String oriDatabase = context.getWikiId();
            context.setWikiId(context.getMainXWiki());
            XWikiHibernateStore mainHibernateStore = context.getWiki().getHibernateStore();
            try {
                mainHibernateStore.beginTransaction(context);
                Session session = mainHibernateStore.getSession(context);
                session.save(event);
                mainHibernateStore.endTransaction(context, true);
            } catch (XWikiException e) {
                mainHibernateStore.endTransaction(context, false);
            } finally {
                context.setWikiId(oriDatabase);
            }
        }
    }

    @Override
    public void addActivityEvent(String streamName, String type, String title, XWikiContext context)
        throws ActivityStreamException
    {
        addActivityEvent(streamName, type, title, null, context);
    }

    @Override
    public void addActivityEvent(String streamName, String type, String title, List<String> params, XWikiContext context)
        throws ActivityStreamException
    {
        ActivityEvent event = newActivityEvent();
        event.setStream(streamName);
        event.setType(type);
        event.setTitle(title);
        event.setBody(title);
        event.setParams(params);
        addActivityEvent(event, context);
    }

    @Override
    public void addDocumentActivityEvent(String streamName, XWikiDocument doc, String type, String title,
        XWikiContext context) throws ActivityStreamException
    {
        addDocumentActivityEvent(streamName, doc, type, ActivityEventPriority.NOTIFICATION, title, null, context);
    }

    @Override
    public void addDocumentActivityEvent(String streamName, XWikiDocument doc, String type, int priority, String title,
        XWikiContext context) throws ActivityStreamException
    {
        addDocumentActivityEvent(streamName, doc, type, priority, title, null, context);
    }

    @Override
    public void addDocumentActivityEvent(String streamName, XWikiDocument doc, String type, String title,
        List<String> params, XWikiContext context) throws ActivityStreamException
    {
        addDocumentActivityEvent(streamName, doc, type, ActivityEventPriority.NOTIFICATION, title, params, context);
    }

    @Override
    public void addDocumentActivityEvent(String streamName, XWikiDocument doc, String type, int priority, String title,
        List<String> params, XWikiContext context) throws ActivityStreamException
    {
        ActivityEventImpl event = new ActivityEventImpl();
        event.setStream(streamName);
        event.setPage(doc.getFullName());
        if (doc.getDatabase() != null) {
            event.setWiki(doc.getDatabase());
        }
        event.setDate(doc.getDate());
        event.setPriority(priority);
        event.setType(type);
        event.setTitle(title);
        event.setBody(title);
        event.setVersion(doc.getVersion());
        event.setParams(params);
        // This might be wrong once non-altering events will be logged.
        event.setUser(getSerializedReference(doc.getAuthorReference()));
        event.setHidden(doc.isHidden());
        addActivityEvent(event, doc, context);
    }

    /**
     * @param event the event
     * @param bTransaction true if inside a transaction
     * @param context the XWiki Context
     * @return the event
     * @throws ActivityStreamException
     */
    private ActivityEventImpl loadActivityEvent(ActivityEvent event, boolean bTransaction, XWikiContext context)
        throws ActivityStreamException
    {
        boolean bTransactionMutable = bTransaction;
        ActivityEventImpl act = null;
        String eventId = event.getEventId();

        if (useLocalStore(context)) {
            // load event from the local database
            XWikiHibernateStore hibstore = context.getWiki().getHibernateStore();
            try {
                if (bTransactionMutable) {
                    hibstore.checkHibernate(context);
                    bTransactionMutable = hibstore.beginTransaction(false, context);
                }
                Session session = hibstore.getSession(context);
                Query query =
                    session
                        .createQuery("select act.eventId from ActivityEventImpl as act where act.eventId = :eventId");
                query.setString("eventId", eventId);
                if (query.uniqueResult() != null) {
                    act = new ActivityEventImpl();
                    session.load(act, eventId);
                }

                if (bTransactionMutable) {
                    hibstore.endTransaction(context, false, false);
                }
            } catch (Exception e) {
                throw new ActivityStreamException();
            } finally {
                try {
                    if (bTransactionMutable) {
                        hibstore.endTransaction(context, false, false);
                    }
                } catch (Exception e) {
                    // Do nothing.
                }
            }
        } else if (useMainStore(context)) {
            // load event from the main database
            String oriDatabase = context.getWikiId();
            context.setWikiId(context.getMainXWiki());
            XWikiHibernateStore hibstore = context.getWiki().getHibernateStore();
            try {
                if (bTransactionMutable) {
                    hibstore.checkHibernate(context);
                    bTransactionMutable = hibstore.beginTransaction(false, context);
                }
                Session session = hibstore.getSession(context);
                Query query =
                    session
                        .createQuery("select act.eventId from ActivityEventImpl as act where act.eventId = :eventId");
                query.setString("eventId", eventId);
                if (query.uniqueResult() != null) {
                    act = new ActivityEventImpl();
                    session.load(act, eventId);
                }

                if (bTransactionMutable) {
                    hibstore.endTransaction(context, false, false);
                }
            } catch (Exception e) {
                throw new ActivityStreamException();
            } finally {
                context.setWikiId(oriDatabase);
                try {
                    if (bTransactionMutable) {
                        hibstore.endTransaction(context, false, false);
                    }
                } catch (Exception e) {
                    // Do nothing.
                }
            }
        }

        return act;
    }

    @Override
    public void deleteActivityEvent(ActivityEvent event, XWikiContext context) throws ActivityStreamException
    {
        boolean bTransaction = true;
        ActivityEventImpl evImpl = loadActivityEvent(event, true, context);
        String oriDatabase = context.getWikiId();

        if (useLocalStore(context)) {
            XWikiHibernateStore hibstore;

            // delete event from the local database
            if (context.getWikiId().equals(event.getWiki())) {
                hibstore = context.getWiki().getHibernateStore();
            } else {
                context.setWikiId(event.getWiki());
                hibstore = context.getWiki().getHibernateStore();
            }

            try {
                if (bTransaction) {
                    hibstore.checkHibernate(context);
                    bTransaction = hibstore.beginTransaction(context);
                }

                Session session = hibstore.getSession(context);

                session.delete(evImpl);

                if (bTransaction) {
                    hibstore.endTransaction(context, true);
                }

            } catch (XWikiException e) {
                throw new ActivityStreamException();
            } finally {
                try {
                    if (bTransaction) {
                        hibstore.endTransaction(context, false);
                    }
                    if (context.getWikiId().equals(oriDatabase)) {
                        context.setWikiId(oriDatabase);
                    }
                } catch (Exception e) {
                    // Do nothing.
                }
            }
        }

        if (useMainStore(context)) {
            // delete event from the main database
            context.setWikiId(context.getMainXWiki());
            XWikiHibernateStore hibstore = context.getWiki().getHibernateStore();
            try {
                if (bTransaction) {
                    hibstore.checkHibernate(context);
                    bTransaction = hibstore.beginTransaction(context);
                }

                Session session = hibstore.getSession(context);

                session.delete(evImpl);

                if (bTransaction) {
                    hibstore.endTransaction(context, true);
                }

            } catch (XWikiException e) {
                throw new ActivityStreamException();
            } finally {
                try {
                    if (bTransaction) {
                        hibstore.endTransaction(context, false);
                    }
                    context.setWikiId(oriDatabase);
                } catch (Exception e) {
                    // Do nothing
                }
            }
        }
    }

    @Override
    public List<ActivityEvent> searchEvents(String hql, boolean filter, int nb, int start, XWikiContext context)
        throws ActivityStreamException
    {
        return searchEvents("", hql, filter, nb, start, context);
    }

    @Override
    public List<ActivityEvent> searchEvents(String hql, boolean filter, boolean globalSearch, int nb, int start,
        XWikiContext context) throws ActivityStreamException
    {
        return searchEvents("", hql, filter, globalSearch, nb, start, context);
    }

    @Override
    public List<ActivityEvent> searchEvents(String hql, boolean filter, boolean globalSearch, int nb, int start,
        List<Object> parameterValues, XWikiContext context) throws ActivityStreamException
    {
        return searchEvents("", hql, filter, globalSearch, nb, start, parameterValues, context);
    }

    @Override
    public List<ActivityEvent> searchEvents(String fromHql, String hql, boolean filter, int nb, int start,
        XWikiContext context) throws ActivityStreamException
    {
        return searchEvents(fromHql, hql, filter, nb, start, null, context);
    }

    @Override
    public List<ActivityEvent> searchEvents(String fromHql, String hql, boolean filter, boolean globalSearch, int nb,
        int start, XWikiContext context) throws ActivityStreamException
    {
        return searchEvents(fromHql, hql, filter, globalSearch, nb, start, null, context);
    }

    @Override
    public List<ActivityEvent> searchEvents(String fromHql, String hql, boolean filter, int nb, int start,
        List<Object> parameterValues, XWikiContext context) throws ActivityStreamException
    {
        return searchEvents(fromHql, hql, filter, false, nb, start, parameterValues, context);
    }

    /**
     * This method will add a where clause to filter events fired from hidden documents. The clause will not be added to
     * the query if the user has specified that he wish to see hidden documents in his profile. If the clause is added
     * this method will also add a 'where' to the query if it is missing.
     * 
     * @param query The query to add the filter to
     */
    private void addHiddenEventsFilter(StringBuffer query)
    {
        ConfigurationSource source = Utils.getComponent(ConfigurationSource.class, "user");
        Integer preference = source.getProperty("displayHiddenDocuments", Integer.class);
        if (preference == null || preference != 1) {
            if (!query.toString().contains(" where ")) {
                query.append(" where ");
            }
            query.append(" (act.hidden <> true or act.hidden is null) and ");
        }
    }

    /**
     * This method will add the passed optional where clause to the given query if the optional clause is not an empty
     * string nor null. If the clause is added this method will also add a 'where' to the query if it is missing.
     * 
     * @param query The query to add the where clause to
     * @param optionalWhereClause The optional where clause to add
     */
    private void addOptionalEventsFilter(StringBuffer query, String optionalWhereClause)
    {
        if (StringUtils.isNotBlank(optionalWhereClause)) {
            if (!query.toString().contains(" where ")) {
                query.append(" where ");
            }
            query.append(optionalWhereClause);
        }
    }

    @Override
    public List<ActivityEvent> searchEvents(String fromHql, String hql, boolean filter, boolean globalSearch, int nb,
        int start, List<Object> parameterValues, XWikiContext context) throws ActivityStreamException
    {
        StringBuffer searchHql = new StringBuffer();
        List<ActivityEvent> results;

        if (filter) {
            searchHql.append("select act from ActivityEventImpl as act, ActivityEventImpl as act2 ");
            searchHql.append(fromHql);
            searchHql.append(" where act.eventId=act2.eventId and ");
            addHiddenEventsFilter(searchHql);
            searchHql.append(hql);
            searchHql.append(" group by act.requestId having (act.priority)=max(act2.priority) order by act.date desc");
        } else {
            searchHql.append("select act from ActivityEventImpl as act ");
            searchHql.append(fromHql);
            searchHql.append(" where ");
            addHiddenEventsFilter(searchHql);
            searchHql.append(hql);
            searchHql.append(" order by act.date desc");
        }

        if (globalSearch) {
            // Search in the main database
            String oriDatabase = context.getWikiId();
            try {
                context.setWikiId(context.getMainXWiki());
                results =
                    context.getWiki().getStore().search(searchHql.toString(), nb, start, parameterValues, context);
            } catch (XWikiException e) {
                throw new ActivityStreamException(e);
            } finally {
                context.setWikiId(oriDatabase);
            }
        } else {
            try {
                // Search in the local database
                results =
                    context.getWiki().getStore().search(searchHql.toString(), nb, start, parameterValues, context);
            } catch (XWikiException e) {
                throw new ActivityStreamException(e);
            }
        }

        return results;
    }

    @Override
    public List<ActivityEvent> getEvents(boolean filter, int nb, int start, XWikiContext context)
        throws ActivityStreamException
    {
        return searchEvents("1=1", filter, nb, start, context);
    }

    @Override
    public List<ActivityEvent> getEventsForSpace(String space, boolean filter, int nb, int start, XWikiContext context)
        throws ActivityStreamException
    {
        return searchEvents("act.space='" + space + "'", filter, nb, start, context);
    }

    @Override
    public List<ActivityEvent> getEventsForUser(String user, boolean filter, int nb, int start, XWikiContext context)
        throws ActivityStreamException
    {
        return searchEvents("act.user='" + user + "'", filter, nb, start, context);
    }

    @Override
    public List<ActivityEvent> getEvents(String stream, boolean filter, int nb, int start, XWikiContext context)
        throws ActivityStreamException
    {
        return searchEvents("act.stream='" + stream + "'", filter, nb, start, context);
    }

    @Override
    public List<ActivityEvent> getEventsForSpace(String stream, String space, boolean filter, int nb, int start,
        XWikiContext context) throws ActivityStreamException
    {
        return searchEvents("act.space='" + space + "' and act.stream='" + stream + "'", filter, nb, start, context);
    }

    @Override
    public List<ActivityEvent> getEventsForUser(String stream, String user, boolean filter, int nb, int start,
        XWikiContext context) throws ActivityStreamException
    {
        return searchEvents("act.user='" + user + "' and act.stream='" + stream + "'", filter, nb, start, context);
    }

    @Override
    public SyndEntry getFeedEntry(ActivityEvent event, XWikiContext context)
    {
        return getFeedEntry(event, "", context);
    }

    @Override
    public SyndEntry getFeedEntry(ActivityEvent event, String suffix, XWikiContext context)
    {
        SyndEntry entry = new SyndEntryImpl();
        String user = event.getUser();
        String displayUser = context.getWiki().getUserName(user, null, false, context);
        entry.setAuthor(displayUser);
        event.setTitle(event.getTitle() + ".rss.title" + suffix);
        entry.setTitle(event.getDisplayTitle(context));
        event.setBody(event.getBody() + ".rss.body" + suffix);
        SyndContentImpl sc = new SyndContentImpl();
        sc.setValue(event.getDisplayBody(context));
        sc.setType("text/html");
        entry.setDescription(sc);
        String url;
        try {
            url = (new URL(context.getURL(), event.getUrl())).toString();
        } catch (MalformedURLException e) {
            url = event.getUrl();
        }
        entry.setLink(url);
        entry.setPublishedDate(event.getDate());
        entry.setUpdatedDate(event.getDate());
        return entry;
    }

    @Override
    public SyndFeed getFeed(List<ActivityEvent> events, XWikiContext context)
    {
        return getFeed(events, "", context);
    }

    @Override
    public SyndFeed getFeed(List<ActivityEvent> events, String suffix, XWikiContext context)
    {
        SyndFeed feed = new SyndFeedImpl();
        List<SyndEntry> entries = new ArrayList<SyndEntry>();
        for (ActivityEvent event : events) {
            SyndEntry entry = getFeedEntry(event, suffix, context);
            entries.add(entry);
        }
        feed.setEntries(entries);
        return feed;
    }

    @Override
    public SyndFeed getFeed(List<ActivityEvent> events, String author, String title, String description,
        String copyright, String encoding, String url, XWikiContext context)
    {
        return getFeed(events, author, title, description, copyright, encoding, url, "", context);
    }

    @Override
    public SyndFeed getFeed(List<ActivityEvent> events, String author, String title, String description,
        String copyright, String encoding, String url, String suffix, XWikiContext context)
    {
        SyndFeed feed = getFeed(events, suffix, context);
        feed.setAuthor(author);
        feed.setDescription(description);
        feed.setCopyright(copyright);
        feed.setEncoding(encoding);
        feed.setLink(url);
        feed.setTitle(title);
        return feed;
    }

    @Override
    public String getFeedOutput(List<ActivityEvent> events, String author, String title, String description,
        String copyright, String encoding, String url, String type, XWikiContext context)
    {
        return getFeedOutput(events, author, title, description, copyright, encoding, url, type, "", context);
    }

    @Override
    public String getFeedOutput(List<ActivityEvent> events, String author, String title, String description,
        String copyright, String encoding, String url, String type, String suffix, XWikiContext context)
    {
        SyndFeed feed = getFeed(events, author, title, description, copyright, encoding, url, suffix, context);
        return getFeedOutput(feed, type);
    }

    @Override
    public String getFeedOutput(SyndFeed feed, String type)
    {
        feed.setFeedType(type);
        StringWriter writer = new StringWriter();
        SyndFeedOutput output = new SyndFeedOutput();
        try {
            output.output(feed, writer);
            writer.close();
            return writer.toString();
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public List<Event> getEvents()
    {
        return LISTENER_EVENTS;
    }

    @Override
    public String getName()
    {
        return LISTENER_NAME;
    }

    private static BeginFoldEvent IGNORED_EVENTS = new BeginFoldEvent()
    {
        @Override
        public boolean matches(Object otherEvent)
        {
            return otherEvent instanceof BeginFoldEvent;
        }
    };

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Do not record some ignored events
        ObservationContext observationContext = Utils.getComponent(ObservationContext.class);
        if (observationContext.isIn(IGNORED_EVENTS)) {
            return;
        }

        XWikiDocument currentDoc = (XWikiDocument) source;
        XWikiDocument originalDoc = currentDoc.getOriginalDocument();
        XWikiContext context = (XWikiContext) data;
        String wiki = context.getWikiId();
        String msgPrefix = "activitystream.event.";
        String streamName = getStreamName(currentDoc.getSpace(), context);

        // If we haven't found a stream to store the event or if both currentDoc and originalDoc are null: exit
        if (streamName == null) {
            return;
        }

        // Take events into account only once in a cluster
        if (!Utils.getComponent(RemoteObservationManagerContext.class).isRemoteState()) {
            String eventType;
            String displayTitle;
            String additionalIdentifier = null;

            if (event instanceof DocumentCreatedEvent) {
                eventType = ActivityEventType.CREATE;
                displayTitle = currentDoc.getRenderedTitle(context);
            } else if (event instanceof DocumentUpdatedEvent) {
                eventType = ActivityEventType.UPDATE;
                displayTitle = originalDoc.getRenderedTitle(context);
            } else if (event instanceof DocumentDeletedEvent) {
                eventType = ActivityEventType.DELETE;
                displayTitle = originalDoc.getRenderedTitle(context);
                // When we receive a DELETE event, the given document is blank and does not have version & hidden tag
                // properly set.
                currentDoc.setVersion(originalDoc.getVersion());
                currentDoc.setHidden(originalDoc.isHidden());
            } else if (event instanceof CommentAddedEvent) {
                eventType = ActivityEventType.ADD_COMMENT;
                displayTitle = currentDoc.getRenderedTitle(context);
                additionalIdentifier = ((CommentAddedEvent) event).getIdentifier();
            } else if (event instanceof CommentDeletedEvent) {
                eventType = ActivityEventType.DELETE_COMMENT;
                displayTitle = currentDoc.getRenderedTitle(context);
                additionalIdentifier = ((CommentDeletedEvent) event).getIdentifier();
            } else if (event instanceof CommentUpdatedEvent) {
                eventType = ActivityEventType.UPDATE_COMMENT;
                displayTitle = currentDoc.getRenderedTitle(context);
                additionalIdentifier = ((CommentUpdatedEvent) event).getIdentifier();
            } else if (event instanceof AttachmentAddedEvent) {
                eventType = ActivityEventType.ADD_ATTACHMENT;
                displayTitle = currentDoc.getRenderedTitle(context);
                additionalIdentifier = ((AttachmentAddedEvent) event).getName();
            } else if (event instanceof AttachmentDeletedEvent) {
                eventType = ActivityEventType.DELETE_ATTACHMENT;
                displayTitle = currentDoc.getRenderedTitle(context);
                additionalIdentifier = ((AttachmentDeletedEvent) event).getName();
            } else if (event instanceof AttachmentUpdatedEvent) {
                eventType = ActivityEventType.UPDATE_ATTACHMENT;
                displayTitle = currentDoc.getRenderedTitle(context);
                additionalIdentifier = ((AttachmentUpdatedEvent) event).getName();
            } else if (event instanceof AnnotationAddedEvent) {
                eventType = ActivityEventType.ADD_ANNOTATION;
                displayTitle = currentDoc.getRenderedTitle(context);
                additionalIdentifier = ((AnnotationAddedEvent) event).getIdentifier();
            } else if (event instanceof AnnotationDeletedEvent) {
                eventType = ActivityEventType.DELETE_ANNOTATION;
                displayTitle = currentDoc.getRenderedTitle(context);
                additionalIdentifier = ((AnnotationDeletedEvent) event).getIdentifier();
            } else { // update annotation
                eventType = ActivityEventType.UPDATE_ANNOTATION;
                displayTitle = currentDoc.getRenderedTitle(context);
                additionalIdentifier = ((AnnotationUpdatedEvent) event).getIdentifier();
            }

            List<String> params = new ArrayList<String>();
            params.add(displayTitle);
            if (additionalIdentifier != null) {
                params.add(additionalIdentifier);
            }

            try {
                addDocumentActivityEvent(streamName, currentDoc, eventType, msgPrefix + eventType, params, context);
            } catch (ActivityStreamException e) {
                LOGGER.error("Exception while trying to add a document activity event, updated document: [" + wiki
                    + ":" + currentDoc + "]");
            }
        }
    }

    @Override
    public List<ActivityEvent> getRelatedEvents(ActivityEvent event, XWikiContext context)
        throws ActivityStreamException
    {
        List<Object> params = new ArrayList<Object>();
        params.add(event.getRequestId());

        return this.searchEvents("", "act.requestId= ? ", false, false, 0, 0, params, context);
    }

    @Override
    public List<Object[]> searchUniquePages(String optionalWhereClause, int maxItems, int startAt, XWikiContext context)
        throws ActivityStreamException
    {
        return searchUniquePages(optionalWhereClause, null, maxItems, startAt, context);
    }

    @Override
    public List<Object[]> searchUniquePages(String optionalWhereClause, List<Object> parametersValues, int maxItems,
        int startAt, XWikiContext context) throws ActivityStreamException
    {
        StringBuffer searchHql = new StringBuffer();
        List<Object[]> results;

        searchHql.append("select act.page, max(act.date) from ActivityEventImpl as act");
        addHiddenEventsFilter(searchHql);
        addOptionalEventsFilter(searchHql, optionalWhereClause);
        searchHql.append(" group by act.page order by 2 desc");

        String originalDatabase = context.getWikiId();
        try {
            context.setWikiId(context.getMainXWiki());
            results =
                context.getWiki().getStore().search(searchHql.toString(), maxItems, startAt, parametersValues, context);
        } catch (XWikiException e) {
            throw new ActivityStreamException(e);
        } finally {
            context.setWikiId(originalDatabase);
        }

        return results;
    }

    @Override
    public List<Object[]> searchDailyPages(String optionalWhereClause, int maxItems, int startAt, XWikiContext context)
        throws ActivityStreamException
    {
        return searchDailyPages(optionalWhereClause, null, maxItems, startAt, context);
    }

    @Override
    public List<Object[]> searchDailyPages(String optionalWhereClause, List<Object> parametersValues, int maxItems,
        int startAt, XWikiContext context) throws ActivityStreamException
    {
        StringBuffer searchHql = new StringBuffer();
        List<Object[]> results = new ArrayList<Object[]>();

        searchHql.append("select year(act.date), month(act.date), day(act.date), act.page, max(act.date), act.wiki "
            + "from ActivityEventImpl as act");
        addHiddenEventsFilter(searchHql);
        addOptionalEventsFilter(searchHql, optionalWhereClause);
        searchHql.append(" group by year(act.date), month(act.date), day(act.date), act.page, act.wiki "
            + "order by 5 desc");

        String originalDatabase = context.getWikiId();
        try {
            context.setWikiId(context.getMainXWiki());
            List<Object[]> rawResults =
                context.getWiki().getStore().search(searchHql.toString(), maxItems, startAt, parametersValues, context);
            for (Object[] rawResult : rawResults) {
                results.add(new Object[] { rawResult[3], rawResult[4], rawResult[5] });
            }
        } catch (XWikiException e) {
            throw new ActivityStreamException(e);
        } finally {
            context.setWikiId(originalDatabase);
        }

        return results;
    }

    /**
     * @param documentReference to be serialized
     * @return the default (absolute) string serialized document reference
     */
    private static String getSerializedReference(DocumentReference documentReference)
    {
        EntityReferenceSerializer<String> serializer = Utils.getComponent(EntityReferenceSerializer.TYPE_STRING);
        String stringSerialization = serializer.serialize(documentReference);

        return stringSerialization;
    }
}
