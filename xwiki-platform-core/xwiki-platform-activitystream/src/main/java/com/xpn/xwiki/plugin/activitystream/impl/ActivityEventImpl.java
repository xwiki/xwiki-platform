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
    protected String eventId;

    /**
     * Request ID.
     */
    protected String requestId;

    /**
     * Priority.
     */
    protected int priority;

    /**
     * Date the event occured.
     */
    protected Date date;

    /**
     * Stream the event belongs to.
     */
    protected String stream;

    /**
     * Application which fired the event (as of august 2009 this application is always "xwiki").
     */
    protected String application;

    /**
     * Type of the event, see {@link ActivityEventType}.
     */
    protected String type;

    /**
     * Context user at the time the event has been fired.
     */
    protected String user;

    /**
     * Wiki in which the event occured, example: "xwiki".
     */
    protected String wiki;

    /**
     * Space in which the event occured, example: "Main".
     */
    protected String space;

    /**
     * Name of the document which fired the event, example: "Main.WebHome".
     */
    protected String page;

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

    /**
     * True if the page which fired the event is marked as hidden, false otherwise.
     */
    protected boolean hidden;

    /**
     * Named parameters.
     */
    protected Map<String, String> parameters;
    
    @Override
    public String getDisplayTitle(XWikiContext context)
    {
        return context.getMessageTool().get(this.title, getParams());
    }

    @Override
    public String getDisplayBody(XWikiContext context)
    {
        return context.getMessageTool().get(this.body, getParams());
    }

    @Override
    public String getDisplayDate(XWikiContext context)
    {
        return context.getWiki().formatDate(this.date, null, context);
    }

    @Override
    public String getDisplayUser(XWikiContext context)
    {
        return context.getWiki().getUserName(this.user, context);
    }

    @Override
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
     * @return the parameters of the event
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

    @Override
    public String getEventId()
    {
        return this.eventId;
    }

    @Override
    public void setEventId(String eventId)
    {
        this.eventId = eventId;
    }

    @Override
    public String getRequestId()
    {
        return this.requestId;
    }

    @Override
    public void setRequestId(String requestId)
    {
        this.requestId = requestId;
    }

    @Override
    public int getPriority()
    {
        return this.priority;
    }

    @Override
    public void setPriority(int priority)
    {
        this.priority = priority;
    }

    @Override
    public String getApplication()
    {
        return this.application;
    }

    @Override
    public void setStream(String stream)
    {
        this.stream = stream;
    }

    @Override
    public String getVersion()
    {
        return this.version;
    }

    @Override
    public void setVersion(String version)
    {
        if (version != null) {
            this.version = version;
        }
    }

    @Override
    public String getParam1()
    {
        return this.param1;
    }

    @Override
    public void setParam1(String param1)
    {
        this.param1 = param1;
    }

    @Override
    public String getParam2()
    {
        return this.param2;
    }

    @Override
    public void setParam2(String param2)
    {
        this.param2 = param2;
    }

    @Override
    public String getParam3()
    {
        return this.param3;
    }

    @Override
    public void setParam3(String param3)
    {
        this.param3 = param3;
    }

    @Override
    public String getParam4()
    {
        return this.param4;
    }

    @Override
    public void setParam4(String param4)
    {
        this.param4 = param4;
    }

    @Override
    public String getParam5()
    {
        return this.param5;
    }

    @Override
    public void setParam5(String param5)
    {
        this.param5 = param5;
    }

    @Override
    public String getWiki()
    {
        return this.wiki;
    }

    @Override
    public void setWiki(String wiki)
    {
        this.wiki = wiki;
    }

    @Override
    public String getType()
    {
        return this.type;
    }

    @Override
    public void setType(String type)
    {
        this.type = type;
    }

    @Override
    public String getUser()
    {
        return this.user;
    }

    @Override
    public void setUser(String user)
    {
        this.user = user;
    }

    @Override
    public String getSpace()
    {
        return this.space;
    }

    @Override
    public void setSpace(String space)
    {
        this.space = space;
    }

    @Override
    public String getPage()
    {
        return this.page;
    }

    @Override
    public void setPage(String page)
    {
        this.page = page;
    }

    @Override
    public String getUrl()
    {
        return this.url;
    }

    @Override
    public void setUrl(String url)
    {
        this.url = url;
    }

    @Override
    public String getTitle()
    {
        return this.title;
    }

    @Override
    public void setTitle(String title)
    {
        this.title = title;
    }

    @Override
    public String getBody()
    {
        return this.body;
    }

    @Override
    public void setBody(String body)
    {
        this.body = body;
    }

    @Override
    public Date getDate()
    {
        return this.date;
    }

    @Override
    public void setDate(Date date)
    {
        this.date = date;
    }

    @Override
    public String getStream()
    {
        return this.stream;
    }

    @Override
    public void setApplication(String application)
    {
        this.application = application;
    }

    @Override
    public void setParameters(Map<String, String> parameters)
    {
        this.parameters = parameters;
    }

    @Override
    public Map<String, String> getParameters()
    {
        return this.parameters;
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
    public void setHidden(boolean hidden)
    {
        this.hidden = hidden;
    }
}
