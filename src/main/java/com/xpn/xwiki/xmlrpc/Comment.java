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
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.xmlrpc.Convert.ConversionException;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.suigeneris.jrcs.rcs.Version;

public class Comment
{
    private String id;

    private String pageId;

    private String title;

    private String content;

    private String url;

    private Date created;

    private String creator;

    public Comment(XWikiDocument doc, BaseObject obj, XWikiContext context) throws XWikiException
    {
        Version[] versions = doc.getRevisions(context);
        if (versions[versions.length - 1].toString().equals(doc.getVersion())) {
            setId(doc.getFullName() + ";" + obj.getNumber());
            setPageId(doc.getFullName());
            setUrl(doc.getURL("view", context));
        } else {
            setId(doc.getFullName() + ":" + doc.getVersion() + ";" + obj.getNumber());
            setPageId(doc.getFullName() + ":" + doc.getVersion());
            setUrl(doc.getURL("view", "rev=" + doc.getVersion(), context));
        }
        setTitle(doc.getName());
        setContent(obj.getStringValue("comment"));
        setCreated(obj.getDateValue("date"));
        setCreator(obj.getStringValue("author"));
    }

    /**
     * @param map A Map<String, String>
     */
    public Comment(Map map) throws ConversionException
    {
    	setId((String)map.get("id"));
    	setPageId((String)map.get("pageId"));
    	setTitle((String)map.get("title"));
    	setContent((String)map.get("content"));
    	setUrl((String)map.get("url"));
    	if (map.containsKey("created")) {
    	    setCreated(Convert.str2date((String)map.get("created")));
    	}
    	setCreator((String)map.get("creator"));
	}

    /**
     * @return A Map<String, String>
     */
    public Map toMap()
    {
        Map map = new HashMap();
        map.put("id", getId());
        map.put("pageId", getPageId());
        map.put("title", getTitle());
        map.put("content", getContent());
        map.put("url", getUrl());
        map.put("created", Convert.date2str(getCreated()));
        map.put("creator", getCreator());
        return map;
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
