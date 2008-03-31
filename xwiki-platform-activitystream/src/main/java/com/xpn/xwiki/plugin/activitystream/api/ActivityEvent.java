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

import com.xpn.xwiki.XWikiContext;

/**
 * An Activity Event represents a event done by a user in a certain context.
 * 
 * @version $Id: $
 */
public interface ActivityEvent
{
    /**
     * @return The unique ID of the event
     */
    String getEventId();

    /**
     * @param id Unique ID of the event
     * @see #getEventId()
     */
    void setEventId(String id);

    /**
     * @return The request ID
     */
    String getRequestId();

    /**
     * @param id The request ID of the event
     * @see #getRequestId()
     */
    void setRequestId(String id);

    /**
     * @return The priority of the event
     */
    int getPriority();

    /**
     * @param priority The priority of the event
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
     * @return The application name
     */
    String getApplication();

    /**
     * @param application The application Name
     * @see #getApplication()
     */
    void setApplication(String application);

    /**
     * @return The stream name
     */
    String getStream();

    /**
     * @param stream The stream Name
     * @see #getStream()
     */
    void setStream(String stream);

    /**
     * @return The stream name
     */
    Date getDate();

    /**
     * @param date The event date
     * @see #getDate()
     */
    void setDate(Date date);

    /**
     * @return The wiki name of the user creating the event
     */
    String getUser();

    /**
     * @param user The wiki name of the user creating the event
     * @see #getUser()
     */
    void setUser(String user);

    /**
     * @return The wiki name in which the event was created
     */
    String getWiki();

    /**
     * @param wiki The wiki name in which the event was created
     * @see #getWiki()
     */
    void setWiki(String wiki);

    /**
     * @return The space name in which the event was created
     */
    String getSpace();

    /**
     * @param space The space name in which the event was created
     * @see #getSpace()
     */
    void setSpace(String space);

    /**
     * @return The page of the event
     */
    String getPage();

    /**
     * @param page The page of the event
     * @see #getPage()
     */
    void setPage(String page);

    /**
     * @return The target url
     */
    String getUrl();

    /**
     * @param url The url of the event
     * @see #getUrl()
     */
    void setUrl(String url);

    /**
     * @return The title of the event
     */
    String getTitle();

    /**
     * @param title The title of the event
     * @see #getTitle()
     */
    void setTitle(String title);

    /**
     * @return The Body of the event
     */
    String getBody();

    /**
     * @param body The body of the event
     * @see #getBody()
     */
    void setBody(String body);

    /**
     * @return The first param of the event
     */
    String getParam1();

    /**
     * @param param1 The first param of the event
     * @see #getParam1()
     */
    void setParam1(String param1);

    /**
     * @return The second param of the event
     */
    String getParam2();

    /**
     * @param param2 The second param of the event
     * @see #getParam2()
     */
    void setParam2(String param2);

    /**
     * @return The third param of the event
     */
    String getParam3();

    /**
     * @param param3 The third param of the event
     * @see #getParam3()
     */
    void setParam3(String param3);

    /**
     * @return The fourth param of the event
     */
    String getParam4();

    /**
     * @param param4 The fourth param of the event
     * @see #getParam4()
     */
    void setParam4(String param4);

    /**
     * @return The fifth param of the event
     */
    String getParam5();

    /**
     * @param param5 The fifth param of the event
     * @see #getParam5()
     */
    void setParam5(String param5);

    /**
     * Retrieves the event in displayable format
     * 
     * @param context
     * @return
     */
    String getDisplayTitle(XWikiContext context);

    /**
     * Retrieves the event body in displayable format
     * 
     * @param context
     * @return
     */
    String getDisplayBody(XWikiContext context);

    /**
     * Retrieves the event date in displayable format
     * 
     * @param context
     * @return
     */
    String getDisplayDate(XWikiContext context);

    /**
     * Retrieves the event user in displayable format
     * 
     * @param context
     * @return
     */
    String getDisplayUser(XWikiContext context);

    /**
     * Set all params at once
     * 
     * @param params
     */
    void setParams(List params);

}
