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

import java.util.HashMap;
import java.util.Map;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

public class User
{
    private String name;

    private String fullname;

    private String email;

    private String url;

    public User(XWikiDocument userdoc, XWikiContext context)
    {
        setName(userdoc.getName());
        setFullname(userdoc.getStringValue("XWiki.XWikiUsers", "fullName"));
        setEmail(userdoc.getStringValue("XWiki.XWikiUsers", "email"));
        setUrl(userdoc.getURL("view", context));
    }
    
    public User(Map map)
    {
        setName((String) map.get("name"));
        setFullname((String) map.get("fullname"));
        setEmail((String) map.get("email"));
        setUrl((String) map.get("url"));
    }
    
    public Map toMap()
    {
        Map map = new HashMap();
        map.put("name", getName());
        map.put("fullname", getFullname());
        map.put("email", getEmail());
        map.put("url", getUrl());
        return map;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getFullname()
    {
        return fullname;
    }

    public void setFullname(String fullname)
    {
        this.fullname = fullname;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }
}
