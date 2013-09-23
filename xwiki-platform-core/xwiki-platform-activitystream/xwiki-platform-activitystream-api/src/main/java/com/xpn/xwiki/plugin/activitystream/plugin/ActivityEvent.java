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

import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.XWikiContext;

import java.util.Date;
import java.util.Map;

/**
 * Api wrapper for a {@link com.xpn.xwiki.plugin.activitystream.api.ActivityEvent} to be exposed from velocity.
 * 
 * @version $Id$
 */
public class ActivityEvent extends Api
{
    /**
     * Wrapped event.
     */
    protected com.xpn.xwiki.plugin.activitystream.api.ActivityEvent event;

    /**
     * Constructor.
     *  
     * @param event event to wrap
     * @param context the XWiki context
     */
    public ActivityEvent(com.xpn.xwiki.plugin.activitystream.api.ActivityEvent event, XWikiContext context)
    {
        super(context);
        this.event = event;
    }

    /**
     * @return The unique ID of the event
     */
    public String getEventId()
    {
        return event.getEventId();
    }

    /**
     * @return The request ID
     */
    public String getRequestId()
    {
        return event.getRequestId();
    }

    /**
     * @return The priority of the event
     */
    public int getPriority()
    {
        return event.getPriority();
    }

    /**
     * @return The type of the event
     */
    public String getType()
    {
        return event.getType();
    }

    /**
     * @return The application name
     */
    public String getApplication()
    {
        return event.getApplication();
    }

    /**
     * @return The stream name
     */
    public String getStream()
    {
        return event.getStream();
    }

    /**
     * @return The stream name
     */
    public Date getDate()
    {
        return event.getDate();
    }

    /**
     * @return The wiki name of the user creating the event
     */
    public String getUser()
    {
        return event.getUser();
    }

    /**
     * @return The wiki name in which the event was created
     */
    public String getWiki()
    {
        return event.getWiki();
    }

    /**
     * @return The space name in which the event was created
     */
    public String getSpace()
    {
        return event.getSpace();
    }

    /**
     * @return The page of the event
     */
    public String getPage()
    {
        return event.getPage();
    }

    /**
     * @return The target url
     */
    public String getUrl()
    {
        return event.getUrl();
    }

    /**
     * @return The title of the event
     */
    public String getTitle()
    {
        return event.getTitle();
    }

    /**
     * @return The Body of the event
     */
    public String getBody()
    {
        return event.getBody();
    }

    /**
     * Allows to modify the title of an event This might be useful to control the display or RSS feeds.
     * 
     * @param title title to set
     */
    public void setTitle(String title)
    {
        if (hasProgrammingRights()) {
            event.setTitle(title);
        }
    }

    /**
     * Allows to modify the body of an event This might be useful to control the display or RSS feeds.
     * 
     * @param body body to set
     */
    public void setBody(String body)
    {
        if (hasProgrammingRights()) {
            event.setBody(body);
        }
    }

    /**
     * @return The document version on the event
     */
    public String getVersion()
    {
        return event.getVersion();
    }

    /**
     * @return The first param of the event
     */
    public String getParam1()
    {
        return event.getParam1();
    }

    /**
     * @return The second param of the event
     */
    public String getParam2()
    {
        return event.getParam2();
    }

    /**
     * @return The third param of the event
     */
    public String getParam3()
    {
        return event.getParam3();
    }

    /**
     * @return The fourth param of the event
     */
    public String getParam4()
    {
        return event.getParam4();
    }

    /**
     * @return The fifth param of the event
     */
    public String getParam5()
    {
        return event.getParam5();
    }

    /**
     * @return the event in displayable format.
     */
    public String getDisplayTitle()
    {
        return event.getDisplayTitle(context);
    }

    /**
     * @return the event body in displayable format
     */
    public String getDisplayBody()
    {
        return event.getDisplayBody(context);
    }

    /**
     * @return the event date in displayable format
     */
    public String getDisplayDate()
    {
        return event.getDisplayDate(context);
    }

    /**
     * @return the event user in displayable format
     */
    public String getDisplayUser()
    {
        return event.getDisplayUser(context);
    }

    /**
     * @return the parameters associated with the event
     */
    public Map<String, String> getParameters()
    {
        return event.getParameters();
    }
    
    /**
     * @return the wrapped event
     */
    public com.xpn.xwiki.plugin.activitystream.api.ActivityEvent getProtectedEvent()
    {
        if (hasProgrammingRights()) {
            return event;
        } else {
            return null;
        }
    }

    /**
     * @return the wrapped event
     */
    protected com.xpn.xwiki.plugin.activitystream.api.ActivityEvent getEvent()
    {
        return event;
    }
}
