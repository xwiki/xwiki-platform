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
package org.xwiki.plugin.activitystream.impl;

import java.util.List;
import java.util.ArrayList;
import java.io.StringWriter;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;

import org.apache.commons.lang.RandomStringUtils;
import org.hibernate.Session;
import org.xwiki.plugin.activitystream.api.ActivityEvent;
import org.xwiki.plugin.activitystream.api.ActivityEventPriority;
import org.xwiki.plugin.activitystream.api.ActivityEventType;
import org.xwiki.plugin.activitystream.api.ActivityStream;
import org.xwiki.plugin.activitystream.api.ActivityStreamException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.notify.DocChangeRule;
import com.xpn.xwiki.notify.XWikiDocChangeNotificationInterface;
import com.xpn.xwiki.notify.XWikiNotificationRule;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.SyndFeedOutput;
import com.sun.syndication.io.FeedException;

public class ActivityStreamImpl implements ActivityStream, XWikiDocChangeNotificationInterface
{
    public void initClasses(XWikiContext context) throws XWikiException
    {
        // listen to notifications
        context.getWiki().getNotificationManager().addGeneralRule(
                new DocChangeRule(this));
    }

    protected void prepareEvent(ActivityEvent event, XWikiDocument doc, XWikiContext context)
    {
        if (doc==null)
            doc = context.getDoc();

        if (event.getUser() == null) {
            event.setUser(context.getUser());
        }
        if (event.getStream() == null) {
            String space =  (doc==null) ? "" : doc.getSpace();

            event.setStream(getStreamName(space));
        }
        if (event.getWiki() == null) {
            event.setWiki(context.getDatabase());
        }
        if (event.getSpace() == null) {
            event.setSpace((doc==null) ? "" : doc.getSpace());
        }
        if (event.getPage() == null) {
            event.setPage((doc==null) ? "" : doc.getFullName());
        }
        if (event.getUrl() == null) {
            event.setUrl((doc==null) ? "" : doc.getURL("view", context));
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

    public String getStreamName(String space) {
        return space;
    }

    protected String generateEventId(ActivityEvent event, XWikiContext context)
    {
        String key = event.getStream() + "-" + event.getApplication() + "-" + event.getWiki() + ":"
                + event.getPage() + "-" + event.getType();
        long hash = key.hashCode();
        if (hash<0)
         hash = -hash;

        String id =  "" + hash +  "-" + event.getDate().getTime() + "-"
                + RandomStringUtils.randomAlphanumeric(8);
        if (context.get("activitystream_requestid")==null) {
            context.put("activitystream_requestid", id);
        }
        return id;
    }

    public void addActivityEvent(ActivityEvent event, XWikiContext context)
            throws ActivityStreamException
    {
        addActivityEvent(event, null, context);
    }

    public void addActivityEvent(ActivityEvent event, XWikiDocument doc, XWikiContext context)
            throws ActivityStreamException
    {
        prepareEvent(event, doc, context);

        // store event using hibernate
        XWikiHibernateStore hibstore = context.getWiki().getHibernateStore();
        try {
            hibstore.beginTransaction(context);
            Session session = hibstore.getSession(context);
            session.save(event);
            hibstore.endTransaction(context, true);
        } catch (XWikiException e) {
            hibstore.endTransaction(context, false);
        }
    }

    public void addActivityEvent(String streamName, String type, String title, XWikiContext context)
            throws ActivityStreamException
    {
        addActivityEvent(streamName, type, title, null, context);
    }

    public void addActivityEvent(String streamName, String type, String title, List params, XWikiContext context)
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
                                         XWikiContext context) throws ActivityStreamException {
        addDocumentActivityEvent(streamName, doc, type, priority, title, null, context);
    }

    public void addDocumentActivityEvent(String streamName, XWikiDocument doc, String type, String title, List params,
                                         XWikiContext context) throws ActivityStreamException
    {
        addDocumentActivityEvent(streamName, doc, type, ActivityEventPriority.NOTIFICATION, title, params, context);
    }

    public void addDocumentActivityEvent(String streamName, XWikiDocument doc, String type, int priority, String title, List params,
                                         XWikiContext context) throws ActivityStreamException
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
        event.setParams(params);
        addActivityEvent(event, doc, context);
    }

    public List searchEvents(String hql, boolean filter, int nb, int start, XWikiContext context)
            throws ActivityStreamException
    {
        String searchHql;

        if (filter) {
            searchHql = "select act from ActivityEventImpl as act, ActivityEventImpl as act2 where act.eventId=act2.eventId and " + hql + " group by act.requestId having (act.priority)=max(act2.priority) order by act.date desc";
        } else {
            searchHql = "select act from ActivityEventImpl as act where " + hql + " order by act.date desc";
        }

        try {
            return context.getWiki().search(searchHql, nb, start, context);
        } catch (XWikiException e) {
            throw new ActivityStreamException(e);
        }
    }

    public List getEvents(boolean filter, int nb, int start, XWikiContext context)
            throws ActivityStreamException
    {
        return searchEvents("", filter, nb, start, context);
    }

    public List getEventsForSpace(String space, boolean filter, int nb, int start,
                                  XWikiContext context) throws ActivityStreamException
    {
        return searchEvents("act.space='" + space + "'", filter, nb, start, context);
    }

    public List getEventsForUser(String user, boolean filter, int nb, int start,
                                 XWikiContext context) throws ActivityStreamException
    {
        return searchEvents("act.user='" + user + "'", filter, nb, start, context);
    }

    public List getEvents(String stream, boolean filter, int nb, int start, XWikiContext context)
            throws ActivityStreamException
    {
        return searchEvents("act.stream='" + stream + "'", filter, nb, start, context);
    }

    public List getEventsForSpace(String stream, String space, boolean filter, int nb, int start,
                                  XWikiContext context) throws ActivityStreamException
    {
        return searchEvents("act.space='" + space + "' and act.stream='" + stream + "'",
                filter, nb, start, context);
    }

    public List getEventsForUser(String stream, String user, boolean filter, int nb, int start,
                                 XWikiContext context) throws ActivityStreamException
    {
        return searchEvents("act.user='" + user + "' and act.stream='" + stream + "'",
                filter, nb, start, context);
    }

    protected ActivityEvent newActivityEvent()
    {
        return new ActivityEventImpl();
    }

    public void notify(XWikiNotificationRule rule, XWikiDocument newdoc, XWikiDocument olddoc,
                       int event, XWikiContext context)
    {
        ArrayList params = new ArrayList();
        params.set(0, newdoc.getDisplayTitle(context));

        String streamName = getStreamName(newdoc.getSpace());

        if (streamName==null)
         return;

        try {
            switch (event) {
                case XWikiDocChangeNotificationInterface.EVENT_CHANGE:
                    addDocumentActivityEvent(streamName, newdoc, ActivityEventType.UPDATE, "as_document_has_been_updated", params, context);
                    break;
                case XWikiDocChangeNotificationInterface.EVENT_NEW:
                    addDocumentActivityEvent(streamName, newdoc, ActivityEventType.CREATE, "as_document_has_been_created", params, context);
                    break;
                case XWikiDocChangeNotificationInterface.EVENT_DELETE:
                    addDocumentActivityEvent(streamName, newdoc, ActivityEventType.DELETE, "as_document_has_been_deleted", params, context);
                    break;
                case XWikiDocChangeNotificationInterface.EVENT_UPDATE_CONTENT:
                    addDocumentActivityEvent(streamName, newdoc, ActivityEventType.UPDATE, "as_document_has_been_updated", params, context);
                    break;
                case XWikiDocChangeNotificationInterface.EVENT_UPDATE_OBJECT:
                    addDocumentActivityEvent(streamName, newdoc, ActivityEventType.UPDATE, "as_document_has_been_updated", params, context);
                    break;
                case XWikiDocChangeNotificationInterface.EVENT_UPDATE_CLASS:
                    addDocumentActivityEvent(streamName, newdoc, ActivityEventType.UPDATE, "as_document_has_been_updated", params, context);
                    break;
            }
        } catch (Throwable e) {
            // Error in activity stream notify should be ignored but logged in the log file
            e.printStackTrace();
        }
    }


    public SyndEntry getFeedEntry(ActivityEvent event, XWikiContext context) {
        SyndEntry entry = new SyndEntryImpl();
        String user = event.getUser();
        String displayUser = context.getWiki().getUserName(user, null, false, context);
        entry.setAuthor(displayUser);
        entry.setTitle(event.getDisplayTitle(context));
        // entry.setDescription(event.getDisplayBody(context));
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

    public SyndFeed getFeed(List events, XWikiContext context) {
        SyndFeed feed = new SyndFeedImpl();
        List entries = new ArrayList();
        for (int i=0;i<events.size();i++) {
           ActivityEvent event = (ActivityEvent) events.get(i);
           SyndEntry entry = getFeedEntry(event, context);
           entries.add(entry);
        }
        feed.setEntries(entries);
        return feed;
    }

    public SyndFeed getFeed(List events, String author, String title, String description, String copyright, String encoding, String url, XWikiContext context) {
        SyndFeed feed = getFeed(events, context);
        feed.setAuthor(author);
        feed.setDescription(description);
        feed.setCopyright(copyright);
        feed.setEncoding(encoding);
        feed.setLink(url);
        feed.setTitle(title);
        return feed;
    }

    public String getFeedOutput(List events, String author, String title, String description, String copyright, String encoding, String url, String type, XWikiContext context) {
        SyndFeed feed = getFeed(events, author, title, description, copyright, encoding, url, context);
        return getFeedOutput(feed, type);
    }

    public String getFeedOutput(SyndFeed feed, String type) {
        feed.setFeedType(type);
        StringWriter writer = new StringWriter();
        SyndFeedOutput output = new SyndFeedOutput();
        try {
            output.output(feed,writer);
            writer.close();
            return writer.toString();
        } catch (Exception e) {
            return "";
        }
    }

}
