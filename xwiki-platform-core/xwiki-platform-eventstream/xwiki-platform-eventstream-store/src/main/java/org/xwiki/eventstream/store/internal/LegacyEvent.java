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
package org.xwiki.eventstream.store.internal;

import java.util.Date;
import java.util.Set;

/**
 * A Legacy Activity Event represents an event as it was stored when Activity Stream were used in XWiki.
 *
 * TODO: migrate the database schema so we don't need to use this legacy class anymore.
 *
 * @version $Id$
 * @since 11.0RC1
 */
public class LegacyEvent
{
    /**
     * ID.
     */
    protected String eventId;

    /**
     * Request ID.
     */
    protected String requestId;

    /**
     * Stream the event belongs to.
     */
    protected String stream;

    /**
     * Date the event occured.
     */
    protected Date date;

    /**
     * Priority.
     */
    protected int priority;

    /**
     * Type of the event.
     */
    protected String type;

    /**
     * Application which fired the event (as of august 2009 this application is always "xwiki").
     */
    protected String application;

    /**
     * Context user at the time the event has been fired.
     */
    protected String user;

    /**
     * Wiki in which the event occured, example: "xwiki".
     */
    protected String wiki;

    /**
     * Space (local serialized reference) in which the event occurred, example: "Main", "A.B", "A.B.C", etc.
     */
    protected String space;

    /**
     * Name of the document which fired the event, example: "Main.WebHome".
     */
    protected String page;

    /**
     * True if the page which fired the event is marked as hidden, false otherwise.
     */
    protected boolean hidden;

    /**
     * URL of the document which fired the event, example: "/xwiki/bin/view/Main/WebHome".
     */
    protected String url;

    /**
     * Title of the event.
     */
    protected String title;

    /**
     * Body message of the event.
     */
    protected String body = "";

    /**
     * Version of the document at the time the event occured.
     */
    protected String version = "";

    /**
     * Free param 1.
     */
    protected String param1 = "";

    /**
     * Free param 2.
     */
    protected String param2 = "";

    /**
     * Free param 3.
     */
    protected String param3 = "";

    /**
     * Free param 4.
     */
    protected String param4 = "";

    /**
     * Free param 5.
     */
    protected String param5 = "";

    protected Set<String> target;

    /**
     * Each event has a unique ID.
     *
     * @return the unique ID of the event
     */
    public String getEventId()
    {
        return this.eventId;
    }

    /**
     * @param eventId the unique ID of the event to set
     * @see #getEventId()
     */
    public void setEventId(String eventId)
    {
        this.eventId = eventId;
    }

    /**
     * Multiple events can correspond to the same activity, so events can be grouped under the same request ID.
     *
     * @return the event group ID
     */
    public String getRequestId()
    {
        return this.requestId;
    }

    /**
     * @param requestId the event group ID
     * @see #getRequestId()
     */
    public void setRequestId(String requestId)
    {
        this.requestId = requestId;
    }

    /**
     * Events can happen in different contexts; for example, wiki activity events happen in different spaces, user
     * statuses are targeted to different groups.
     *
     * @return the stream name
     */
    public String getStream()
    {
        return this.stream;
    }

    /**
     * @param stream the stream Name
     * @see #getStream()
     */
    public void setStream(String stream)
    {
        this.stream = stream;
    }

    /**
     * An event happens at a precise date.
     *
     * @return the event date
     */
    public Date getDate()
    {
        return this.date;
    }

    /**
     * @param date the event date
     * @see #getDate()
     */
    public void setDate(Date date)
    {
        this.date = date;
    }

    /**
     * Different events can have a different priority. This allows to determine which events are more or less important
     * in the same request, or which events are important in the stream. For example, annotation automatic updates are
     * less important in a group of changes triggered by a document update, while a major version is more important than
     * a minor version.
     *
     * @return the priority of the event
     */
    public int getPriority()
    {
        return this.priority;
    }

    /**
     * @param priority the priority of the event
     * @see #getPriority()
     */
    public void setPriority(int priority)
    {
        this.priority = priority;
    }

    /**
     * @return The type of the event
     */
    public String getType()
    {
        return this.type;
    }

    /**
     * @param type The type of the event
     * @see #getType()
     */
    public void setType(String type)
    {
        this.type = type;
    }

    /**
     * Events can be triggered by different applications, not only the main wiki update events: user statuses, blog
     * pingbacks, new extension added...
     *
     * @return the application name
     */
    public String getApplication()
    {
        return this.application;
    }

    /**
     * @param application the application Name
     * @see #getApplication()
     */
    public void setApplication(String application)
    {
        this.application = application;
    }

    /**
     * Event usually occur as the result of a registered user activity.
     *
     * @return the wiki name of the user creating the event
     */
    public String getUser()
    {
        return this.user;
    }

    /**
     * @param user the wiki name of the user creating the event
     * @see #getUser()
     */
    public void setUser(String user)
    {
        this.user = user;
    }

    /**
     * In a wiki farm, each event happens in one of the wikis.
     *
     * @return the wiki name in which the event was created
     */
    public String getWiki()
    {
        return this.wiki;
    }

    /**
     * @param wiki the wiki name in which the event was created
     * @see #getWiki()
     */
    public void setWiki(String wiki)
    {
        this.wiki = wiki;
    }

    /**
     * Document-related events target a certain document, and documents belong to a space; this is the space of the
     * target document.
     *
     * @return the local serialized reference of the space in which the event was created
     */
    public String getSpace()
    {
        return this.space;
    }

    /**
     * @param space the local serialized reference of the space in which the event was created
     * @see #getSpace()
     */
    public void setSpace(String space)
    {
        this.space = space;
    }

    /**
     * Document-related events target a certain document, and documents have a name; this is the full name of the target
     * document.
     *
     * @return the page of the event
     */
    public String getPage()
    {
        return this.page;
    }

    /**
     * @param page the full name of the page of the event
     * @see #getPage()
     */
    public void setPage(String page)
    {
        this.page = page;
    }

    /**
     * @return True if the event has been triggered by an action performed on a document marked as hidden.
     */
    public boolean isHidden()
    {
        return this.hidden;
    }

    /**
     * @param hidden the event hidden flag.
     * @see #isHidden()
     */
    public void setHidden(Boolean hidden)
    {
        if (hidden != null) {
            this.hidden = hidden;
        }
    }

    /**
     * If an event happens in an URL-accessible location (a document), or if the event itself can be seen at a given
     * URL, this field stores that URL.
     *
     * @return the URL related to the event
     */
    public String getUrl()
    {
        return this.url;
    }

    /**
     * @param url the URL related to the event
     * @see #getUrl()
     */
    public void setUrl(String url)
    {
        this.url = url;
    }

    /**
     * A name for the event.
     *
     * @return the title of the event
     */
    public String getTitle()
    {
        return this.title;
    }

    /**
     * @param title the title of the event
     * @see #getTitle()
     */
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * A larger section of text where the event can store some data.
     *
     * @return the body of the event
     */
    public String getBody()
    {
        return this.body;
    }

    /**
     * @param body the body of the event
     * @see #getBody()
     */
    public void setBody(String body)
    {
        this.body = body;
    }

    /**
     * For events related to documents, this field records the version of the document at the time when the event
     * occurred.
     *
     * @return the document version when the event occurred
     */
    public String getVersion()
    {
        return this.version;
    }

    /**
     * @param version the document version when the event occurred
     * @see #getVersion()
     */
    public void setVersion(String version)
    {
        if (version != null) {
            this.version = version;
        }
    }

    /**
     * Free form parameter to be used by the event consumer.
     *
     * @return the first parameter associated with the event
     */
    public String getParam1()
    {
        return this.param1;
    }

    /**
     * @param param1 the first parameter associated with the event
     * @see #getParam1()
     */
    public void setParam1(String param1)
    {
        this.param1 = param1;
    }

    /**
     * Free form parameter to be used by the event consumer.
     *
     * @return the second parameter associated with the event
     */
    public String getParam2()
    {
        return this.param2;
    }

    /**
     * @param param2 the second parameter associated with the event
     * @see #getParam2()
     */
    public void setParam2(String param2)
    {
        this.param2 = param2;
    }

    /**
     * Free form parameter to be used by the event consumer.
     *
     * @return the third parameter associated with the event
     */
    public String getParam3()
    {
        return this.param3;
    }

    /**
     * @param param3 the third parameter associated with the event
     * @see #getParam3()
     */
    public void setParam3(String param3)
    {
        this.param3 = param3;
    }

    /**
     * Free form parameter to be used by the event consumer.
     *
     * @return the fourth parameter associated with the event
     */
    public String getParam4()
    {
        return this.param4;
    }

    /**
     * @param param4 the fourth parameter associated with the event
     * @see #getParam4()
     */
    public void setParam4(String param4)
    {
        this.param4 = param4;
    }

    /**
     * Free form parameter to be used by the event consumer.
     *
     * @return the fifth parameter associated with the event
     */
    public String getParam5()
    {
        return this.param5;
    }

    /**
     * @param param5 the fifth parameter associated with the event
     * @see #getParam5()
     */
    public void setParam5(String param5)
    {
        this.param5 = param5;
    }

    /**
     * @return a list of entities (users, groups) that are interested by this event
     */
    public Set<String> getTarget()
    {
        return target;
    }

    /**
     * @param target a list of entities (users, groups) that are interested by this event
     */
    public void setTarget(Set<String> target)
    {
        this.target = target;
    }
}
