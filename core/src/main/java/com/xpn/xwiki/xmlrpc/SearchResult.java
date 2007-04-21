/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
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
 * @author sdumitriu
 */

package com.xpn.xwiki.xmlrpc;

import com.xpn.xwiki.doc.XWikiDocument;

import java.util.Map;
import java.util.HashMap;

public class SearchResult
{
    private String title;

    private String url;

    private String excerpt;

    private String id;

    private String type;

    public SearchResult(String title, String url, String excerpt, String id, String type)
    {
        setTitle(title);
        setUrl(url);
        setExcerpt(excerpt);
        setId(id);
        setType(type);
    }

    public SearchResult(XWikiDocument document)
    {
        setTitle(document.getFullName());
        setId(document.getFullName());
        setUrl("http://127.0.0.1:9080/xwiki/bin/view/" + document.getSpace() + "/"
            + document.getName());
        setType("page");
        String content = document.getContent();
        if (content.length() <= 256)
            setExcerpt(content);
        else
            setExcerpt(content.substring(0, 256));
    }

    Map getParameters()
    {
        Map params = new HashMap();
        params.put("title", getTitle());
        params.put("url", getUrl());
        params.put("excerpt", getExcerpt());
        params.put("id", getId());
        params.put("type", getType());
        return params;
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

    public String getExcerpt()
    {
        return excerpt;
    }

    public void setExcerpt(String excerpt)
    {
        this.excerpt = excerpt;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }
}
