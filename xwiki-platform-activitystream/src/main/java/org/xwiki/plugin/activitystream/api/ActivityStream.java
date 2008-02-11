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
package org.xwiki.plugin.activitystream.api;

import java.util.List;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Manages the activity stream
 */
public interface ActivityStream
{
    /**
     * Creates the classes used by the activity stream when necessary
     */
    void initClasses(XWikiContext context) throws XWikiException;

    /**
     * Tranforms space name into stream name
     * @param space
     * @return
     */
    String getStreamName(String space);

    /**
     * Adding and activity event. The Id does not need to be filled as it will be created. Date and
     * Wiki are optional
     * 
     * @param event
     * @param context
     * @throws ActivityStreamException
     */
    void addActivityEvent(ActivityEvent event, XWikiContext context)
        throws ActivityStreamException;

    void addActivityEvent(String streamName, String type, String title, XWikiContext context)
        throws ActivityStreamException;

    void addActivityEvent(String streamName, String type, String title, List params, XWikiContext context)
        throws ActivityStreamException;

    void addDocumentActivityEvent(String streamName, XWikiDocument doc, String type, String title,
        XWikiContext context) throws ActivityStreamException;

    void addDocumentActivityEvent(String streamName, XWikiDocument doc, String type, int priority, String title,
        XWikiContext context) throws ActivityStreamException;

    void addDocumentActivityEvent(String streamName, XWikiDocument doc, String type, String title, List params,
        XWikiContext context) throws ActivityStreamException;

    void addDocumentActivityEvent(String streamName, XWikiDocument doc, String type, int priority, String title, List params,
        XWikiContext context) throws ActivityStreamException;

    List searchEvents(String hql, boolean filter, int nb, int start, XWikiContext context)
        throws ActivityStreamException;

    List getEvents(boolean filter, int nb, int start, XWikiContext context)
        throws ActivityStreamException;

    List getEventsForSpace(String space, boolean filter, int nb, int start, XWikiContext context)
        throws ActivityStreamException;

    List getEventsForUser(String user, boolean filter, int nb, int start, XWikiContext context)
        throws ActivityStreamException;

    List getEvents(String streamName, boolean filter, int nb, int start, XWikiContext context)
        throws ActivityStreamException;

    List getEventsForSpace(String streamName, String space, boolean filter, int nb, int start,
        XWikiContext context) throws ActivityStreamException;

    List getEventsForUser(String streamName, String user, boolean filter, int nb, int start,
        XWikiContext context) throws ActivityStreamException;
}
