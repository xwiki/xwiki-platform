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

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.xpn.xwiki.XWikiContext;

/**
 * An Activity Event represents an event done by a user in a certain context.
 * 
 * @version $Id$
 */
public interface ActivityEvent
{
    /**
     * Each event has a unique ID.
     * 
     * @return the unique ID of the event
     */
    String getEventId();

    /**
     * @param id the unique ID of the event to set
     * @see #getEventId()
     */
    void setEventId(String id);

    /**
     * Multiple events can correspond to the same activity, so events can be grouped under the same request ID.
     * 
     * @return the event group ID
     */
    String getRequestId();

    /**
     * @param id the event group ID
     * @see #getRequestId()
     */
    void setRequestId(String id);

    /**
     * Different events can have a different priority. This allows to determine which events are more or less important
     * in the same request, or which events are important in the stream. For example, annotation automatic updates are
     * less important in a group of changes triggered by a document update, while a major version is more important than
     * a minor version.
     * 
     * @return the priority of the event
     */
    int getPriority();

    /**
     * @param priority the priority of the event
     * @see #getPriority()
     */
    void setPriority(int priority);

    /**
     * @return The type of the event
     */
    String getType();

    /**
     * @param type The type of the event
     * @see #getType()
     */
    void setType(String type);

    /**
     * Events can be triggered by different applications, not only the main wiki update events: user statuses, blog
     * pingbacks, new extension added...
     * 
     * @return the application name
     */
    String getApplication();

    /**
     * @param application the application Name
     * @see #getApplication()
     */
    void setApplication(String application);

    /**
     * Events can happen in different contexts; for example, wiki activity events happen in different spaces, user
     * statuses are targeted to different groups.
     * 
     * @return the stream name
     */
    String getStream();

    /**
     * @param stream the stream Name
     * @see #getStream()
     */
    void setStream(String stream);

    /**
     * An event happens at a precise date.
     * 
     * @return the event date
     */
    Date getDate();

    /**
     * @param date the event date
     * @see #getDate()
     */
    void setDate(Date date);

    /**
     * Event usually occur as the result of a registered user activity.
     * 
     * @return the wiki name of the user creating the event
     */
    String getUser();

    /**
     * @param user the wiki name of the user creating the event
     * @see #getUser()
     */
    void setUser(String user);

    /**
     * In a wiki farm, each event happens in one of the wikis.
     * 
     * @return the wiki name in which the event was created
     */
    String getWiki();

    /**
     * @param wiki the wiki name in which the event was created
     * @see #getWiki()
     */
    void setWiki(String wiki);

    /**
     * Document-related events target a certain document, and documents belong to a space; this is the space of the
     * target document.
     * 
     * @return the space name in which the event was created
     */
    String getSpace();

    /**
     * @param space the space name in which the event was created
     * @see #getSpace()
     */
    void setSpace(String space);

    /**
     * Document-related events target a certain document, and documents have a name; this is the name of the target
     * document.
     * 
     * @return the page of the event
     */
    String getPage();

    /**
     * @param page the page of the event
     * @see #getPage()
     */
    void setPage(String page);

    /**
     * If an event happens in an URL-accessible location (a document), or if the event itself can be seen at a given
     * URL, this field stores that URL.
     * 
     * @return the URL related to the event
     */
    String getUrl();

    /**
     * @param url the URL related to the event
     * @see #getUrl()
     */
    void setUrl(String url);

    /**
     * A name for the event.
     * 
     * @return the title of the event
     */
    String getTitle();

    /**
     * @param title the title of the event
     * @see #getTitle()
     */
    void setTitle(String title);

    /**
     * A larger section of text where the event can store some data.
     * 
     * @return the body of the event
     */
    String getBody();

    /**
     * @param body the body of the event
     * @see #getBody()
     */
    void setBody(String body);

    /**
     * For events related to documents, this field records the version of the document at the time when the event
     * occurred.
     * 
     * @return the document version when the event occurred
     */
    String getVersion();

    /**
     * @param version the document version when the event occurred
     * @see #getVersion()
     */
    void setVersion(String version);

    /**
     * Free form parameter to be used by the event consumer.
     * 
     * @return the first parameter associated with the event
     */
    String getParam1();

    /**
     * @param param1 the first parameter associated with the event
     * @see #getParam1()
     */
    void setParam1(String param1);

    /**
     * Free form parameter to be used by the event consumer.
     * 
     * @return the second parameter associated with the event
     */
    String getParam2();

    /**
     * @param param2 the second parameter associated with the event
     * @see #getParam2()
     */
    void setParam2(String param2);

    /**
     * Free form parameter to be used by the event consumer.
     * 
     * @return the third parameter associated with the event
     */
    String getParam3();

    /**
     * @param param3 the third parameter associated with the event
     * @see #getParam3()
     */
    void setParam3(String param3);

    /**
     * Free form parameter to be used by the event consumer.
     * 
     * @return the fourth parameter associated with the event
     */
    String getParam4();

    /**
     * @param param4 the fourth parameter associated with the event
     * @see #getParam4()
     */
    void setParam4(String param4);

    /**
     * Free form parameter to be used by the event consumer.
     * 
     * @return the fifth parameter associated with the event
     */
    String getParam5();

    /**
     * @param param5 the fifth parameter associated with the event
     * @see #getParam5()
     */
    void setParam5(String param5);

    /**
     * Get a more user-friendly, or a localized version of the {@link #getTitle() event title}.
     * 
     * @param context the XWiki context
     * @return the event title in a human readable format
     * @see #getTitle()
     */
    String getDisplayTitle(XWikiContext context);

    /**
     * Get a more user-friendly, or a localized version of the {@link #getBody() event body}.
     * 
     * @param context the XWiki context
     * @return the event body in a human readable format
     * @see #getBody()
     */
    String getDisplayBody(XWikiContext context);

    /**
     * Get a formatted, localized version of the {@link #getDate() event date}.
     * 
     * @param context the XWiki context
     * @return the event date in a human readable format
     * @see #getDate()
     */
    String getDisplayDate(XWikiContext context);

    /**
     * Get the {@link #getUser() user name} as a HTML link.
     * 
     * @param context the XWiki context
     * @return the event user in a human readable format (example: Administrator instead of XWiki.Admin), as a HTML
     *         fragment
     */
    String getDisplayUser(XWikiContext context);

    /**
     * Set all parameters at once.
     * 
     * @param params parameters to set, a list of at most 5 entries
     */
    void setParams(List<String> params);

    /**
     * Associates name-value pair parameters with this event.
     * 
     * @param parameters the parameters to associate
     */
    void setParameters(Map<String, String> parameters);

    /**
     * @return name-value pair parameters associated with this event.
     */
    Map<String, String> getParameters();

}
