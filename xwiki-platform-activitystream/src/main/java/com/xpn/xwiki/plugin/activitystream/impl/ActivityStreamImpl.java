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

import java.util.List;
import java.util.ArrayList;
import java.io.StringWriter;
import java.net.URL;
import java.net.MalformedURLException;

import org.apache.commons.lang.RandomStringUtils;
import org.hibernate.Query;
import org.hibernate.Session;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.notify.DocChangeRule;
import com.xpn.xwiki.notify.XWikiDocChangeNotificationInterface;
import com.xpn.xwiki.notify.XWikiNotificationRule;
import com.xpn.xwiki.plugin.activitystream.api.ActivityEvent;
import com.xpn.xwiki.plugin.activitystream.api.ActivityEventPriority;
import com.xpn.xwiki.plugin.activitystream.api.ActivityEventType;
import com.xpn.xwiki.plugin.activitystream.api.ActivityStream;
import com.xpn.xwiki.plugin.activitystream.api.ActivityStreamException;
import com.xpn.xwiki.plugin.activitystream.plugin.ActivityStreamPlugin;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.SyndFeedOutput;

/**
 * @version $Id: $
 */
public class ActivityStreamImpl implements ActivityStream, XWikiDocChangeNotificationInterface
{
    public void init(XWikiContext context) throws XWikiException
    {
        // listen to notifications
        context.getWiki().getNotificationManager().addGeneralRule(new DocChangeRule(this));
        // init activitystream cleaner
        ActivityStreamCleaner.getInstance().init(context);
    }

    protected void prepareEvent(ActivityEvent event, XWikiDocument doc, XWikiContext context)
    {
        if (doc == null)
            doc = context.getDoc();

        if (event.getUser() == null) {
            event.setUser(context.getUser());
        }
        if (event.getStream() == null) {
            String space = (doc == null) ? "" : doc.getSpace();

            event.setStream(getStreamName(space, context));
        }
        if (event.getWiki() == null) {
            event.setWiki(context.getDatabase());
        }
        if (event.getSpace() == null) {
            event.setSpace((doc == null) ? "" : doc.getSpace());
        }
        if (event.getPage() == null) {
            event.setPage((doc == null) ? "" : doc.getFullName());
        }
        if (event.getUrl() == null) {
            event.setUrl((doc == null) ? "" : doc.getURL("view", context));
        }
        if (event.getApplication() == null) {
            event.setApplication("xwiki");
        }
        if (event.getDate() == null) {
            event.setDate(context.getWiki().getCurrentDate());
        }
        if (event.getEventId() == null) {
            event.setEventId(generateEventId(event, context));
        }
        if (event.getRequestId() == null) {
            event.setRequestId((String) context.get("activitystream_requestid"));
        }
    }

    public String getStreamName(String space, XWikiContext context)
    {
        return space;
    }

    protected String generateEventId(ActivityEvent event, XWikiContext context)
    {
        String key =
            event.getStream() + "-" + event.getApplication() + "-" + event.getWiki() + ":" + event.getPage() + "-"
                + event.getType();
        long hash = key.hashCode();
        if (hash < 0)
            hash = -hash;

        String id = "" + hash + "-" + event.getDate().getTime() + "-" + RandomStringUtils.randomAlphanumeric(8);
        if (context.get("activitystream_requestid") == null) {
            context.put("activitystream_requestid", id);
        }
        return id;
    }

    public void addActivityEvent(ActivityEvent event, XWikiContext context) throws ActivityStreamException
    {
        addActivityEvent(event, null, context);
    }

    /**
     * This method determine if events must be store in the local wiki. If the wiki is not running in virtual mode this
     * method will always return true. If it is running in virtual and if the activitystream is set not to store events
     * in the main wiki the method will always return true. It the configuration does not match those 2 conditions, the
     * method retrieves the platform.plugin.activitystream.uselocalstore configuration option. If the option is not
     * found the method returns true (default behavior).
     * 
     * @param context the XWiki context
     * @return true if the activity stream is configured to store events in the main wiki, false otherwise
     */
    private boolean useLocalStore(XWikiContext context)
    {
        if (!context.getWiki().isVirtualMode()) {
            // If we aren't in virtual mode, force local store.
            return true;
        } else if (!useMainStore(context)) {
            // If we are in virtual mode but the main store is disabled, force local store.
            return true;
        }

        ActivityStreamPlugin plugin = (ActivityStreamPlugin) context.getWiki().getPlugin("activitystream", context);
        return Integer.parseInt(plugin.getActivityStreamPreference("uselocalstore", "1", context)) == 1;
    }

    /**
     * This method determine if events must be store in the main wiki. If the wiki is not running in virtual mode this
     * method will always return false. If it is running in virtual mode this method retrieves the
     * platform.plugin.activitystream.usemainstore configuration option. If the option is not found the method returns
     * true (default behavior).
     * 
     * @param context the XWiki context
     * @return true if the activity stream is configured to store events in the main wiki, false otherwise
     */
    private boolean useMainStore(XWikiContext context)
    {
        if (!context.getWiki().isVirtualMode()) {
            // If we aren't in virtual mode, local store is forced.
            return false;
        }

        if (context.getWiki().isVirtualMode() && context.getDatabase().equals(context.getMainXWiki())) {
            // We're in the main database, we don't have to store the data twice.
            return false;
        }
        
        ActivityStreamPlugin plugin = (ActivityStreamPlugin) context.getWiki().getPlugin("activitystream", context);
        return Integer.parseInt(plugin.getActivityStreamPreference("usemainstore", "1", context)) == 1;
    }

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
            String oriDatabase = context.getDatabase();
            context.setDatabase(context.getMainXWiki());
            XWikiHibernateStore mainHibernateStore = context.getWiki().getHibernateStore();
            try {
                mainHibernateStore.beginTransaction(context);
                Session session = mainHibernateStore.getSession(context);
                session.save(event);
                mainHibernateStore.endTransaction(context, true);
            } catch (XWikiException e) {
                mainHibernateStore.endTransaction(context, false);
            } finally {
                context.setDatabase(oriDatabase);
            }
        }
    }

    public void addActivityEvent(String streamName, String type, String title, XWikiContext context)
        throws ActivityStreamException
    {
        addActivityEvent(streamName, type, title, null, context);
    }

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

    public void addDocumentActivityEvent(String streamName, XWikiDocument doc, String type, String title,
        XWikiContext context) throws ActivityStreamException
    {
        addDocumentActivityEvent(streamName, doc, type, ActivityEventPriority.NOTIFICATION, title, null, context);
    }

    public void addDocumentActivityEvent(String streamName, XWikiDocument doc, String type, int priority, String title,
        XWikiContext context) throws ActivityStreamException
    {
        addDocumentActivityEvent(streamName, doc, type, priority, title, null, context);
    }

    public void addDocumentActivityEvent(String streamName, XWikiDocument doc, String type, String title,
        List<String> params, XWikiContext context) throws ActivityStreamException
    {
        addDocumentActivityEvent(streamName, doc, type, ActivityEventPriority.NOTIFICATION, title, params, context);
    }

    public void addDocumentActivityEvent(String streamName, XWikiDocument doc, String type, int priority, String title,
        List<String> params, XWikiContext context) throws ActivityStreamException
    {
        ActivityEvent event = newActivityEvent();
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
        addActivityEvent(event, doc, context);
    }

    private ActivityEventImpl loadActivityEvent(ActivityEvent ev, boolean bTransaction, XWikiContext context)
        throws ActivityStreamException
    {
        ActivityEventImpl act = null;
        String eventId = ev.getEventId();

        if (useLocalStore(context)) {
            // load event from the local database
            XWikiHibernateStore hibstore = context.getWiki().getHibernateStore();
            try {
                if (bTransaction) {
                    hibstore.checkHibernate(context);
                    bTransaction = hibstore.beginTransaction(false, context);
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

                if (bTransaction)
                    hibstore.endTransaction(context, false, false);
            } catch (Exception e) {
                throw new ActivityStreamException();
            } finally {
                try {
                    if (bTransaction) {
                        hibstore.endTransaction(context, false, false);
                    }
                } catch (Exception e) {
                }
            }
        } else if (useMainStore(context)) {
            // load event from the main database
            String oriDatabase = context.getDatabase();
            context.setDatabase(context.getMainXWiki());
            XWikiHibernateStore hibstore = context.getWiki().getHibernateStore();
            try {
                if (bTransaction) {
                    hibstore.checkHibernate(context);
                    bTransaction = hibstore.beginTransaction(false, context);
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

                if (bTransaction)
                    hibstore.endTransaction(context, false, false);
            } catch (Exception e) {
                throw new ActivityStreamException();
            } finally {
                context.setDatabase(oriDatabase);
                try {
                    if (bTransaction) {
                        hibstore.endTransaction(context, false, false);
                    }
                } catch (Exception e) {
                }
            }
        }

        return act;
    }

    public void deleteActivityEvent(ActivityEvent event, XWikiContext context) throws ActivityStreamException
    {
        boolean bTransaction = true;
        ActivityEventImpl evImpl = loadActivityEvent(event, true, context);
        String oriDatabase = context.getDatabase();

        if (useLocalStore(context)) {
            XWikiHibernateStore hibstore;
            
            // delete event from the local database
            if (context.getDatabase().equals(event.getWiki())) {
                hibstore = context.getWiki().getHibernateStore();
            } else {
                context.setDatabase(event.getWiki());
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
                    if (context.getDatabase().equals(oriDatabase)) {
                        context.setDatabase(oriDatabase);
                    }
                } catch (Exception e) {
                }
            }
        }

        if (useMainStore(context)) {
            // delete event from the main database
            context.setDatabase(context.getMainXWiki());
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
                    context.setDatabase(oriDatabase);
                } catch (Exception e) {
                }
            }
        }
    }

    public List<ActivityEvent> searchEvents(String hql, boolean filter, int nb, int start, XWikiContext context) 
        throws ActivityStreamException
    {
        return searchEvents("", hql, filter, nb, start, context);
    }

    public List<ActivityEvent> searchEvents(String hql, boolean filter, boolean globalSearch, int nb, int start, 
        XWikiContext context) throws ActivityStreamException
    {
        return searchEvents("", hql, filter, globalSearch, nb, start, context);
    }

    public List<ActivityEvent> searchEvents(String hql, boolean filter, boolean globalSearch, int nb, int start, 
        List<Object> parameterValues, XWikiContext context) throws ActivityStreamException
    {
        return searchEvents("", hql, filter, globalSearch, nb, start, parameterValues, context);
    }

    /**
     * Alternate searchEvents function for the Activiy Stream
     * 
     * @param fromHql
     * @param hql
     * @param filter
     * @param nb
     * @param start
     * @param context
     * @return
     * @throws ActivityStreamException
     */
    public List<ActivityEvent> searchEvents(String fromHql, String hql, boolean filter, int nb, int start,
        XWikiContext context) throws ActivityStreamException
    {
        return searchEvents(fromHql, hql, filter, nb, start, null, context);
    }
    
    public List<ActivityEvent> searchEvents(String fromHql, String hql, boolean filter, boolean globalSearch, int nb, 
        int start, XWikiContext context) throws ActivityStreamException
    {
        return searchEvents(fromHql, hql, filter, globalSearch, nb, start, null, context);
    }
    
    public List<ActivityEvent> searchEvents(String fromHql, String hql, boolean filter, int nb, int start, 
        List<Object> parameterValues, XWikiContext context) throws ActivityStreamException
    {
        return searchEvents(fromHql, hql, filter, false, nb, start, parameterValues, context);
    }

    public List<ActivityEvent> searchEvents(String fromHql, String hql, boolean filter, boolean globalSearch, int nb,
        int start, List<Object> parameterValues, XWikiContext context) throws ActivityStreamException
    {
        StringBuffer searchHql = new StringBuffer();
        List<ActivityEvent> results;

        if (filter) {
            searchHql.append("select act from ActivityEventImpl as act, ActivityEventImpl as act2 ");
            searchHql.append(fromHql);
            searchHql.append(" where act.eventId=act2.eventId and ");
            searchHql.append(hql);
            searchHql.append(" group by act.requestId having (act.priority)=max(act2.priority) order by act.date desc");
        } else {
            searchHql.append("select act from ActivityEventImpl as act ");
            searchHql.append(fromHql);
            searchHql.append(" where ");
            searchHql.append(hql);
            searchHql.append(" order by act.date desc");
        }

        if (globalSearch) {
            // Search in the main database
            String oriDatabase = context.getDatabase();
            try {
                context.setDatabase(context.getMainXWiki());
                results =
                    context.getWiki().getStore().search(searchHql.toString(), nb, start, parameterValues, context);
            } catch (XWikiException e) {
                throw new ActivityStreamException(e);
            } finally {
                context.setDatabase(oriDatabase);
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

    public List<ActivityEvent> getEvents(boolean filter, int nb, int start, XWikiContext context)
        throws ActivityStreamException
    {
        return searchEvents("1=1", filter, nb, start, context);
    }

    public List<ActivityEvent> getEventsForSpace(String space, boolean filter, int nb, int start, XWikiContext context)
        throws ActivityStreamException
    {
        return searchEvents("act.space='" + space + "'", filter, nb, start, context);
    }

    public List<ActivityEvent> getEventsForUser(String user, boolean filter, int nb, int start, XWikiContext context)
        throws ActivityStreamException
    {
        return searchEvents("act.user='" + user + "'", filter, nb, start, context);
    }

    public List<ActivityEvent> getEvents(String stream, boolean filter, int nb, int start, XWikiContext context)
        throws ActivityStreamException
    {
        return searchEvents("act.stream='" + stream + "'", filter, nb, start, context);
    }

    public List<ActivityEvent> getEventsForSpace(String stream, String space, boolean filter, int nb, int start,
        XWikiContext context) throws ActivityStreamException
    {
        return searchEvents("act.space='" + space + "' and act.stream='" + stream + "'", filter, nb, start, context);
    }

    public List<ActivityEvent> getEventsForUser(String stream, String user, boolean filter, int nb, int start,
        XWikiContext context) throws ActivityStreamException
    {
        return searchEvents("act.user='" + user + "' and act.stream='" + stream + "'", filter, nb, start, context);
    }

    protected ActivityEvent newActivityEvent()
    {
        return new ActivityEventImpl();
    }

    public void notify(XWikiNotificationRule rule, XWikiDocument newdoc, XWikiDocument olddoc, int event,
        XWikiContext context)
    {
        ArrayList<String> params = new ArrayList<String>();
        params.add(0, newdoc.getDisplayTitle(context));
        String msgPrefix = "activitystream.event.";
        String streamName = getStreamName(newdoc.getSpace(), context);

        if (streamName == null)
            return;

        try {
            switch (event) {
                case XWikiDocChangeNotificationInterface.EVENT_CHANGE:
                    if (olddoc == null || olddoc.isNew())
                        addDocumentActivityEvent(streamName, newdoc, ActivityEventType.CREATE, msgPrefix
                            + ActivityEventType.CREATE, params, context);
                    else if (newdoc == null || newdoc.isNew())
                        addDocumentActivityEvent(streamName, newdoc, ActivityEventType.DELETE, msgPrefix
                            + ActivityEventType.DELETE, params, context);
                    else
                        addDocumentActivityEvent(streamName, newdoc, ActivityEventType.UPDATE, msgPrefix
                            + ActivityEventType.UPDATE, params, context);
                    break;
                case XWikiDocChangeNotificationInterface.EVENT_NEW:
                    addDocumentActivityEvent(streamName, newdoc, ActivityEventType.CREATE, msgPrefix
                        + ActivityEventType.CREATE, params, context);
                    break;
                case XWikiDocChangeNotificationInterface.EVENT_DELETE:
                    addDocumentActivityEvent(streamName, newdoc, ActivityEventType.DELETE, msgPrefix
                        + ActivityEventType.DELETE, params, context);
                    break;
                case XWikiDocChangeNotificationInterface.EVENT_UPDATE_CONTENT:
                    addDocumentActivityEvent(streamName, newdoc, ActivityEventType.UPDATE, msgPrefix
                        + ActivityEventType.UPDATE, params, context);
                    break;
                case XWikiDocChangeNotificationInterface.EVENT_UPDATE_OBJECT:
                    addDocumentActivityEvent(streamName, newdoc, ActivityEventType.UPDATE, msgPrefix
                        + ActivityEventType.UPDATE, params, context);
                    break;
                case XWikiDocChangeNotificationInterface.EVENT_UPDATE_CLASS:
                    addDocumentActivityEvent(streamName, newdoc, ActivityEventType.UPDATE, msgPrefix
                        + ActivityEventType.UPDATE, params, context);
                    break;
            }
        } catch (Throwable e) {
            // Error in activity stream notify should be ignored but logged in the log file
            e.printStackTrace();
        }
    }

    public SyndEntry getFeedEntry(ActivityEvent event, XWikiContext context)
    {
        return getFeedEntry(event, "", context);
    }

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

    public SyndFeed getFeed(List<ActivityEvent> events, XWikiContext context)
    {
        return getFeed(events, "", context);
    }

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

    public SyndFeed getFeed(List<ActivityEvent> events, String author, String title, String description,
        String copyright, String encoding, String url, XWikiContext context)
    {
        return getFeed(events, author, title, description, copyright, encoding, url, "", context);
    }

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

    public String getFeedOutput(List<ActivityEvent> events, String author, String title, String description,
        String copyright, String encoding, String url, String type, XWikiContext context)
    {
        return getFeedOutput(events, author, title, description, copyright, encoding, url, type, "", context);
    }

    public String getFeedOutput(List<ActivityEvent> events, String author, String title, String description,
        String copyright, String encoding, String url, String type, String suffix, XWikiContext context)
    {
        SyndFeed feed = getFeed(events, author, title, description, copyright, encoding, url, suffix, context);
        return getFeedOutput(feed, type);
    }

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

}
