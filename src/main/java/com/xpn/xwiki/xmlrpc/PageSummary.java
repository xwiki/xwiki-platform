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
 */
package com.xpn.xwiki.xmlrpc;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.xmlrpc.Convert.ConversionException;

import java.util.HashMap;
import java.util.Map;

import org.suigeneris.jrcs.rcs.Version;

public class PageSummary
{
    private String id;

    private String space;

    private String parentId;

    private String title;

    private String url;

    private int locks;

    public PageSummary(String id, String space, String parentId, String title, String url,
        int locks)
    {
        setId(id);
        setSpace(space);
        setParentId(parentId);
        setTitle(title);
        setUrl(url);
        setLocks(locks);
    }

    public PageSummary(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        Version[] versions = doc.getRevisions(context);
        if (versions[versions.length-1].toString().equals(doc.getVersion())) {
            // Current version of document
            setId(doc.getFullName());
            setUrl(doc.getURL("view", context));
        } else {
            // Old version of document
            setId(doc.getFullName() + ":" + doc.getVersion());
            setUrl(doc.getURL("view", "rev="+doc.getVersion(), context));
        }
        
        setSpace(doc.getSpace());
        setParentId(doc.getParent());
        setTitle(doc.getName());
        setLocks(0);
    }
    
    public PageSummary(Map map) throws ConversionException
    {
        setId((String) map.get("id"));
        setSpace((String) map.get("space"));
        setParentId((String) map.get("parentId"));
        setTitle((String) map.get("title"));
        setUrl((String) map.get("url"));
        if (map.containsKey("locks")) {
            setLocks(Convert.str2int((String) map.get("locks")));
        }
    }

    Map toMap()
    {
        Map map = new HashMap();
        map.put("id", getId());
        map.put("space", getSpace());
        map.put("parentId", getParentId());
        map.put("title", getTitle());
        map.put("url", getUrl());
        map.put("locks", Convert.int2str(getLocks()));
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

    public String getSpace()
    {
        return space;
    }

    public void setSpace(String space)
    {
        this.space = space;
    }

    public String getParentId()
    {
        return parentId;
    }

    public void setParentId(String parentId)
    {
        this.parentId = parentId;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public int getLocks()
    {
        return locks;
    }

    public void setLocks(int locks)
    {
        this.locks = locks;
    }
}
