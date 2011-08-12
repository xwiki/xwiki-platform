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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.activitystream.api.ActivityEvent;

/**
 * @version $Id$
 */
public class ActivityEventImpl implements ActivityEvent
{
    /**
     * ID.
     */
    private String eventId;

    /**
     * Request ID.
     */
    private String requestId;

    /**
     * Priority.
     */
    private int priority;

    /**
     * Date the event occured.
     */
    private Date date;

    /**
     * Stream the event belongs to.
     */
    private String stream;

    /**
     * Application which fired the event (as of august 2009 this application is always "xwiki").
     */
    private String application;

    /**
     * Type of the event, see {@link ActivityEventType}.
     */
    private String type;

    /**
     * Context user at the time the event has been fired.
     */
    private String user;

    /**
     * Wiki in which the event occured, example: "xwiki".
     */
    private String wiki;

    /**
     * Space in which the event occured, example: "Main".
     */
    private String space;

    /**
     * Name of the document which fired the event, example: "Main.WebHome".
     */
    private String page;

    /**
     * URL of the document which fired the event, example: "/xwiki/bin/view/Main/WebHome".
     */
    private String url;

    /**
     * Title of the event.
     */
    private String title;

    /**
     * Body message of the event.
     */
    private String body = "";

    /**
     * Version of the document at the time the event occured.
     */
    private String version = "";

    /**
     * Free param 1.
     */
    private String param1 = "";

    /**
     * Free param 2.
     */
    private String param2 = "";

    /**
     * Free param 3.
     */
    private String param3 = "";

    /**
     * Free param 4.
     */
    private String param4 = "";

    /**
     * Free param 5.
     */
    private String param5 = "";

    /**
     * Named parameters.
     */
    private Map<String, String> parameters;
    
    /**
     * {@inheritDoc}
     */
    public String getDisplayTitle(XWikiContext context)
    {
        return context.getMessageTool().get(this.title, getParams());
    }

    /**
     * {@inheritDoc}
     */
    public String getDisplayBody(XWikiContext context)
    {
        return context.getMessageTool().get(this.body, getParams());
    }

    /**
     * {@inheritDoc}
     */
    public String getDisplayDate(XWikiContext context)
    {
        return context.getWiki().formatDate(this.date, null, context);
    }

    /**
     * {@inheritDoc}
     */
    public String getDisplayUser(XWikiContext context)
    {
        return context.getWiki().getUserName(this.user, context);
    }

    /**
     * {@inheritDoc}
     */
    public void setParams(List<String> params)
    {
        if (params != null) {
            if (params.size() > 0) {
                setParam1(params.get(0));
            }
            if (params.size() > 1) {
                setParam2(params.get(1));
            }
            if (params.size() > 2) {
                setParam3(params.get(2));
            }
            if (params.size() > 3) {
                setParam4(params.get(3));
            }
            if (params.size() > 4) {
                setParam5(params.get(4));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getParams()
    {
        List<String> params = new ArrayList<String>();
        params.add(getParam1());
        params.add(getParam2());
        params.add(getParam3());
        params.add(getParam4());
        params.add(getParam5());
        return params;
    }

    /**
     * {@inheritDoc}
     */
    public String getEventId()
    {
        return this.eventId;
    }

    /**
     * {@inheritDoc}
     */
    public void setEventId(String eventId)
    {
        this.eventId = eventId;
    }

    /**
     * {@inheritDoc}
     */
    public String getRequestId()
    {
        return this.requestId;
    }

    /**
     * {@inheritDoc}
     */
    public void setRequestId(String requestId)
    {
        this.requestId = requestId;
    }

    /**
     * {@inheritDoc}
     */
    public int getPriority()
    {
        return this.priority;
    }

    /**
     * {@inheritDoc}
     */
    public void setPriority(int priority)
    {
        this.priority = priority;
    }

    /**
     * {@inheritDoc}
     */
    public String getApplication()
    {
        return this.application;
    }

    /**
     * {@inheritDoc}
     */
    public void setStream(String stream)
    {
        this.stream = stream;
    }

    /**
     * {@inheritDoc}
     */
    public String getVersion()
    {
        return this.version;
    }

    /**
     * {@inheritDoc}
     */
    public void setVersion(String version)
    {
        if (version != null) {
            this.version = version;
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getParam1()
    {
        return this.param1;
    }

    /**
     * {@inheritDoc}
     */
    public void setParam1(String param1)
    {
        this.param1 = param1;
    }

    /**
     * {@inheritDoc}
     */
    public String getParam2()
    {
        return this.param2;
    }

    /**
     * {@inheritDoc}
     */
    public void setParam2(String param2)
    {
        this.param2 = param2;
    }

    /**
     * {@inheritDoc}
     */
    public String getParam3()
    {
        return this.param3;
    }

    /**
     * {@inheritDoc}
     */
    public void setParam3(String param3)
    {
        this.param3 = param3;
    }

    /**
     * {@inheritDoc}
     */
    public String getParam4()
    {
        return this.param4;
    }

    /**
     * {@inheritDoc}
     */
    public void setParam4(String param4)
    {
        this.param4 = param4;
    }

    /**
     * {@inheritDoc}
     */
    public String getParam5()
    {
        return this.param5;
    }

    /**
     * {@inheritDoc}
     */
    public void setParam5(String param5)
    {
        this.param5 = param5;
    }

    /**
     * {@inheritDoc}
     */
    public String getWiki()
    {
        return this.wiki;
    }

    /**
     * {@inheritDoc}
     */
    public void setWiki(String wiki)
    {
        this.wiki = wiki;
    }

    /**
     * {@inheritDoc}
     */
    public String getType()
    {
        return this.type;
    }

    /**
     * {@inheritDoc}
     */
    public void setType(String type)
    {
        this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    public String getUser()
    {
        return this.user;
    }

    /**
     * {@inheritDoc}
     */
    public void setUser(String user)
    {
        this.user = user;
    }

    /**
     * {@inheritDoc}
     */
    public String getSpace()
    {
        return this.space;
    }

    /**
     * {@inheritDoc}
     */
    public void setSpace(String space)
    {
        this.space = space;
    }

    /**
     * {@inheritDoc}
     */
    public String getPage()
    {
        return this.page;
    }

    /**
     * {@inheritDoc}
     */
    public void setPage(String page)
    {
        this.page = page;
    }

    /**
     * {@inheritDoc}
     */
    public String getUrl()
    {
        return this.url;
    }

    /**
     * {@inheritDoc}
     */
    public void setUrl(String url)
    {
        this.url = url;
    }

    /**
     * {@inheritDoc}
     */
    public String getTitle()
    {
        return this.title;
    }

    /**
     * {@inheritDoc}
     */
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * {@inheritDoc}
     */
    public String getBody()
    {
        return this.body;
    }

    /**
     * {@inheritDoc}
     */
    public void setBody(String body)
    {
        this.body = body;
    }

    /**
     * {@inheritDoc}
     */
    public Date getDate()
    {
        return this.date;
    }

    /**
     * {@inheritDoc}
     */
    public void setDate(Date date)
    {
        this.date = date;
    }

    /**
     * {@inheritDoc}
     */
    public String getStream()
    {
        return this.stream;
    }

    /**
     * {@inheritDoc}
     */
    public void setApplication(String application)
    {
        this.application = application;
    }

    /**
     * {@inheritDoc}
     */
    public void setParameters(Map<String, String> parameters)
    {
        this.parameters = parameters;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, String> getParameters()
    {
        return this.parameters;
    }
}
