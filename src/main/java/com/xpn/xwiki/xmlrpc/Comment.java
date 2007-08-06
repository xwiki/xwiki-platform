/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 *
 */

package com.xpn.xwiki.xmlrpc;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Comment
{
    private String id;

    private String pageId;

    private String title;

    private String content;

    private String url;

    private Date created;

    private String creator;

    public Comment(XWikiDocument doc, BaseObject obj, XWikiContext context)
    {
        setId(doc.getFullName() + ":" + obj.getNumber());
        setPageId(doc.getFullName());
        setTitle(doc.getName());
        setContent(obj.getStringValue("comment"));
        setCreated(obj.getDateValue("date"));
        setCreator(obj.getStringValue("author"));
        setUrl(doc.getURL("view", context));
    }

    public Comment(Map parameters)
    {
    	setId((String)parameters.get("id"));
    	setPageId((String)parameters.get("pageId"));
    	setTitle((String)parameters.get("title"));
    	setContent((String)parameters.get("content"));
    	setUrl((String)parameters.get("url"));
    	setCreated((Date)parameters.get("created"));
    	setCreator((String)parameters.get("creator"));
	}
    
	public Map getParameters()
	{
        Map params = new HashMap();
        params.put("id", getId());
        params.put("pageId", getPageId());
        params.put("title", getTitle());
        params.put("content", getContent());
        params.put("url", getUrl());
        params.put("created", getCreated());
        params.put("creator", getCreator());
        return params;
    }
	
	public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getPageId()
    {
        return pageId;
    }

    public void setPageId(String pageId)
    {
        this.pageId = pageId;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public Date getCreated()
    {
        return created;
    }

    public void setCreated(Date created)
    {
        this.created = created;
    }

    public String getCreator()
    {
        return creator;
    }

    public void setCreator(String creator)
    {
        this.creator = creator;
    }
}
