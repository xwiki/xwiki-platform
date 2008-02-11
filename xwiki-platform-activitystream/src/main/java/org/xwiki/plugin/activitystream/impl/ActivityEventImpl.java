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

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import org.xwiki.plugin.activitystream.api.ActivityEvent;

import com.xpn.xwiki.XWikiContext;

public class ActivityEventImpl implements ActivityEvent
{
    protected String eventId;

    protected String requestId;

    protected int priority = 0;

    protected Date date;

    protected String stream;

    protected String application;

    protected String type;

    protected String user;

    protected String wiki;

    protected String space;

    protected String page;

    protected String url;

    protected String title;

    protected String body = "";

    protected String param1 = "";

    protected String param2 = "";

    protected String param3 = "";

    protected String param4 = "";

    protected String param5 = "";

    public String getDisplayTitle(XWikiContext context)
    {
        return context.getMessageTool().get(title, getParams());
    }

    public String getDisplayBody(XWikiContext context)
    {
        return context.getMessageTool().get(body, getParams());
    }

    public String getDisplayDate(XWikiContext context)
    {
        return context.getWiki().formatDate(date, null, context);
    }

    public String getDisplayUser(XWikiContext context)
    {
        return context.getWiki().getLocalUserName(user, context);
    }

    public void setParams(List params) {
        if (params!=null) {
            if (params.size()>0) {
                setParam1(params.get(0).toString());
            }
            if (params.size()>1) {
                setParam1(params.get(1).toString());
            }
            if (params.size()>2) {
                setParam1(params.get(2).toString());
            }
            if (params.size()>3) {
                setParam1(params.get(3).toString());
            }
            if (params.size()>4) {
                setParam1(params.get(4).toString());
            }
        }
    }

    public List getParams() {
        List params = new ArrayList();
        params.add(getParam1());
        params.add(getParam2());
        params.add(getParam3());
        params.add(getParam4());
        params.add(getParam5());
        return params;
    }

    public String getEventId()
    {
        return eventId;
    }

    public void setEventId(String eventId)
    {
        this.eventId = eventId;
    }

    public String getRequestId()
    {
        return requestId;
    }

    public void setRequestId(String requestId)
    {
        this.requestId = requestId;
    }

    public int getPriority()
    {
        return priority;
    }

    public void setPriority(int priority)
    {
        this.priority = priority;
    }

    public String getApplication()
    {
        return application;
    }

    public void setStream(String stream)
    {
        this.stream = stream;
    }

    public String getParam1()
    {
        return param1;
    }

    public void setParam1(String param1)
    {
        this.param1 = param1;
    }

    public String getParam2()
    {
        return param2;
    }

    public void setParam2(String param2)
    {
        this.param2 = param2;
    }

    public String getParam3()
    {
        return param3;
    }

    public void setParam3(String param3)
    {
        this.param3 = param3;
    }

    public String getParam4()
    {
        return param4;
    }

    public void setParam4(String param4)
    {
        this.param4 = param4;
    }

    public String getParam5()
    {
        return param5;
    }

    public void setParam5(String param5)
    {
        this.param5 = param5;
    }

    public String getWiki()
    {
        return wiki;
    }

    public void setWiki(String wiki)
    {
        this.wiki = wiki;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getUser()
    {
        return user;
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    public String getSpace()
    {
        return space;
    }

    public void setSpace(String space)
    {
        this.space = space;
    }

    public String getPage()
    {
        return page;
    }

    public void setPage(String page)
    {
        this.page = page;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getBody()
    {
        return body;
    }

    public void setBody(String body)
    {
        this.body = body;
    }

    public Date getDate()
    {
        return date;
    }

    public void setDate(Date date)
    {
        this.date = date;
    }

    public String getStream()
    {
        return stream;
    }

    public void setApplication(String application)
    {
        this.application = application;
    }
}
