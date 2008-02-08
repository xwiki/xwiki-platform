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

public class ActivityStreamImpl implements ActivityStream, XWikiDocChangeNotificationInterface
{
    public void initClasses(XWikiContext context) throws XWikiException
    {
        // listen to notifications
        context.getWiki().getNotificationManager().addGeneralRule(
            new DocChangeRule(this, true, true));
    }

    protected void prepareEvent(ActivityEvent event, XWikiContext context)
    {
        if (event.getUser() == null) {
            event.setUser(context.getUser());
        }
        if (event.getStream() == null) {
            event.setStream(context.getUser());
        }
        if (event.getWiki() == null) {
            event.setWiki(context.getDatabase());
        }
        if (event.getSpace() == null) {
            event.setSpace(context.getDoc().getSpace());
        }
        if (event.getPage() == null) {
            event.setPage(context.getDoc().getFullName());
        }
        if (event.getUrl() == null) {
            event.setUrl(context.getDoc().getURL("view", context));
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

    protected String generateEventId(ActivityEvent event, XWikiContext context)
    {
        return event.getStream() + "-" + event.getApplication() + "-" + event.getWiki() + ":"
            + event.getPage() + "-" + event.getType() + "-" + event.getDate().getTime() + "-"
            + RandomStringUtils.randomAlphanumeric(4);
    }

    public void addActivityEvent(ActivityEvent event, XWikiContext context)
        throws ActivityStreamException
    {
        prepareEvent(event, context);

        // store event using hibernate
        XWikiHibernateStore hibstore = context.getWiki().getHibernateStore();
        try {
            hibstore.beginTransaction(context);
            Session session = hibstore.getSession(context);
            session.saveOrUpdate(event);
            hibstore.endTransaction(context, true);
        } catch (XWikiException e) {
            hibstore.endTransaction(context, false);
        }
    }

    public void addActivityEvent(String type, String title, XWikiContext context)
        throws ActivityStreamException
    {
        ActivityEvent event = newActivityEvent();
        event.setType(type);
        event.setTitle(title);
        event.setBody(title);
        addActivityEvent(event, context);
    }

    public void addDocumentActivityEvent(XWikiDocument doc, String type, String title,
        XWikiContext context) throws ActivityStreamException
    {
        ActivityEvent event = newActivityEvent();
        event.setPage(doc.getFullName());
        if (doc.getDatabase() != null) {
            event.setWiki(doc.getDatabase());
        }
        event.setDate(doc.getDate());
        event.setPriority(ActivityEventPriority.ACTION);
        event.setType(type);
        event.setTitle(title);
        event.setBody(title);
        addActivityEvent(event, context);
    }

    public List searchEvents(String hql, boolean filter, int nb, int start, XWikiContext context)
        throws ActivityStreamException
    {
        String searchHql = "select act.* from ActivityEvent as act " + hql;
        String filterQuery =
            ", ActivityEvent act2 where act.eventId=act2.eventId and act.priority=max(act2.priority)";
        if (filter) {
            searchHql += filterQuery;
        }

        if (!hql.contains("order by")) {
            searchHql += " order by act.date desc";
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
        return searchEvents("where act.space='" + space + "'", filter, nb, start, context);
    }

    public List getEventsForUser(String user, boolean filter, int nb, int start,
        XWikiContext context) throws ActivityStreamException
    {
        return searchEvents("where act.user='" + user + "'", filter, nb, start, context);
    }

    public List getEvents(String stream, boolean filter, int nb, int start, XWikiContext context)
        throws ActivityStreamException
    {
        return searchEvents("where act.stream='" + stream + "'", filter, nb, start, context);
    }

    public List getEventsForSpace(String stream, String space, boolean filter, int nb, int start,
        XWikiContext context) throws ActivityStreamException
    {
        return searchEvents("where act.space='" + space + "' and act.stream='" + stream + "'",
            filter, nb, start, context);
    }

    public List getEventsForUser(String stream, String user, boolean filter, int nb, int start,
        XWikiContext context) throws ActivityStreamException
    {
        return searchEvents("where act.user='" + user + "' and act.stream='" + stream + "'",
            filter, nb, start, context);
    }

    protected ActivityEvent newActivityEvent()
    {
        return new ActivityEventImpl();
    }

    public void notify(XWikiNotificationRule rule, XWikiDocument newdoc, XWikiDocument olddoc,
        int event, XWikiContext context)
    {
        try {
            switch (event) {
                case /* EVENT_CHANGE */0:
                    addActivityEvent(ActivityEventType.UPDATE, null, context);
                    break;
                case /* EVENT_NEW */1:
                    addActivityEvent(ActivityEventType.CREATE, null, context);
                    break;
                case /* EVENT_DELETE */2:
                    addActivityEvent(ActivityEventType.DELETE, null, context);
                    break;
                case /* EVENT_UPDATE_CONTENT */3:
                    addActivityEvent(ActivityEventType.UPDATE, null, context);
                    break;
                case /* EVENT_UPDATE_OBJECT */4:
                    break;
                case /* EVENT_UPDATE_CLASS */5:
                    break;
            }
        } catch (ActivityStreamException e) {
            // ignore
        }
    }
}
